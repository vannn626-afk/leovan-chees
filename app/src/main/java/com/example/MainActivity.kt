package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.ChessCloudRepository
import com.example.data.local.PlayerProfileEntity
import com.example.ui.auth.SignInScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutFromGoogle
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ActiveChessGameScreen
import com.example.ui.screens.ChessAcademyScreen
import com.example.ui.screens.ChessPuzzlesScreen
import com.example.ui.screens.GameAnalysisScreen
import com.example.ui.screens.HowToPlayScreen
import com.example.ui.screens.ImproveYourChessScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.PlaySetupHubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.LeovanChessTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.LeovanChessViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRootWithAuthGate()
        }
    }
}

@Composable
fun AppRootWithAuthGate() {
    val auth = remember { runCatching { Firebase.auth }.getOrNull() }
    val currentUser by if (auth != null) {
        auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    } else {
        remember { mutableStateOf(null) }
    }
    var bypassAuthForOffline by remember { mutableStateOf(false) }

    if (currentUser == null && !bypassAuthForOffline) {
        LeovanChessTheme(themeOption = AppThemeOption.CLASSIC_WOOD) {
            SignInScreen(
                onAuthSuccess = { bypassAuthForOffline = false },
                onContinueOffline = { bypassAuthForOffline = true }
            )
        }
    } else {
        val userKey = currentUser?.uid ?: "offline_local_player"
        key(userKey) {
            val chessViewModel: LeovanChessViewModel = viewModel(
                key = userKey,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val cloudRepo = runCatching {
                            val databaseId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(databaseId)
                            ChessCloudRepository(db)
                        }.getOrNull()
                        LeovanChessViewModel(app, cloudRepo)
                    }
                }
            )
            LeovanChessApp(
                chessViewModel = chessViewModel,
                isCloudSignedIn = currentUser != null,
                cloudUserEmail = currentUser?.email,
                onRequestCloudAuthAction = {
                    if (currentUser == null) {
                        bypassAuthForOffline = false
                    }
                }
            )
        }
    }
}

