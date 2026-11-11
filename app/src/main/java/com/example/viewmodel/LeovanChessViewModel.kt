package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.content.AcademyLesson
import com.example.data.content.ChessAcademyCatalog
import com.example.data.content.ChessPuzzle
import com.example.data.content.ChessPuzzleCatalog
import com.example.data.content.PuzzleCategory
import com.example.data.content.PuzzleDifficulty
import com.example.data.local.LeovanDatabase
import com.example.data.local.LeovanRepository
import com.example.data.local.LessonProgressEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PuzzleProgressEntity
import com.example.data.local.SavedGameEntity
import com.example.data.online.AuthoritativeServerReferee
import com.example.data.online.OnlineConnectionState
import com.example.data.online.OnlineMultiplayerClient
import com.example.data.online.OnlineRoomSnapshot
import com.example.data.sound.ChessSoundManager
import com.example.engine.AiDifficulty
import com.example.engine.BoardState
import com.example.engine.ChessAiEngine
import com.example.engine.ChessEngine
import com.example.engine.ChessMove
import com.example.engine.ChessPiece
import com.example.engine.FenPgnCodec
import com.example.engine.GameAnalysisReport
import com.example.engine.GameTermination
import com.example.engine.MoveFlag
import com.example.engine.PieceColor
import com.example.engine.PieceType
import com.example.engine.Square
import com.example.engine.StockfishUciAnalyzer
import com.example.engine.TimeControlCategory
import com.example.engine.TimeControlPreset
import com.example.ui.theme.AppThemeOption
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class AppScreen {
    MAIN_MENU,
    PLAY_HUB,
    ACTIVE_GAME,
    GAME_ANALYSIS,
    CHESS_ACADEMY,
    PUZZLES,
    HOW_TO_PLAY,
    IMPROVE_YOUR_CHESS,
    STATISTICS,
    SETTINGS,
    ABOUT
}

enum class GameMode(val displayName: String) {
    VS_COMPUTER("Lawan Komputer"),
    LOCAL_1V1("Lokal 1 vs 1"),
    ONLINE_1V1("Online 1 vs 1")
}

enum class ColorSelectionChoice(val label: String) {
    WHITE("Putih"),
    BLACK("Hitam"),
    RANDOM("Acak")
}

data class ActiveMatchUiState(
    val gameMode: GameMode = GameMode.VS_COMPUTER,
    val boardState: BoardState = ChessEngine.createInitialState(),
    val stateHistoryStack: List<BoardState> = emptyList(),
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val pendingPromotionBaseMove: ChessMove? = null,
    val humanColor: PieceColor = PieceColor.WHITE,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val aiThinkTimeMs: Long = AiDifficulty.MEDIUM.defaultThinkTimeMs,
    val isAiThinking: Boolean = false,
    val whitePlayerName: String = "Pemain",
    val blackPlayerName: String = "Komputer (Menengah)",
    val timeControl: TimeControlPreset = TimeControlPreset.ALL_PRESETS[6], // 10+0
    val whiteTimeRemainingMs: Long = 600_000L,
    val blackTimeRemainingMs: Long = 600_000L,
    val isPaused: Boolean = false,
    val isBoardFlipped: Boolean = false,
    val autoRotateLocalBoard: Boolean = true,
    val beginnerModeEnabled: Boolean = false,
    val beginnerCoachMessage: String = "Ketuk salah satu bidak Anda untuk melihat langkah sah dan saran taktis.",
    val hintMove: ChessMove? = null,
    val undoConsentPendingFrom: PieceColor? = null,
    val drawOfferPendingFrom: PieceColor? = null,
    val matchStartTimeMs: Long = System.currentTimeMillis(),
    val elapsedMatchSeconds: Long = 0L,
    val isResultRecorded: Boolean = false,
    val onlineAuthoritativeRoomCode: String? = null,
    val onlinePlayerToken: String? = null
)

