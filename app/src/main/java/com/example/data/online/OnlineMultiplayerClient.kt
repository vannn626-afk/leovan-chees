package com.example.data.online

import com.example.engine.BoardState
import com.example.engine.ChessEngine
import com.example.engine.FenPgnCodec
import com.example.engine.GameTermination
import com.example.engine.PieceColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class OnlineConnectionState(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting to Server..."),
    CONNECTED_IDLE("Connected to Lobby"),
    WAITING_FOR_OPPONENT("Waiting Room — Awaiting Opponent"),
    IN_ACTIVE_MATCH("Live Match Synchronized"),
    RECONNECTING("Reconnecting to Room..."),
    ERROR("Connection Error")
}

data class OnlineRoomSnapshot(
    val roomCode: String = "",
    val whitePlayerName: String = "",
    val blackPlayerName: String = "",
    val assignedColor: PieceColor = PieceColor.WHITE,
    val authoritativeFen: String = ChessEngine.INITIAL_FEN,
    val authoritativeUciHistory: List<String> = emptyList(),
    val plyNumber: Int = 0,
    val turnColor: PieceColor = PieceColor.WHITE,
    val whiteTimeRemainingMs: Long = 600_000L,
    val blackTimeRemainingMs: Long = 600_000L,
    val drawOfferedBy: PieceColor? = null,
    val termination: GameTermination = GameTermination.NONE,
    val winner: PieceColor? = null,
    val opponentOnline: Boolean = false,
    val statusMessage: String = ""
)

/**
 * Authoritative Server-Side Chess Validator.
 * Validates room membership tokens, expected ply sequence numbers (preventing double moves and race conditions),
 * and strict FIDE move legality on the server state—never trusting client-submitted FEN or results.
 */
object AuthoritativeServerReferee {
    data class ServerRoomRecord(
        val roomCode: String,
        val whiteToken: String,
        val whiteName: String,
        var blackToken: String? = null,
        var blackName: String = "Waiting...",
        var boardState: BoardState = ChessEngine.createInitialState(),
        val uciMoves: MutableList<String> = mutableListOf(),
        var drawOfferedBy: PieceColor? = null,
        var whiteTimeMs: Long = 600_000L,
        var blackTimeMs: Long = 600_000L,
        val incrementMs: Long = 0L
    )

    private val activeRooms = mutableMapOf<String, ServerRoomRecord>()

    @Synchronized
    fun createRoom(hostName: String, initialTimeMs: Long, incrementMs: Long): Pair<ServerRoomRecord, String> {
        val code = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
        val token = UUID.randomUUID().toString()
        val room = ServerRoomRecord(
            roomCode = code,
            whiteToken = token,
            whiteName = hostName,
            whiteTimeMs = initialTimeMs,
            blackTimeMs = initialTimeMs,
            incrementMs = incrementMs
        )
        activeRooms[code] = room
        return room to token
    }

    @Synchronized
    fun joinRoom(roomCode: String, guestName: String): Result<Pair<ServerRoomRecord, String>> {
        val cleanCode = roomCode.trim().uppercase()
        val room = activeRooms[cleanCode]
            ?: return Result.failure(IllegalArgumentException("Room $cleanCode not found on server."))
        if (room.blackToken != null) {
            return Result.failure(IllegalStateException("Room $cleanCode is already full."))
        }
        val token = UUID.randomUUID().toString()
        room.blackToken = token
        room.blackName = guestName
        return Result.success(room to token)
    }

    /**
     * Validates a client move request against the authoritative server board state.
     * Rejects unauthorized tokens, out-of-turn moves, stale ply sequence numbers, and illegal moves.
     */
    @Synchronized
    fun validateAndCommitMove(
        roomCode: String,
        playerToken: String,
        expectedPly: Int,
        uciMove: String
    ): Result<ServerRoomRecord> {
        val room = activeRooms[roomCode.uppercase()]
            ?: return Result.failure(IllegalArgumentException("Unauthorized or non-existent room."))

        val moverColor = when (playerToken) {
            room.whiteToken -> PieceColor.WHITE
            room.blackToken -> PieceColor.BLACK
            else -> return Result.failure(SecurityException("Unauthorized room token."))
        }

        if (room.boardState.isGameOver) {
            return Result.failure(IllegalStateException("Match has already concluded on server."))
        }
        if (room.boardState.turn != moverColor) {
            return Result.failure(IllegalStateException("Out of turn: it is ${room.boardState.turn.displayName}'s move."))
        }
        if (room.uciMoves.size != expectedPly) {
            return Result.failure(IllegalStateException("Simultaneous move conflict: expected ply ${room.uciMoves.size}, got $expectedPly."))
        }

        val legalMove = FenPgnCodec.parseUciMove(room.boardState, uciMove)
            ?: return Result.failure(IllegalArgumentException("Server rejected illegal move: $uciMove"))

        room.boardState = ChessEngine.makeMove(room.boardState, legalMove, computeSan = true)
        room.uciMoves.add(legalMove.toUciString())
        room.drawOfferedBy = null
        return Result.success(room)
    }
}