@Composable
fun LeovanChessApp(
    chessViewModel: LeovanChessViewModel = viewModel(),
    isCloudSignedIn: Boolean = false,
    cloudUserEmail: String? = null,
    onRequestCloudAuthAction: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val profileRaw by chessViewModel.profileState.collectAsStateWithLifecycle()
    val profile = profileRaw ?: PlayerProfileEntity()
    val currentScreen by chessViewModel.currentScreen.collectAsStateWithLifecycle()
    val ongoingGame by chessViewModel.ongoingSavedGame.collectAsStateWithLifecycle()
    val completedGames by chessViewModel.completedGames.collectAsStateWithLifecycle()
    val lessonProgress by chessViewModel.lessonProgress.collectAsStateWithLifecycle()
    val puzzleProgress by chessViewModel.puzzleProgress.collectAsStateWithLifecycle()
    val matchState by chessViewModel.matchState.collectAsStateWithLifecycle()
    val analysisReport by chessViewModel.analysisReport.collectAsStateWithLifecycle()
    val isAnalyzing by chessViewModel.isAnalyzing.collectAsStateWithLifecycle()
    val onlineConnState by chessViewModel.onlineClient.connectionState.collectAsStateWithLifecycle()

    val activeTheme = runCatching {
        AppThemeOption.valueOf(profile.selectedTheme)
    }.getOrDefault(AppThemeOption.CLASSIC_WOOD)

    val animDurationMs = when (profile.animationIntensity) {
        "SUBTLE" -> 80
        "DYNAMIC" -> 260
        else -> 170
    }

    // BackHandler on all secondary screens
    BackHandler(enabled = currentScreen != AppScreen.MAIN_MENU) {
        chessViewModel.navigateBack()
    }

    LeovanChessTheme(themeOption = activeTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(
                    targetState = currentScreen,
                    animationSpec = tween(durationMillis = animDurationMs.coerceAtLeast(100)),
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        AppScreen.MAIN_MENU -> {
                            MainMenuScreen(
                                profile = profile,
                                ongoingGame = ongoingGame,
                                lessonProgress = lessonProgress,
                                onNavigate = { chessViewModel.navigateTo(it) },
                                onQuickPlay = { diff, color, tc, beg ->
                                    chessViewModel.startVsComputerMatch(
                                        difficulty = diff,
                                        colorChoice = color,
                                        timeControl = tc,
                                        beginnerMode = beg
                                    )
                                },
                                onResumeOngoingGame = { chessViewModel.resumeOngoingSavedGame() },
                                onCycleTheme = { chessViewModel.updateTheme(it) }
                            )
                        }

                        AppScreen.PLAY_HUB -> {
                            PlaySetupHubScreen(
                                profile = profile,
                                onlineConnectionState = onlineConnState,
                                onBack = { chessViewModel.navigateBack() },
                                onStartVsComputer = { diff, color, tc, beg, thinkMs ->
                                    chessViewModel.startVsComputerMatch(diff, color, tc, beg, thinkMs)
                                },
                                onStartLocal1v1 = { wName, bName, tc, autoRot, beg ->
                                    chessViewModel.startLocal1v1Match(wName, bName, tc, autoRot, beg)
                                },
                                onCreateOnlineRoom = { hostName, tc ->
                                    chessViewModel.createAuthoritativeOnlineRoom(hostName, tc)
                                },
                                onJoinOnlineRoom = { code, guestName, tc ->
                                    chessViewModel.joinAuthoritativeOnlineRoom(code, guestName, tc)
                                },
                                onQuickOnlineMatch = { name, tc ->
                                    chessViewModel.requestOnlineQuickMatch(name, tc)
                                }
                            )
                        }

                        AppScreen.ACTIVE_GAME -> {
                            ActiveChessGameScreen(
                                matchState = matchState,
                                profile = profile,
                                onBackToMenu = { chessViewModel.navigateHome() },
                                onSquareClicked = { chessViewModel.onSquareClicked(it) },
                                onPieceDragged = { from, to ->
                                    chessViewModel.onSquareClicked(from)
                                    chessViewModel.onSquareClicked(to)
                                },
                                onSelectPromotion = { chessViewModel.onPromotionPieceSelected(it) },
                                onDismissPromotion = { chessViewModel.dismissPromotionDialog() },
                                onTogglePause = { chessViewModel.togglePauseMatch() },
                                onFlipBoard = { chessViewModel.flipBoardManually() },
                                onToggleBeginnerMode = { chessViewModel.toggleBeginnerModeInGame() },
                                onRequestHint = { chessViewModel.requestBeginnerHint() },
                                onRequestUndo = { chessViewModel.requestUndoMove() },
                                onRespondUndoConsent = { chessViewModel.respondToLocalUndoConsent(it) },
                                onOfferOrClaimDraw = { chessViewModel.offerOrClaimDraw() },
                                onRespondDrawOffer = { chessViewModel.respondToDrawOffer(it) },
                                onResign = { chessViewModel.resignMatch() },
                                onRestartMatch = { chessViewModel.restartCurrentMatch() },
                                onAnalyzeGame = { chessViewModel.launchGameAnalysis() },
                                onImportFenPgn = { chessViewModel.importFenOrPgnIntoMatch(it) }
                            )
                        }

                        AppScreen.GAME_ANALYSIS -> {
                            GameAnalysisScreen(
                                matchState = matchState,
                                report = analysisReport,
                                isAnalyzing = isAnalyzing,
                                onBack = { chessViewModel.navigateBack() }
                            )
                        }

                        AppScreen.CHESS_ACADEMY -> {
                            ChessAcademyScreen(
                                lessonProgress = lessonProgress,
                                onBack = { chessViewModel.navigateBack() },
                                onCompleteLesson = { chessViewModel.completeAcademyLesson(it) }
                            )
                        }

                        AppScreen.PUZZLES -> {
                            ChessPuzzlesScreen(
                                profile = profile,
                                puzzleProgress = puzzleProgress,
                                onBack = { chessViewModel.navigateBack() },
                                onPuzzleSolved = { puz, att, daily ->
                                    chessViewModel.recordPuzzleSolved(puz, att, daily)
                                },
                                onPuzzleMistake = { chessViewModel.recordPuzzleMistake() }
                            )
                        }

                        AppScreen.HOW_TO_PLAY -> {
                            HowToPlayScreen(onBack = { chessViewModel.navigateBack() })
                        }

                        AppScreen.IMPROVE_YOUR_CHESS -> {
                            ImproveYourChessScreen(
                                profile = profile,
                                onBack = { chessViewModel.navigateBack() },
                                onToggleMilestone = { chessViewModel.toggleChecklistMilestone(it) }
                            )
                        }

                        AppScreen.STATISTICS -> {
                            StatisticsScreen(
                                profile = profile,
                                completedGames = completedGames,
                                lessonProgress = lessonProgress,
                                onBack = { chessViewModel.navigateBack() },
                                onAnalyzeSavedGame = { saved ->
                                    chessViewModel.analyzeSavedHistoricalGame(saved)
                                }
                            )
                        }

                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                profile = profile,
                                onBack = { chessViewModel.navigateBack() },
                                onSelectTheme = { chessViewModel.updateTheme(it) },
                                onUpdateProfileName = { name, av ->
                                    chessViewModel.updatePlayerProfileName(name, av)
                                },
                                onUpdateSettingsFlags = { chessViewModel.updateSettingsFlags(it) },
                                onResetAllData = { chessViewModel.resetAllData() },
                                isCloudSignedIn = isCloudSignedIn,
                                cloudUserEmail = cloudUserEmail,
                                onSignInOrOutCloud = {
                                    if (isCloudSignedIn) {
                                        signOutFromGoogle(credentialManager, coroutineScope)
                                    } else {
                                        onRequestCloudAuthAction()
                                    }
                                }
                            )
                        }

                        AppScreen.ABOUT -> {
                            AboutScreen(onBack = { chessViewModel.navigateBack() })
                        }
                    }
                }
            }
        }
    }
}