class LeovanChessViewModel(
    application: Application,
    private val cloudRepository: com.example.data.firebase.ChessCloudRepository =
        com.example.data.firebase.ChessCloudRepository(application)
) : AndroidViewModel(application) {
    private val repository = LeovanRepository(LeovanDatabase.getInstance(application).chessDao())
    val onlineClient = OnlineMultiplayerClient()
    private var firestoreRoomJob: Job? = null

    private val _screenStack = MutableStateFlow(listOf(AppScreen.MAIN_MENU))
    val currentScreen: StateFlow<AppScreen> = MutableStateFlow(AppScreen.MAIN_MENU).also { flow ->
        viewModelScope.launch {
            _screenStack.collect { stack ->
                flow.value = stack.lastOrNull() ?: AppScreen.MAIN_MENU
            }
        }
    }

    val profileState: StateFlow<PlayerProfileEntity> = repository.profileFlow
        .map { it ?: PlayerProfileEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerProfileEntity()
        )

    val ongoingSavedGame: StateFlow<SavedGameEntity?> = repository.ongoingGameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completedGames: StateFlow<List<SavedGameEntity>> = repository.completedGamesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lessonProgress: StateFlow<List<LessonProgressEntity>> = repository.lessonProgressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val puzzleProgress: StateFlow<List<PuzzleProgressEntity>> = repository.puzzleProgressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _matchState = MutableStateFlow(ActiveMatchUiState())
    val matchState: StateFlow<ActiveMatchUiState> = _matchState.asStateFlow()

    private val _analysisReport = MutableStateFlow<GameAnalysisReport?>(null)
    val analysisReport: StateFlow<GameAnalysisReport?> = _analysisReport.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private var clockJob: Job? = null
    private var aiJob: Job? = null

    init {
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile()
            ChessSoundManager.updatePreferences(profile.soundEffectsEnabled, profile.musicEnabled)
        }
        viewModelScope.launch {
            profileState.collect { profile ->
                ChessSoundManager.updatePreferences(profile.soundEffectsEnabled, profile.musicEnabled)
                if (com.google.firebase.Firebase.auth.currentUser != null) {
                    cloudRepository.saveOrUpdateUserProfile(
                        playerName = profile.playerName,
                        preferredTheme = profile.selectedTheme,
                        preferredPieceColor = if (profile.defaultBoardFlipped) "BLACK" else "WHITE",
                        totalGamesPlayed = profile.gamesPlayed.toLong(),
                        totalWins = profile.gamesWon.toLong(),
                        totalLosses = profile.gamesLost.toLong(),
                        totalDraws = profile.gamesDrawn.toLong(),
                        puzzlesSolvedCount = profile.puzzlesSolved.toLong(),
                        currentPuzzleStreak = profile.currentPuzzleStreak.toLong(),
                        bestPuzzleStreak = profile.bestPuzzleStreak.toLong()
                    )
                }
            }
        }
        // Observe live WebSocket updates when playing Online 1v1
        viewModelScope.launch {
            onlineClient.roomSnapshot.collect { snapshot ->
                if (snapshot != null && _matchState.value.gameMode == GameMode.ONLINE_1V1) {
                    applyAuthoritativeOnlineSnapshot(snapshot)
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        ChessSoundManager.playButtonClick()
        val stack = _screenStack.value
        if (stack.lastOrNull() != screen) {
            _screenStack.value = stack + screen
        }
    }

    fun navigateBack(): Boolean {
        ChessSoundManager.playButtonClick()
        val stack = _screenStack.value
        return if (stack.size > 1) {
            _screenStack.value = stack.dropLast(1)
            true
        } else {
            false
        }
    }

    fun navigateHome() {
        ChessSoundManager.playButtonClick()
        _screenStack.value = listOf(AppScreen.MAIN_MENU)
    }

    // =========================================================================
    // MATCH INITIALIZATION & GAMEPLAY
    // =========================================================================

    fun startVsComputerMatch(
        difficulty: AiDifficulty,
        colorChoice: ColorSelectionChoice,
        timeControl: TimeControlPreset,
        beginnerMode: Boolean,
        customAiThinkTimeMs: Long = difficulty.defaultThinkTimeMs
    ) {
        val profile = profileState.value ?: PlayerProfileEntity()
        val humanColor = when (colorChoice) {
            ColorSelectionChoice.WHITE -> PieceColor.WHITE
            ColorSelectionChoice.BLACK -> PieceColor.BLACK
            ColorSelectionChoice.RANDOM -> if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        }
        val initialBoard = ChessEngine.createInitialState()
        val whiteName = if (humanColor == PieceColor.WHITE) profile.playerName else "LEOVAN AI (${difficulty.displayName})"
        val blackName = if (humanColor == PieceColor.BLACK) profile.playerName else "LEOVAN AI (${difficulty.displayName})"

        aiJob?.cancel()
        _analysisReport.value = null

        _matchState.value = ActiveMatchUiState(
            gameMode = GameMode.VS_COMPUTER,
            boardState = initialBoard,
            stateHistoryStack = emptyList(),
            humanColor = humanColor,
            aiDifficulty = difficulty,
            aiThinkTimeMs = customAiThinkTimeMs,
            isAiThinking = false,
            whitePlayerName = whiteName,
            blackPlayerName = blackName,
            timeControl = timeControl,
            whiteTimeRemainingMs = timeControl.initialTimeMs,
            blackTimeRemainingMs = timeControl.initialTimeMs,
            isPaused = false,
            isBoardFlipped = humanColor == PieceColor.BLACK,
            beginnerModeEnabled = beginnerMode || profile.beginnerAssistanceEnabled,
            beginnerCoachMessage = if (beginnerMode || profile.beginnerAssistanceEnabled) {
                "Mode Pemula Aktif: Ketuk bidak untuk melihat petak tujuan, aturan gerak, dan peringatan skak."
            } else "",
            matchStartTimeMs = System.currentTimeMillis()
        )

        ChessSoundManager.playGameStartSound()
        startClockTicker()
        navigateTo(AppScreen.ACTIVE_GAME)

        if (humanColor == PieceColor.BLACK) {
            triggerAiTurnIfNeeded()
        }
    }

    fun startLocal1v1Match(
        whiteName: String,
        blackName: String,
        timeControl: TimeControlPreset,
        autoRotateBoard: Boolean,
        beginnerMode: Boolean
    ) {
        val profile = profileState.value ?: PlayerProfileEntity()
        aiJob?.cancel()
        _analysisReport.value = null

        _matchState.value = ActiveMatchUiState(
            gameMode = GameMode.LOCAL_1V1,
            boardState = ChessEngine.createInitialState(),
            stateHistoryStack = emptyList(),
            humanColor = PieceColor.WHITE,
            whitePlayerName = whiteName.ifBlank { "Pemain 1 (Putih)" },
            blackPlayerName = blackName.ifBlank { "Pemain 2 (Hitam)" },
            timeControl = timeControl,
            whiteTimeRemainingMs = timeControl.initialTimeMs,
            blackTimeRemainingMs = timeControl.initialTimeMs,
            isPaused = false,
            isBoardFlipped = false,
            autoRotateLocalBoard = autoRotateBoard,
            beginnerModeEnabled = beginnerMode || profile.beginnerAssistanceEnabled,
            beginnerCoachMessage = "Putih melangkah lebih dulu. Bergantian perangkat setelah tiap langkah.",
            matchStartTimeMs = System.currentTimeMillis()
        )

        ChessSoundManager.playGameStartSound()
        startClockTicker()
        navigateTo(AppScreen.ACTIVE_GAME)
    }

    fun createAuthoritativeOnlineRoom(
        hostName: String,
        timeControl: TimeControlPreset
    ) {
        val profile = profileState.value
        val cleanHost = hostName.ifBlank { profile.playerName }
        // Register room on authoritative server referee and notify WebSocket relay if connected
        val (room, token) = AuthoritativeServerReferee.createRoom(
            hostName = cleanHost,
            initialTimeMs = timeControl.initialTimeMs,
            incrementMs = timeControl.incrementMs
        )
        onlineClient.createPrivateRoom(cleanHost, timeControl.id)

        _matchState.value = ActiveMatchUiState(
            gameMode = GameMode.ONLINE_1V1,
            boardState = room.boardState,
            humanColor = PieceColor.WHITE,
            whitePlayerName = room.whiteName,
            blackPlayerName = room.blackName,
            timeControl = timeControl,
            whiteTimeRemainingMs = room.whiteTimeMs,
            blackTimeRemainingMs = room.blackTimeMs,
            onlineAuthoritativeRoomCode = room.roomCode,
            onlinePlayerToken = token,
            beginnerModeEnabled = false,
            beginnerCoachMessage = "Online Room ${room.roomCode} created. Share the room code with your opponent."
        )
        if (com.google.firebase.Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                cloudRepository.createChessRoom(room.roomCode, cleanHost)
                subscribeToFirestoreRoom(room.roomCode)
            }
        }
        ChessSoundManager.playGameStartSound()
        startClockTicker()
        navigateTo(AppScreen.ACTIVE_GAME)
    }

    fun joinAuthoritativeOnlineRoom(
        roomCode: String,
        guestName: String,
        timeControl: TimeControlPreset
    ): Result<Unit> {
        val profile = profileState.value
        val cleanGuest = guestName.ifBlank { profile.playerName }
        val cleanCode = roomCode.trim().uppercase()
        onlineClient.joinRoomByCode(cleanCode, cleanGuest)

        if (com.google.firebase.Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                val cloudJoin = cloudRepository.joinRoomByCode(cleanCode, cleanGuest)
                cloudJoin.onSuccess { cloudRoom ->
                    _matchState.value = ActiveMatchUiState(
                        gameMode = GameMode.ONLINE_1V1,
                        boardState = FenPgnCodec.parseFen(cloudRoom.fen),
                        humanColor = PieceColor.BLACK,
                        whitePlayerName = cloudRoom.hostName,
                        blackPlayerName = cloudRoom.guestName.ifBlank { cleanGuest },
                        timeControl = timeControl,
                        whiteTimeRemainingMs = timeControl.initialTimeMs,
                        blackTimeRemainingMs = timeControl.initialTimeMs,
                        isBoardFlipped = true,
                        onlineAuthoritativeRoomCode = cloudRoom.roomCode,
                        onlinePlayerToken = "FIRESTORE_${cloudRoom.roomCode}"
                    )
                    subscribeToFirestoreRoom(cloudRoom.roomCode)
                    ChessSoundManager.playGameStartSound()
                    startClockTicker()
                    navigateTo(AppScreen.ACTIVE_GAME)
                }
            }
            val localServerJoin = AuthoritativeServerReferee.joinRoom(cleanCode, cleanGuest)
            if (localServerJoin.isFailure) {
                return Result.success(Unit)
            }
        }

        val localServerJoin = AuthoritativeServerReferee.joinRoom(cleanCode, cleanGuest)
        return localServerJoin.map { (room, token) ->
            _matchState.value = ActiveMatchUiState(
                gameMode = GameMode.ONLINE_1V1,
                boardState = room.boardState,
                humanColor = PieceColor.BLACK,
                whitePlayerName = room.whiteName,
                blackPlayerName = room.blackName,
                timeControl = timeControl,
                whiteTimeRemainingMs = room.whiteTimeMs,
                blackTimeRemainingMs = room.blackTimeMs,
                isBoardFlipped = true,
                onlineAuthoritativeRoomCode = room.roomCode,
                onlinePlayerToken = token
            )
            ChessSoundManager.playGameStartSound()
            startClockTicker()
            navigateTo(AppScreen.ACTIVE_GAME)
        }
    }

    fun requestOnlineQuickMatch(playerName: String, timeControl: TimeControlPreset) {
        val cleanName = playerName.ifBlank { profileState.value.playerName }
        onlineClient.requestMatchmaking(cleanName, timeControl.id)
        if (com.google.firebase.Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                val res = cloudRepository.findWaitingRoomOrCreate(cleanName)
                res.onSuccess { room ->
                    val uid = com.google.firebase.Firebase.auth.currentUser?.uid ?: ""
                    val myColor = if (room.hostUid == uid) PieceColor.WHITE else PieceColor.BLACK
                    _matchState.value = ActiveMatchUiState(
                        gameMode = GameMode.ONLINE_1V1,
                        boardState = FenPgnCodec.parseFen(room.fen),
                        humanColor = myColor,
                        whitePlayerName = room.hostName,
                        blackPlayerName = room.guestName.ifBlank { "Waiting for Challenger..." },
                        timeControl = timeControl,
                        whiteTimeRemainingMs = timeControl.initialTimeMs,
                        blackTimeRemainingMs = timeControl.initialTimeMs,
                        isBoardFlipped = myColor == PieceColor.BLACK,
                        onlineAuthoritativeRoomCode = room.roomCode,
                        onlinePlayerToken = "FIRESTORE_${room.roomCode}",
                        beginnerCoachMessage = if (room.status == "waiting") {
                            "Waiting for an online challenger in Room ${room.roomCode}..."
                        } else {
                            "Matched in Room ${room.roomCode}! Game started."
                        }
                    )
                    subscribeToFirestoreRoom(room.roomCode)
                    ChessSoundManager.playGameStartSound()
                    startClockTicker()
                    navigateTo(AppScreen.ACTIVE_GAME)
                }
            }
        }
    }

    private fun subscribeToFirestoreRoom(roomCode: String) {
        firestoreRoomJob?.cancel()
        firestoreRoomJob = viewModelScope.launch {
            cloudRepository.observeChessRoom(roomCode).collect { room ->
                if (room != null && _matchState.value.gameMode == GameMode.ONLINE_1V1) {
                    val moves = room.moveHistoryUci.split(" ").filter { it.isNotBlank() }
                    var reconstructed = ChessEngine.createInitialState()
                    for (uci in moves) {
                        val m = FenPgnCodec.parseUciMove(reconstructed, uci) ?: break
                        reconstructed = ChessEngine.makeMove(reconstructed, m, computeSan = true)
                    }
                    _matchState.value = _matchState.value.copy(
                        boardState = reconstructed,
                        whitePlayerName = room.hostName,
                        blackPlayerName = room.guestName.ifBlank { "Waiting for Opponent..." },
                        onlineAuthoritativeRoomCode = room.roomCode
                    )
                }
            }
        }
    }

    private fun applyAuthoritativeOnlineSnapshot(snapshot: OnlineRoomSnapshot) {
        val current = _matchState.value
        val reconstructedBoard = runCatching {
            var state = ChessEngine.createInitialState()
            for (uci in snapshot.authoritativeUciHistory) {
                val mv = FenPgnCodec.parseUciMove(state, uci) ?: break
                state = ChessEngine.makeMove(state, mv, computeSan = true)
            }
            state
        }.getOrElse { FenPgnCodec.parseFen(snapshot.authoritativeFen) }

        _matchState.value = current.copy(
            boardState = reconstructedBoard,
            whitePlayerName = snapshot.whitePlayerName,
            blackPlayerName = snapshot.blackPlayerName,
            humanColor = snapshot.assignedColor,
            whiteTimeRemainingMs = snapshot.whiteTimeRemainingMs,
            blackTimeRemainingMs = snapshot.blackTimeRemainingMs,
            drawOfferPendingFrom = snapshot.drawOfferedBy,
            onlineAuthoritativeRoomCode = snapshot.roomCode
        )
    }

    fun resumeOngoingSavedGame() {
        val saved = ongoingSavedGame.value ?: return
        val restoredBoard = if (saved.uciMoveListCsv.isNotBlank()) {
            var b = ChessEngine.createInitialState()
            for (uci in saved.uciMoveListCsv.split(",").filter { it.isNotBlank() }) {
                val m = FenPgnCodec.parseUciMove(b, uci) ?: break
                b = ChessEngine.makeMove(b, m, computeSan = true)
            }
            b
        } else {
            runCatching { FenPgnCodec.parseFen(saved.currentFen) }.getOrDefault(ChessEngine.createInitialState())
        }

        val mode = runCatching { GameMode.valueOf(saved.gameMode) }.getOrDefault(GameMode.VS_COMPUTER)
        val diff = runCatching { AiDifficulty.valueOf(saved.aiDifficulty) }.getOrDefault(AiDifficulty.MEDIUM)
        val humanCol = if (saved.playerColor == "BLACK") PieceColor.BLACK else PieceColor.WHITE
        val tc = TimeControlPreset.ALL_PRESETS.firstOrNull { it.id == saved.timeControlId }
            ?: TimeControlPreset.ALL_PRESETS[6]

        _matchState.value = ActiveMatchUiState(
            gameMode = mode,
            boardState = restoredBoard,
            humanColor = humanCol,
            aiDifficulty = diff,
            whitePlayerName = saved.whitePlayerName,
            blackPlayerName = saved.blackPlayerName,
            timeControl = tc,
            whiteTimeRemainingMs = saved.whiteTimeRemainingMs,
            blackTimeRemainingMs = saved.blackTimeRemainingMs,
            isBoardFlipped = humanCol == PieceColor.BLACK,
            beginnerModeEnabled = saved.beginnerModeUsed,
            elapsedMatchSeconds = saved.durationSeconds
        )
        startClockTicker()
        navigateTo(AppScreen.ACTIVE_GAME)
        triggerAiTurnIfNeeded()
    }

    // =========================================================================
    // BOARD INTERACTION & MOVE EXECUTION
    // =========================================================================

    fun onSquareClicked(square: Square) {
        val current = _matchState.value
        val board = current.boardState
        if (board.isGameOver || current.isPaused || current.isAiThinking) return

        // In VS_COMPUTER mode, only allow input on human's turn
        if (current.gameMode == GameMode.VS_COMPUTER && board.turn != current.humanColor) {
            return
        }

        val currentlySelected = current.selectedSquare
        val clickedPiece = board.pieceAt(square)

        // 1. If a square is already selected, check if clicked square is a legal destination
        if (currentlySelected != null) {
            val matchingMoves = current.legalMovesForSelected.filter { it.to == square }
            if (matchingMoves.isNotEmpty()) {
                // Check if this is a pawn promotion (4 promotion choices)
                if (matchingMoves.first().flag == MoveFlag.PROMOTION) {
                    _matchState.value = current.copy(pendingPromotionBaseMove = matchingMoves.first())
                } else {
                    executePlayerMove(matchingMoves.first())
                }
                return
            }

            // If user clicked another of their own pieces, switch selection to that piece
            if (clickedPiece != null && clickedPiece.color == board.turn) {
                if (currentlySelected == square) {
                    // Deselect
                    _matchState.value = current.copy(
                        selectedSquare = null,
                        legalMovesForSelected = emptyList()
                    )
                } else {
                    selectPieceOnSquare(square, clickedPiece)
                }
                return
            }

            // Otherwise, user clicked an illegal destination square!
            val explanation = ChessEngine.explainIllegalMoveAttempt(board, currentlySelected, square)
            _matchState.value = current.copy(
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                beginnerCoachMessage = explanation
            )
            return
        }

        // 2. No piece currently selected: select if friendly piece
        if (clickedPiece != null) {
            if (clickedPiece.color == board.turn) {
                selectPieceOnSquare(square, clickedPiece)
            } else if (current.beginnerModeEnabled) {
                _matchState.value = current.copy(
                    beginnerCoachMessage = "Bidak ${clickedPiece.type.displayName} itu milik ${clickedPiece.color.displayName}. Sekarang giliran ${board.turn.displayName} melangkah."
                )
            }
        }
    }

    private fun selectPieceOnSquare(square: Square, piece: ChessPiece) {
        val current = _matchState.value
        val legalForPiece = ChessEngine.getLegalMovesForSquare(current.boardState, square)
        val castlingHint = if (piece.type == PieceType.KING &&
            legalForPiece.any { it.flag == MoveFlag.KINGSIDE_CASTLE || it.flag == MoveFlag.QUEENSIDE_CASTLE }
        ) {
            " Rokade tersedia!"
        } else ""

        val message = if (current.beginnerModeEnabled) {
            "${piece.type.displayName} (${square.algebraic}): ${piece.type.beginnerRuleSummary} (${legalForPiece.size} langkah sah)$castlingHint"
        } else {
            current.beginnerCoachMessage
        }

        _matchState.value = current.copy(
            selectedSquare = square,
            legalMovesForSelected = legalForPiece,
            beginnerCoachMessage = message,
            hintMove = null
        )
    }

    fun onPromotionPieceSelected(promotionType: PieceType) {
        val current = _matchState.value
        val baseMove = current.pendingPromotionBaseMove ?: return
        val fullPromoMove = current.legalMovesForSelected.firstOrNull {
            it.from == baseMove.from && it.to == baseMove.to && it.promotionPiece == promotionType
        } ?: baseMove.copy(promotionPiece = promotionType, flag = MoveFlag.PROMOTION)

        _matchState.value = current.copy(pendingPromotionBaseMove = null)
        executePlayerMove(fullPromoMove)
    }

    fun dismissPromotionDialog() {
        _matchState.value = _matchState.value.copy(pendingPromotionBaseMove = null)
    }

    private fun executePlayerMove(move: ChessMove) {
        val current = _matchState.value
        val prevBoard = current.boardState

        // If Online 1v1 mode with authoritative server validation:
        if (current.gameMode == GameMode.ONLINE_1V1) {
            val roomCode = current.onlineAuthoritativeRoomCode
            val token = current.onlinePlayerToken
            if (roomCode != null && com.google.firebase.Firebase.auth.currentUser != null) {
                viewModelScope.launch {
                    cloudRepository.submitAuthoritativeRoomMove(roomCode, move.toUciString())
                }
            }
            if (roomCode != null && token != null && !token.startsWith("FIRESTORE_")) {
                val serverResult = AuthoritativeServerReferee.validateAndCommitMove(
                    roomCode = roomCode,
                    playerToken = token,
                    expectedPly = prevBoard.moveHistory.size,
                    uciMove = move.toUciString()
                )
                if (serverResult.isFailure) {
                    _matchState.value = current.copy(
                        selectedSquare = null,
                        legalMovesForSelected = emptyList(),
                        beginnerCoachMessage = serverResult.exceptionOrNull()?.message ?: "Server rejected move."
                    )
                    return
                }
                onlineClient.submitMoveToServer(move.toUciString())
            }
        }

        commitMoveInternal(move)
        triggerAiTurnIfNeeded()
    }

    private fun commitMoveInternal(move: ChessMove) {
        val current = _matchState.value
        val prevBoard = current.boardState
        val nextBoard = ChessEngine.makeMove(prevBoard, move, computeSan = true)

        // Apply clock increment to the player who just moved
        val incMs = if (current.timeControl.isUnlimited) 0L else current.timeControl.incrementMs
        val nextWhiteClock = if (prevBoard.turn == PieceColor.WHITE) {
            current.whiteTimeRemainingMs + incMs
        } else current.whiteTimeRemainingMs

        val nextBlackClock = if (prevBoard.turn == PieceColor.BLACK) {
            current.blackTimeRemainingMs + incMs
        } else current.blackTimeRemainingMs

        // Play appropriate sound effect
        when {
            nextBoard.isGameOver -> ChessSoundManager.playGameEndSound(nextBoard.winner == current.humanColor)
            nextBoard.isInCheck -> ChessSoundManager.playCheckSound()
            move.capturedPiece != null || move.flag == MoveFlag.EN_PASSANT -> ChessSoundManager.playCaptureSound()
            else -> ChessSoundManager.playMoveSound()
        }

        // Auto-rotate board in Local 1v1 if enabled
        val nextFlipped = if (current.gameMode == GameMode.LOCAL_1V1 && current.autoRotateLocalBoard && !nextBoard.isGameOver) {
            nextBoard.turn == PieceColor.BLACK
        } else {
            current.isBoardFlipped
        }

        val coachMsg = when {
            nextBoard.termination == GameTermination.CHECKMATE ->
                "Skakmat! ${nextBoard.winner?.displayName} memenangkan permainan (${move.san})."
            nextBoard.isGameOver ->
                "Permainan Selesai: ${nextBoard.termination.title}."
            nextBoard.isInCheck ->
                "Peringatan: Raja ${nextBoard.turn.displayName} sedang Skak! ${nextBoard.turn.displayName} wajib menyelamatkan Raja."
            move.flag == MoveFlag.KINGSIDE_CASTLE || move.flag == MoveFlag.QUEENSIDE_CASTLE ->
                "${prevBoard.turn.displayName} melakukan Rokade (${move.san}), mengamankan Raja dan mengaktifkan Benteng."
            move.flag == MoveFlag.EN_PASSANT ->
                "${prevBoard.turn.displayName} memakan En Passant (${move.san})!"
            move.flag == MoveFlag.PROMOTION ->
                "${prevBoard.turn.displayName} mempromosikan pion menjadi ${move.promotionPiece?.displayName} (${move.san})!"
            else -> "Langkah terakhir: ${move.san}. Giliran ${nextBoard.turn.displayName}."
        }

        _matchState.value = current.copy(
            boardState = nextBoard,
            stateHistoryStack = current.stateHistoryStack + prevBoard,
            selectedSquare = null,
            legalMovesForSelected = emptyList(),
            pendingPromotionBaseMove = null,
            whiteTimeRemainingMs = nextWhiteClock,
            blackTimeRemainingMs = nextBlackClock,
            isBoardFlipped = nextFlipped,
            beginnerCoachMessage = coachMsg,
            hintMove = null,
            undoConsentPendingFrom = null,
            drawOfferPendingFrom = null
        )

        persistMatchProgress()
    }

    private fun triggerAiTurnIfNeeded() {
        val current = _matchState.value
        if (current.gameMode != GameMode.VS_COMPUTER) return
        if (current.boardState.isGameOver) return
        if (current.boardState.turn == current.humanColor) return

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _matchState.value = _matchState.value.copy(isAiThinking = true)
            // Small natural minimum pause so UI renders the player's move cleanly
            delay(180L)
            val stateSnapshot = _matchState.value.boardState
            val difficulty = _matchState.value.aiDifficulty
            val thinkMs = _matchState.value.aiThinkTimeMs

            val aiMove = withContext(Dispatchers.Default) {
                ChessAiEngine.findBestMove(
                    state = stateSnapshot,
                    difficulty = difficulty,
                    timeLimitMs = thinkMs
                )
            }

            if (isActive && !_matchState.value.isPaused && !_matchState.value.boardState.isGameOver) {
                _matchState.value = _matchState.value.copy(isAiThinking = false)
                if (aiMove != null) {
                    commitMoveInternal(aiMove)
                }
            } else {
                _matchState.value = _matchState.value.copy(isAiThinking = false)
            }
        }
    }

    // =========================================================================
    // CLOCK, PAUSE, UNDO, DRAW, RESIGN, HINT, FEN/PGN
    // =========================================================================

    private fun startClockTicker() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (isActive) {
                delay(200L)
                val current = _matchState.value
                if (current.isPaused || current.boardState.isGameOver) continue

                val newElapsedSec = (System.currentTimeMillis() - current.matchStartTimeMs) / 1000L
                if (current.timeControl.isUnlimited) {
                    _matchState.value = current.copy(elapsedMatchSeconds = newElapsedSec)
                    continue
                }

                if (current.boardState.turn == PieceColor.WHITE) {
                    val rem = (current.whiteTimeRemainingMs - 200L).coerceAtLeast(0L)
                    if (rem == 0L) {
                        declareTimeout(PieceColor.WHITE)
                    } else {
                        _matchState.value = current.copy(
                            whiteTimeRemainingMs = rem,
                            elapsedMatchSeconds = newElapsedSec
                        )
                    }
                } else {
                    val rem = (current.blackTimeRemainingMs - 200L).coerceAtLeast(0L)
                    if (rem == 0L) {
                        declareTimeout(PieceColor.BLACK)
                    } else {
                        _matchState.value = current.copy(
                            blackTimeRemainingMs = rem,
                            elapsedMatchSeconds = newElapsedSec
                        )
                    }
                }
            }
        }
    }

    private fun declareTimeout(flaggedColor: PieceColor) {
        val current = _matchState.value
        if (current.boardState.isGameOver) return
        val winner = flaggedColor.opposite
        val updatedBoard = current.boardState.copy(
            termination = GameTermination.TIMEOUT,
            winner = winner
        )
        ChessSoundManager.playGameEndSound(winner == current.humanColor)
        _matchState.value = current.copy(
            boardState = updatedBoard,
            beginnerCoachMessage = "${flaggedColor.displayName} ran out of time! ${winner.displayName} wins on time."
        )
        persistMatchProgress()
    }

    fun togglePauseMatch() {
        val current = _matchState.value
        if (current.boardState.isGameOver) return
        val nextPaused = !current.isPaused
        _matchState.value = current.copy(isPaused = nextPaused)
        if (!nextPaused) {
            triggerAiTurnIfNeeded()
        }
    }

    fun flipBoardManually() {
        val current = _matchState.value
        _matchState.value = current.copy(isBoardFlipped = !current.isBoardFlipped)
    }

    fun toggleBeginnerModeInGame() {
        val current = _matchState.value
        val next = !current.beginnerModeEnabled
        _matchState.value = current.copy(
            beginnerModeEnabled = next,
            beginnerCoachMessage = if (next) {
                "Bantuan Pemula aktif: Ketuk bidak untuk melihat aturan gerak, langkah sah, dan saran taktis."
            } else {
                "Bantuan Pemula disembunyikan."
            }
        )
    }

    fun requestBeginnerHint() {
        val current = _matchState.value
        if (current.boardState.isGameOver || current.isAiThinking) return
        viewModelScope.launch {
            val best = withContext(Dispatchers.Default) {
                ChessAiEngine.findBestMove(current.boardState, AiDifficulty.HARD, 350L)
            }
            if (best != null) {
                _matchState.value = _matchState.value.copy(
                    selectedSquare = best.from,
                    legalMovesForSelected = ChessEngine.getLegalMovesForSquare(current.boardState, best.from),
                    hintMove = best,
                    beginnerCoachMessage = "Saran Pelatih: Coba gerakkan ${best.movedPiece.type.displayName} dari ${best.from.algebraic} ke ${best.to.algebraic}."
                )
            }
        }
    }

    fun requestUndoMove() {
        val current = _matchState.value
        val profile = profileState.value ?: PlayerProfileEntity()
        if (current.stateHistoryStack.isEmpty() || current.isAiThinking) return

        when (current.gameMode) {
            GameMode.VS_COMPUTER -> {
                if (!profile.allowUndoVsAi) {
                    _matchState.value = current.copy(
                        beginnerCoachMessage = "Undo is currently disabled in Settings."
                    )
                    return
                }
                // Undo 2 plies (AI move + Player move) if returning to human's turn, or 1 ply if needed
                val stack = current.stateHistoryStack
                val stepsBack = if (stack.size >= 2 && current.boardState.turn == current.humanColor) 2 else 1
                val targetState = stack[stack.size - stepsBack]
                val remainingStack = stack.dropLast(stepsBack)
                _matchState.value = current.copy(
                    boardState = targetState,
                    stateHistoryStack = remainingStack,
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    hintMove = null,
                    beginnerCoachMessage = "Move undone."
                )
            }
            GameMode.LOCAL_1V1 -> {
                // Request opponent's consent in Local 1v1
                _matchState.value = current.copy(
                    undoConsentPendingFrom = current.boardState.turn.opposite
                )
            }
            GameMode.ONLINE_1V1 -> {
                _matchState.value = current.copy(
                    beginnerCoachMessage = "Takebacks are not permitted in rated Online 1v1 matches."
                )
            }
        }
    }

    fun respondToLocalUndoConsent(approved: Boolean) {
        val current = _matchState.value
        if (!approved || current.stateHistoryStack.isEmpty()) {
            _matchState.value = current.copy(
                undoConsentPendingFrom = null,
                beginnerCoachMessage = "Undo request declined by opponent."
            )
            return
        }
        val prevState = current.stateHistoryStack.last()
        val nextFlipped = if (current.autoRotateLocalBoard) {
            prevState.turn == PieceColor.BLACK
        } else current.isBoardFlipped

        _matchState.value = current.copy(
            boardState = prevState,
            stateHistoryStack = current.stateHistoryStack.dropLast(1),
            selectedSquare = null,
            legalMovesForSelected = emptyList(),
            isBoardFlipped = nextFlipped,
            undoConsentPendingFrom = null,
            beginnerCoachMessage = "Undo approved by both players."
        )
    }

    fun offerOrClaimDraw() {
        val current = _matchState.value
        val board = current.boardState
        if (board.isGameOver) return

        // If 50-move or threefold repetition is available, claim immediately!
        if (board.canClaimFiftyMoveDraw) {
            val updated = board.copy(termination = GameTermination.FIFTY_MOVE_RULE, winner = null)
            _matchState.value = current.copy(boardState = updated)
            persistMatchProgress()
            return
        }
        if (board.canClaimThreefoldDraw) {
            val updated = board.copy(termination = GameTermination.THREEFOLD_REPETITION, winner = null)
            _matchState.value = current.copy(boardState = updated)
            persistMatchProgress()
            return
        }

        when (current.gameMode) {
            GameMode.VS_COMPUTER -> {
                // AI evaluates position: accepts draw if balanced or slightly worse for AI
                val evalWhite = ChessAiEngine.evaluatePositionWhitePerspective(board)
                val aiColor = current.humanColor.opposite
                val evalForAi = if (aiColor == PieceColor.WHITE) evalWhite else -evalWhite
                if (evalForAi <= 40 && board.fullMoveNumber >= 10) {
                    val updated = board.copy(termination = GameTermination.DRAW_BY_AGREEMENT, winner = null)
                    _matchState.value = current.copy(
                        boardState = updated,
                        beginnerCoachMessage = "Computer accepted your draw offer."
                    )
                    persistMatchProgress()
                } else {
                    _matchState.value = current.copy(
                        beginnerCoachMessage = "Computer declined the draw offer and wants to play on!"
                    )
                }
            }
            GameMode.LOCAL_1V1 -> {
                _matchState.value = current.copy(drawOfferPendingFrom = board.turn)
            }
            GameMode.ONLINE_1V1 -> {
                onlineClient.sendDrawOffer()
                _matchState.value = current.copy(
                    beginnerCoachMessage = "Draw offer sent to opponent."
                )
            }
        }
    }

    fun respondToDrawOffer(accepted: Boolean) {
        val current = _matchState.value
        if (!accepted) {
            _matchState.value = current.copy(
                drawOfferPendingFrom = null,
                beginnerCoachMessage = "Draw offer declined."
            )
            return
        }
        if (current.gameMode == GameMode.ONLINE_1V1) {
            onlineClient.acceptDrawOffer()
        }
        val updated = current.boardState.copy(
            termination = GameTermination.DRAW_BY_AGREEMENT,
            winner = null
        )
        ChessSoundManager.playGameEndSound(false)
        _matchState.value = current.copy(
            boardState = updated,
            drawOfferPendingFrom = null,
            beginnerCoachMessage = "Game drawn by mutual agreement."
        )
        persistMatchProgress()
    }

    fun resignMatch() {
        val current = _matchState.value
        if (current.boardState.isGameOver) return
        aiJob?.cancel()

        val resigningColor = if (current.gameMode == GameMode.VS_COMPUTER || current.gameMode == GameMode.ONLINE_1V1) {
            current.humanColor
        } else {
            current.boardState.turn
        }
        val winner = resigningColor.opposite

        if (current.gameMode == GameMode.ONLINE_1V1) {
            onlineClient.sendResignation()
        }

        val updated = current.boardState.copy(
            termination = GameTermination.RESIGNATION,
            winner = winner
        )
        ChessSoundManager.playGameEndSound(false)
        _matchState.value = current.copy(
            boardState = updated,
            isAiThinking = false,
            beginnerCoachMessage = "${resigningColor.displayName} resigned. ${winner.displayName} wins!"
        )
        persistMatchProgress()
    }

    fun restartCurrentMatch() {
        val current = _matchState.value
        when (current.gameMode) {
            GameMode.VS_COMPUTER -> startVsComputerMatch(
                difficulty = current.aiDifficulty,
                colorChoice = if (current.humanColor == PieceColor.WHITE) ColorSelectionChoice.WHITE else ColorSelectionChoice.BLACK,
                timeControl = current.timeControl,
                beginnerMode = current.beginnerModeEnabled,
                customAiThinkTimeMs = current.aiThinkTimeMs
            )
            GameMode.LOCAL_1V1 -> startLocal1v1Match(
                whiteName = current.whitePlayerName,
                blackName = current.blackPlayerName,
                timeControl = current.timeControl,
                autoRotateBoard = current.autoRotateLocalBoard,
                beginnerMode = current.beginnerModeEnabled
            )
            GameMode.ONLINE_1V1 -> {
                createAuthoritativeOnlineRoom(current.whitePlayerName, current.timeControl)
            }
        }
    }

    fun importFenOrPgnIntoMatch(input: String): Boolean {
        val trimmed = input.trim()
        val parsedBoard = if (trimmed.contains("/") && trimmed.lines().size == 1) {
            runCatching { FenPgnCodec.parseFen(trimmed) }.getOrNull()
        } else {
            FenPgnCodec.importPgn(trimmed)
        } ?: return false

        _matchState.value = _matchState.value.copy(
            boardState = parsedBoard,
            stateHistoryStack = emptyList(),
            selectedSquare = null,
            legalMovesForSelected = emptyList(),
            beginnerCoachMessage = "Position loaded from FEN/PGN."
        )
        return true
    }

    private fun persistMatchProgress() {
        val current = _matchState.value
        val board = current.boardState
        val pgn = FenPgnCodec.exportPgn(
            state = board,
            whiteName = current.whitePlayerName,
            blackName = current.blackPlayerName
        )
        val uciCsv = board.moveHistory.joinToString(",") { it.toUciString() }
        val entity = SavedGameEntity(
            isOngoing = !board.isGameOver,
            gameMode = current.gameMode.name,
            whitePlayerName = current.whitePlayerName,
            blackPlayerName = current.blackPlayerName,
            playerColor = current.humanColor.name,
            aiDifficulty = current.aiDifficulty.name,
            currentFen = FenPgnCodec.toFen(board),
            pgnMoves = pgn,
            uciMoveListCsv = uciCsv,
            timeControlId = current.timeControl.id,
            whiteTimeRemainingMs = current.whiteTimeRemainingMs,
            blackTimeRemainingMs = current.blackTimeRemainingMs,
            incrementMs = current.timeControl.incrementMs,
            totalMoves = (board.moveHistory.size + 1) / 2,
            durationSeconds = current.elapsedMatchSeconds,
            winnerColor = board.winner?.name,
            terminationReason = board.termination.name,
            beginnerModeUsed = current.beginnerModeEnabled
        )

        viewModelScope.launch {
            if (!board.isGameOver) {
                repository.saveOngoingGame(entity)
            } else if (!current.isResultRecorded) {
                _matchState.value = _matchState.value.copy(isResultRecorded = true)
                repository.recordCompletedGame(entity)
                repository.updateProfile { prof ->
                    val humanWon = board.winner == current.humanColor
                    val isDraw = board.termination.isDraw
                    prof.copy(
                        gamesPlayed = prof.gamesPlayed + 1,
                        gamesWon = prof.gamesWon + (if (humanWon) 1 else 0),
                        gamesLost = prof.gamesLost + (if (!humanWon && !isDraw && board.winner != null) 1 else 0),
                        gamesDrawn = prof.gamesDrawn + (if (isDraw) 1 else 0),
                        totalPlayTimeSeconds = prof.totalPlayTimeSeconds + current.elapsedMatchSeconds
                    )
                }
                if (com.google.firebase.Firebase.auth.currentUser != null) {
                    val humanWon = board.winner == current.humanColor
                    val isDraw = board.termination.isDraw
                    val outcome = when {
                        humanWon -> "WIN"
                        isDraw -> "DRAW"
                        else -> "LOSS"
                    }
                    cloudRepository.addMatchRecord(
                        gameMode = current.gameMode.name,
                        whiteName = current.whitePlayerName,
                        blackName = current.blackPlayerName,
                        playerColor = current.humanColor.name,
                        resultBanner = board.termination.title,
                        outcomeForPlayer = outcome,
                        pgnText = pgn,
                        finalFen = entity.currentFen,
                        totalMoves = entity.totalMoves.toLong()
                    )
                }
            }
        }
    }

    // =========================================================================
    // POST-GAME ENGINE ANALYSIS (STOCKFISH UCI / LOCAL ENGINE)
    // =========================================================================

    fun launchGameAnalysis(movesToAnalyze: List<ChessMove> = _matchState.value.boardState.moveHistory) {
        navigateTo(AppScreen.GAME_ANALYSIS)
        _isAnalyzing.value = true
        viewModelScope.launch {
            val report = withContext(Dispatchers.Default) {
                StockfishUciAnalyzer.analyzeGame(
                    moves = movesToAnalyze,
                    context = getApplication(),
                    perMoveTimeMs = 75L
                )
            }
            _analysisReport.value = report
            _isAnalyzing.value = false
        }
    }

    fun analyzeSavedHistoricalGame(savedGame: SavedGameEntity) {
        val reconstructedMoves = mutableListOf<ChessMove>()
        var state = ChessEngine.createInitialState()
        for (uci in savedGame.uciMoveListCsv.split(",").filter { it.isNotBlank() }) {
            val mv = FenPgnCodec.parseUciMove(state, uci) ?: break
            state = ChessEngine.makeMove(state, mv, computeSan = true)
            state.moveHistory.lastOrNull()?.let { reconstructedMoves.add(it) }
        }
        _matchState.value = _matchState.value.copy(
            boardState = state,
            whitePlayerName = savedGame.whitePlayerName,
            blackPlayerName = savedGame.blackPlayerName,
            elapsedMatchSeconds = savedGame.durationSeconds
        )
        launchGameAnalysis(reconstructedMoves)
    }

    // =========================================================================
    // ACADEMY, PUZZLES, CHECKLIST & SETTINGS UPDATERS
    // =========================================================================

    fun completeAcademyLesson(lesson: AcademyLesson) {
        viewModelScope.launch {
            repository.markLessonCompleted(lesson.id, lesson.chapterId)
        }
    }

    fun recordPuzzleSolved(puzzle: ChessPuzzle, attempts: Int, isDaily: Boolean) {
        viewModelScope.launch {
            repository.markPuzzleSolved(
                puzzleId = puzzle.id,
                category = puzzle.category.name,
                difficulty = puzzle.difficulty.name,
                attempts = attempts
            )
            repository.updateProfile { prof ->
                val newStreak = prof.currentPuzzleStreak + 1
                val bestStreak = maxOf(prof.bestPuzzleStreak, newStreak)
                val scoreDelta = if (attempts == 1) 18 else 8
                val todayEpoch = System.currentTimeMillis() / 86_400_000L
                prof.copy(
                    puzzlesSolved = prof.puzzlesSolved + 1,
                    puzzleScore = prof.puzzleScore + scoreDelta,
                    currentPuzzleStreak = newStreak,
                    bestPuzzleStreak = bestStreak,
                    lastDailyPuzzleEpochDay = if (isDaily) todayEpoch else prof.lastDailyPuzzleEpochDay
                )
            }
        }
    }

    fun recordPuzzleMistake() {
        viewModelScope.launch {
            repository.updateProfile { prof ->
                prof.copy(
                    currentPuzzleStreak = 0,
                    puzzleScore = (prof.puzzleScore - 5).coerceAtLeast(400)
                )
            }
        }
    }

    fun toggleChecklistMilestone(milestoneId: String) {
        viewModelScope.launch {
            repository.updateProfile { prof ->
                val currentSet = prof.completedChecklistIdsCsv
                    .split(",")
                    .filter { it.isNotBlank() }
                    .toMutableSet()
                if (currentSet.contains(milestoneId)) {
                    currentSet.remove(milestoneId)
                } else {
                    currentSet.add(milestoneId)
                }
                prof.copy(completedChecklistIdsCsv = currentSet.joinToString(","))
            }
        }
    }

    fun updateTheme(theme: AppThemeOption) {
        ChessSoundManager.playButtonClick()
        viewModelScope.launch {
            repository.updateProfile { it.copy(selectedTheme = theme.name) }
        }
    }

    fun updatePlayerProfileName(newName: String, avatarIdx: Int) {
        viewModelScope.launch {
            repository.updateProfile {
                it.copy(
                    playerName = newName.trim().ifBlank { "Grandmaster Player" },
                    avatarIndex = avatarIdx
                )
            }
        }
    }

    fun updateSettingsFlags(transform: (PlayerProfileEntity) -> PlayerProfileEntity) {
        viewModelScope.launch {
            repository.updateProfile(transform)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllLocalData()
        }
    }
}