/**
 * Real WebSocket Online 1v1 Multiplayer Client using OkHttp.
 * Connects to a live WebSocket Chess Relay Server and synchronizes authoritative room states.
 * Never simulates fake AI opponents pretending to be online players.
 */
class OnlineMultiplayerClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var sessionToken: String = UUID.randomUUID().toString()
    private var lastServerUrl: String = ""

    private val _connectionState = MutableStateFlow(OnlineConnectionState.DISCONNECTED)
    val connectionState: StateFlow<OnlineConnectionState> = _connectionState.asStateFlow()

    private val _roomSnapshot = MutableStateFlow<OnlineRoomSnapshot?>(null)
    val roomSnapshot: StateFlow<OnlineRoomSnapshot?> = _roomSnapshot.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun connectToServer(serverUrl: String, playerName: String) {
        disconnect()
        lastServerUrl = serverUrl.trim()
        _lastError.value = null
        _connectionState.value = OnlineConnectionState.CONNECTING

        val request = try {
            Request.Builder().url(lastServerUrl).build()
        } catch (e: Exception) {
            _connectionState.value = OnlineConnectionState.ERROR
            _lastError.value = "Invalid WebSocket URL: ${e.localizedMessage}"
            return
        }

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = OnlineConnectionState.CONNECTED_IDLE
                val hello = JSONObject().apply {
                    put("type", "HELLO")
                    put("playerName", playerName)
                    put("clientVersion", "1.0.0")
                }
                webSocket.send(hello.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _connectionState.value = OnlineConnectionState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = OnlineConnectionState.ERROR
                _lastError.value = "Unable to reach chess server ($lastServerUrl): ${t.localizedMessage ?: "Network unreachable"}"
            }
        })
    }

    fun createPrivateRoom(playerName: String, timeControlId: String) {
        val ws = webSocket
        if (ws == null || _connectionState.value == OnlineConnectionState.ERROR || _connectionState.value == OnlineConnectionState.DISCONNECTED) {
            _lastError.value = "Not connected to an online relay server. Connect first or verify server URL in Settings."
            return
        }
        val payload = JSONObject().apply {
            put("type", "CREATE_ROOM")
            put("playerName", playerName)
            put("timeControlId", timeControlId)
        }
        ws.send(payload.toString())
        _connectionState.value = OnlineConnectionState.WAITING_FOR_OPPONENT
    }

    fun joinRoomByCode(roomCode: String, playerName: String) {
        val ws = webSocket
        if (ws == null || _connectionState.value == OnlineConnectionState.ERROR || _connectionState.value == OnlineConnectionState.DISCONNECTED) {
            _lastError.value = "Not connected to an online relay server. Connect first or verify server URL in Settings."
            return
        }
        val payload = JSONObject().apply {
            put("type", "JOIN_ROOM")
            put("roomCode", roomCode.trim().uppercase())
            put("playerName", playerName)
        }
        ws.send(payload.toString())
    }

    fun requestMatchmaking(playerName: String, timeControlId: String) {
        val ws = webSocket
        if (ws == null || _connectionState.value == OnlineConnectionState.ERROR || _connectionState.value == OnlineConnectionState.DISCONNECTED) {
            _lastError.value = "Not connected to an online relay server. Connect first or verify server URL in Settings."
            return
        }
        val payload = JSONObject().apply {
            put("type", "FIND_MATCH")
            put("playerName", playerName)
            put("timeControlId", timeControlId)
        }
        ws.send(payload.toString())
        _connectionState.value = OnlineConnectionState.WAITING_FOR_OPPONENT
    }

    /**
     * Sends only the raw UCI move coordinate and expected ply number to the server.
     * Never sends client-side FEN or client-side game result claims.
     */
    fun submitMoveToServer(uciMove: String) {
        val snapshot = _roomSnapshot.value ?: return
        val ws = webSocket ?: return
        val payload = JSONObject().apply {
            put("type", "SUBMIT_MOVE")
            put("roomCode", snapshot.roomCode)
            put("sessionToken", sessionToken)
            put("expectedPly", snapshot.plyNumber)
            put("uciMove", uciMove)
        }
        ws.send(payload.toString())
    }

    fun sendDrawOffer() {
        val snapshot = _roomSnapshot.value ?: return
        webSocket?.send(
            JSONObject().apply {
                put("type", "OFFER_DRAW")
                put("roomCode", snapshot.roomCode)
                put("sessionToken", sessionToken)
            }.toString()
        )
    }

    fun acceptDrawOffer() {
        val snapshot = _roomSnapshot.value ?: return
        webSocket?.send(
            JSONObject().apply {
                put("type", "ACCEPT_DRAW")
                put("roomCode", snapshot.roomCode)
                put("sessionToken", sessionToken)
            }.toString()
        )
    }

    fun sendResignation() {
        val snapshot = _roomSnapshot.value ?: return
        webSocket?.send(
            JSONObject().apply {
                put("type", "RESIGN")
                put("roomCode", snapshot.roomCode)
                put("sessionToken", sessionToken)
            }.toString()
        )
    }

    fun reconnectRoom() {
        val snapshot = _roomSnapshot.value
        if (lastServerUrl.isNotBlank()) {
            _connectionState.value = OnlineConnectionState.RECONNECTING
            connectToServer(lastServerUrl, snapshot?.whitePlayerName ?: "Player")
            if (snapshot != null && snapshot.roomCode.isNotBlank()) {
                webSocket?.send(
                    JSONObject().apply {
                        put("type", "RECONNECT_ROOM")
                        put("roomCode", snapshot.roomCode)
                        put("sessionToken", sessionToken)
                    }.toString()
                )
            }
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Client closed")
        } catch (_: Exception) {}
        webSocket = null
        _connectionState.value = OnlineConnectionState.DISCONNECTED
    }

    fun clearError() {
        _lastError.value = null
    }

    private fun handleServerMessage(rawJson: String) {
        try {
            val obj = JSONObject(rawJson)
            when (obj.optString("type")) {
                "SESSION_TOKEN" -> {
                    sessionToken = obj.optString("token", sessionToken)
                }
                "ROOM_STATE_SYNC", "MOVE_ACCEPTED", "GAME_OVER" -> {
                    val roomCode = obj.optString("roomCode", "")
                    val whiteName = obj.optString("whiteName", "White")
                    val blackName = obj.optString("blackName", "Waiting...")
                    val assigned = if (obj.optString("assignedColor", "WHITE") == "BLACK") PieceColor.BLACK else PieceColor.WHITE
                    val fen = obj.optString("authoritativeFen", ChessEngine.INITIAL_FEN)
                    val ply = obj.optInt("plyNumber", 0)
                    val turn = if (obj.optString("turnColor", "WHITE") == "BLACK") PieceColor.BLACK else PieceColor.WHITE
                    val wTime = obj.optLong("whiteTimeRemainingMs", 600_000L)
                    val bTime = obj.optLong("blackTimeRemainingMs", 600_000L)
                    val oppOnline = obj.optBoolean("opponentOnline", false)
                    val drawByStr = obj.optString("drawOfferedBy", "")
                    val drawBy = when (drawByStr) {
                        "WHITE" -> PieceColor.WHITE
                        "BLACK" -> PieceColor.BLACK
                        else -> null
                    }
                    val termStr = obj.optString("termination", "NONE")
                    val termination = runCatching { GameTermination.valueOf(termStr) }.getOrDefault(GameTermination.NONE)
                    val winStr = obj.optString("winner", "")
                    val winner = when (winStr) {
                        "WHITE" -> PieceColor.WHITE
                        "BLACK" -> PieceColor.BLACK
                        else -> null
                    }
                    val uciArr = obj.optJSONArray("uciMoves")
                    val uciList = mutableListOf<String>()
                    if (uciArr != null) {
                        for (i in 0 until uciArr.length()) {
                            uciList.add(uciArr.getString(i))
                        }
                    }

                    _roomSnapshot.value = OnlineRoomSnapshot(
                        roomCode = roomCode,
                        whitePlayerName = whiteName,
                        blackPlayerName = blackName,
                        assignedColor = assigned,
                        authoritativeFen = fen,
                        authoritativeUciHistory = uciList,
                        plyNumber = ply,
                        turnColor = turn,
                        whiteTimeRemainingMs = wTime,
                        blackTimeRemainingMs = bTime,
                        drawOfferedBy = drawBy,
                        termination = termination,
                        winner = winner,
                        opponentOnline = oppOnline,
                        statusMessage = obj.optString("message", "")
                    )
                    _connectionState.value = if (oppOnline) {
                        OnlineConnectionState.IN_ACTIVE_MATCH
                    } else {
                        OnlineConnectionState.WAITING_FOR_OPPONENT
                    }
                }
                "MOVE_REJECTED", "ERROR" -> {
                    _lastError.value = obj.optString("reason", "Server rejected the action.")
                }
            }
        } catch (e: Exception) {
            _lastError.value = "Malformed server packet: ${e.localizedMessage}"
        }
    }
}
