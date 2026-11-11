package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.engine.ChessEngine
import com.example.engine.FenPgnCodec
import com.example.engine.GameTermination
import com.example.engine.PieceColor
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ChessCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveOrUpdateUserProfile(
        playerName: String,
        preferredTheme: String,
        preferredPieceColor: String,
        totalGamesPlayed: Long,
        totalWins: Long,
        totalLosses: Long,
        totalDraws: Long,
        puzzlesSolvedCount: Long,
        currentPuzzleStreak: Long,
        bestPuzzleStreak: Long
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeName = playerName.trim().take(40).ifEmpty { "Grandmaster" }
        val safeTheme = preferredTheme.trim().take(30).ifEmpty { "CLASSIC_WOOD" }
        val safeColor = preferredPieceColor.trim().take(16).ifEmpty { "WHITE" }

        try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "playerName" to safeName,
                    "preferredTheme" to safeTheme,
                    "preferredPieceColor" to safeColor,
                    "totalGamesPlayed" to totalGamesPlayed.coerceAtLeast(0L),
                    "totalWins" to totalWins.coerceAtLeast(0L),
                    "totalLosses" to totalLosses.coerceAtLeast(0L),
                    "totalDraws" to totalDraws.coerceAtLeast(0L),
                    "puzzlesSolvedCount" to puzzlesSolvedCount.coerceAtLeast(0L),
                    "currentPuzzleStreak" to currentPuzzleStreak.coerceAtLeast(0L),
                    "bestPuzzleStreak" to bestPuzzleStreak.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "playerName" to safeName,
                    "preferredTheme" to safeTheme,
                    "preferredPieceColor" to safeColor,
                    "totalGamesPlayed" to totalGamesPlayed.coerceAtLeast(0L),
                    "totalWins" to totalWins.coerceAtLeast(0L),
                    "totalLosses" to totalLosses.coerceAtLeast(0L),
                    "totalDraws" to totalDraws.coerceAtLeast(0L),
                    "puzzlesSolvedCount" to puzzlesSolvedCount.coerceAtLeast(0L),
                    "currentPuzzleStreak" to currentPuzzleStreak.coerceAtLeast(0L),
                    "bestPuzzleStreak" to bestPuzzleStreak.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun addMatchRecord(
        gameMode: String,
        whiteName: String,
        blackName: String,
        playerColor: String,
        resultBanner: String,
        outcomeForPlayer: String,
        pgnText: String,
        finalFen: String,
        totalMoves: Long
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("match_history")
        val docRef = collectionRef.document()
        val validOutcome = when (outcomeForPlayer.uppercase()) {
            "WIN", "LOSS", "DRAW" -> outcomeForPlayer.uppercase()
            else -> "DRAW"
        }
        val payload = mapOf(
            "userId" to uid,
            "gameMode" to gameMode.take(32).ifEmpty { "VS_COMPUTER" },
            "whiteName" to whiteName.take(40).ifEmpty { "White" },
            "blackName" to blackName.take(40).ifEmpty { "Black" },
            "playerColor" to playerColor.take(16).ifEmpty { "WHITE" },
            "resultBanner" to resultBanner.take(120).ifEmpty { "Game Over" },
            "outcomeForPlayer" to validOutcome,
            "pgnText" to pgnText.take(10000),
            "finalFen" to finalFen.take(200).ifEmpty { ChessEngine.INITIAL_FEN },
            "totalMoves" to totalMoves.coerceIn(0L, 500L),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeMatchHistory(userId: String): Flow<List<CloudMatchRecord>> = flow {
        val path = "users/$userId/match_history"
        emitAll(
            db.collection("users").document(userId).collection("match_history")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudMatchRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createChessRoom(roomCode: String, hostName: String): Result<String> = runCatching {
        val uid = requireUserId()
        val safeCode = roomCode.trim().uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(16)
            .let { if (it.length < 4) "LVN${(1000..9999).random()}" else it }
        val docRef = db.collection("chess_rooms").document(safeCode)
        val payload = mapOf(
            "roomCode" to safeCode,
            "hostUid" to uid,
            "hostName" to hostName.trim().take(40).ifEmpty { "Host" },
            "guestUid" to "",
            "guestName" to "",
            "status" to "waiting",
            "fen" to ChessEngine.INITIAL_FEN,
            "turn" to "WHITE",
            "plyCount" to 0L,
            "lastMoveUci" to "",
            "moveHistoryUci" to "",
            "resultBanner" to "",
            "winnerUid" to "",
            "drawOfferBy" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun joinRoomByCode(roomCode: String, guestName: String): Result<ChessRoom> = runCatching {
        val uid = requireUserId()
        val safeCode = roomCode.trim().uppercase().replace(Regex("[^A-Z0-9_-]"), "").take(16)
        val docRef = db.collection("chess_rooms").document(safeCode)
        try {
            val snapshot = docRef.get().await()
            val room = snapshot.toObject(
                ChessRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Room $safeCode not found")

            if (room.status == "waiting" && room.hostUid != uid) {
                val joinUpdate = mapOf(
                    "guestUid" to uid,
                    "guestName" to guestName.trim().take(40).ifEmpty { "Challenger" },
                    "status" to "active",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(joinUpdate).await()
                val updatedSnap = docRef.get().await()
                updatedSnap.toObject(
                    ChessRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: room.copy(guestUid = uid, guestName = guestName, status = "active")
            } else {
                room
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun findWaitingRoomOrCreate(playerName: String): Result<ChessRoom> = runCatching {
        val uid = requireUserId()
        val path = "chess_rooms"
        try {
            val querySnap = db.collection(path)
                .whereEqualTo("status", "waiting")
                .get()
                .await()
            val candidateDoc = querySnap.documents.firstOrNull { doc ->
                val hostUid = doc.getString("hostUid") ?: ""
                hostUid.isNotBlank() && hostUid != uid
            }
            if (candidateDoc != null) {
                joinRoomByCode(candidateDoc.id, playerName).getOrThrow()
            } else {
                val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                val code = "LVN" + (1..3).map { chars.random() }.joinToString("")
                val roomId = createChessRoom(code, playerName).getOrThrow()
                val snap = db.collection(path).document(roomId).get().await()
                snap.toObject(
                    ChessRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: ChessRoom(id = roomId, roomCode = code, hostUid = uid, hostName = playerName)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    fun observeChessRoom(roomId: String): Flow<ChessRoom?> = flow {
        val path = "chess_rooms/$roomId"
        emitAll(
            db.collection("chess_rooms").document(roomId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ChessRoom::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitAuthoritativeRoomMove(
        roomId: String,
        moveUci: String
    ): Result<ChessRoom> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("chess_rooms").document(roomId)
        try {
            val snapshot = docRef.get().await()
            val room = snapshot.toObject(
                ChessRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Room $roomId not found")

            require(room.status == "active") { "Room is not active" }
            val senderColor = when (uid) {
                room.hostUid -> PieceColor.WHITE
                room.guestUid -> PieceColor.BLACK
                else -> throw IllegalStateException("Caller is not a participant in room $roomId")
            }

            // Reconstruct canonical game state from standard initial position using ChessEngine
            var canonicalBoard = ChessEngine.createInitialState()
            val priorMoves = room.moveHistoryUci.split(" ").filter { it.isNotBlank() }
            for (u in priorMoves) {
                val mv = FenPgnCodec.parseUciMove(canonicalBoard, u)
                    ?: throw IllegalStateException("Corrupted room move history at $u")
                canonicalBoard = ChessEngine.makeMove(canonicalBoard, mv, computeSan = true)
            }

            require(!canonicalBoard.isGameOver) { "Match has already concluded." }
            require(canonicalBoard.turn == senderColor) {
                "Out of turn: it is ${canonicalBoard.turn.displayName}'s move."
            }

            val legalMove = FenPgnCodec.parseUciMove(canonicalBoard, moveUci)
                ?: throw IllegalArgumentException("Illegal move rejected by authoritative validator: $moveUci")

            val updatedBoard = ChessEngine.makeMove(canonicalBoard, legalMove, computeSan = true)
            val newHistory = (priorMoves + legalMove.toUciString()).joinToString(" ")
            val isTerminal = updatedBoard.isGameOver
            val newStatus = if (isTerminal) "finished" else "active"
            val resultBanner = when (updatedBoard.termination) {
                GameTermination.CHECKMATE -> "Checkmate — ${senderColor.displayName} Wins!"
                GameTermination.STALEMATE -> "Draw by Stalemate"
                GameTermination.INSUFFICIENT_MATERIAL -> "Draw by Insufficient Material"
                GameTermination.THREEFOLD_REPETITION -> "Draw by Threefold Repetition"
                GameTermination.SEVENTY_FIVE_MOVE_RULE -> "Draw by 75-Move Rule"
                GameTermination.FIFTY_MOVE_RULE -> "Draw by 50-Move Rule"
                GameTermination.DEAD_POSITION -> "Draw by Dead Position"
                else -> ""
            }
            val winnerUid = if (updatedBoard.termination == GameTermination.CHECKMATE) uid else ""
            val updatedFen = FenPgnCodec.toFen(updatedBoard)
            val newPlyCount = (priorMoves.size + 1).toLong()

            val updatePayload = mapOf(
                "status" to newStatus,
                "fen" to updatedFen.take(200),
                "turn" to senderColor.opposite.name,
                "plyCount" to newPlyCount,
                "lastMoveUci" to legalMove.toUciString().take(10),
                "moveHistoryUci" to newHistory.take(5000),
                "resultBanner" to resultBanner.take(120),
                "winnerUid" to winnerUid,
                "drawOfferBy" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
            val updatedSnap = docRef.get().await()
            updatedSnap.toObject(
                ChessRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: room.copy(
                status = newStatus,
                fen = updatedFen,
                turn = senderColor.opposite.name,
                plyCount = newPlyCount,
                lastMoveUci = legalMove.toUciString(),
                moveHistoryUci = newHistory,
                resultBanner = resultBanner,
                winnerUid = winnerUid,
                drawOfferBy = ""
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun finishRoomByResignationOrDraw(
        roomId: String,
        resultBanner: String,
        winnerUid: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("chess_rooms").document(roomId)
        try {
            val updatePayload = mapOf(
                "status" to "finished",
                "resultBanner" to resultBanner.take(120),
                "winnerUid" to winnerUid.take(128),
                "drawOfferBy" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
