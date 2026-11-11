package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlayerProfileEntity
import com.example.data.online.OnlineConnectionState
import com.example.engine.AiDifficulty
import com.example.engine.ChessEngine
import com.example.engine.FenPgnCodec
import com.example.engine.PieceColor
import com.example.engine.PieceType
import com.example.engine.Square
import com.example.engine.TimeControlPreset
import com.example.ui.components.InteractiveChessboard
import com.example.ui.components.PawnPromotionDialog
import com.example.ui.components.StauntonPieceIcon
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalChessThemeColors
import com.example.viewmodel.ActiveMatchUiState
import com.example.viewmodel.ColorSelectionChoice
import com.example.viewmodel.GameMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaySetupHubScreen(
    profile: PlayerProfileEntity,
    onlineConnectionState: OnlineConnectionState,
    onBack: () -> Unit,
    onStartVsComputer: (AiDifficulty, ColorSelectionChoice, TimeControlPreset, Boolean, Long) -> Unit,
    onStartLocal1v1: (String, String, TimeControlPreset, Boolean, Boolean) -> Unit,
    onCreateOnlineRoom: (String, TimeControlPreset) -> Unit,
    onJoinOnlineRoom: (String, String, TimeControlPreset) -> Result<Unit>,
    onQuickOnlineMatch: (String, TimeControlPreset) -> Unit
) {
    val themeColors = LocalChessThemeColors.current

    var selectedMode by remember { mutableStateOf(GameMode.VS_COMPUTER) }
    var selectedDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    var selectedColor by remember { mutableStateOf(ColorSelectionChoice.WHITE) }
    var selectedTimePreset by remember { mutableStateOf(TimeControlPreset.ALL_PRESETS[6]) } // 10+0
    var beginnerMode by remember { mutableStateOf(profile.beginnerAssistanceEnabled) }
    var aiThinkSeconds by remember { mutableFloatStateOf(selectedDifficulty.defaultThinkTimeMs / 1000f) }

    // Pengaturan Lokal 1 vs 1
    var whiteLocalName by remember { mutableStateOf(profile.playerName) }
    var blackLocalName by remember { mutableStateOf("Pemain 2") }
    var autoRotateLocal by remember { mutableStateOf(true) }

    // Pengaturan Online 1 vs 1
    var onlinePlayerName by remember { mutableStateOf(profile.playerName) }
    var joinRoomCodeInput by remember { mutableStateOf("") }
    var joinRoomError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("play_hub_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = themeColors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Pengaturan Permainan",
                            style = MaterialTheme.typography.headlineMedium,
                            color = themeColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pilih mode, waktu, dan tingkat kesulitan catur",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }

            // Tab Pilihan Mode
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeTabButton(
                        title = "Komputer",
                        icon = Icons.Default.Computer,
                        selected = selectedMode == GameMode.VS_COMPUTER,
                        onClick = { selectedMode = GameMode.VS_COMPUTER },
                        tag = "tab_vs_computer",
                        modifier = Modifier.weight(1f)
                    )
                    ModeTabButton(
                        title = "Lokal 1v1",
                        icon = Icons.Default.People,
                        selected = selectedMode == GameMode.LOCAL_1V1,
                        onClick = { selectedMode = GameMode.LOCAL_1V1 },
                        tag = "tab_local_1v1",
                        modifier = Modifier.weight(1f)
                    )
                    ModeTabButton(
                        title = "Online 1v1",
                        icon = Icons.Default.Public,
                        selected = selectedMode == GameMode.ONLINE_1V1,
                        onClick = { selectedMode = GameMode.ONLINE_1V1 },
                        tag = "tab_online_1v1",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Pengaturan Khusus Berdasarkan Mode
            if (selectedMode == GameMode.VS_COMPUTER) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text(
                                text = "Tingkat Kesulitan AI",
                                style = MaterialTheme.typography.titleMedium,
                                color = themeColors.accentPrimary
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AiDifficulty.entries.forEach { diff ->
                                    FilterChip(
                                        selected = selectedDifficulty == diff,
                                        onClick = {
                                            selectedDifficulty = diff
                                            aiThinkSeconds = diff.defaultThinkTimeMs / 1000f
                                        },
                                        label = {
                                            Text("${diff.levelNumber}. ${diff.displayName} (${diff.eloEstimate})")
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = themeColors.accentPrimary,
                                            selectedLabelColor = themeColors.backgroundPrimary
                                        ),
                                        modifier = Modifier.testTag("ai_diff_${diff.name.lowercase()}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = selectedDifficulty.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = themeColors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Waktu Berpikir AI: ${"%.1f".format(aiThinkSeconds)} detik",
                                style = MaterialTheme.typography.labelLarge,
                                color = themeColors.textPrimary
                            )
                            Slider(
                                value = aiThinkSeconds,
                                onValueChange = { aiThinkSeconds = it },
                                valueRange = 0.3f..3.5f
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Pilih Warna Bidak",
                                style = MaterialTheme.typography.titleMedium,
                                color = themeColors.accentPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ColorSelectionChoice.entries.forEach { choice ->
                                    FilterChip(
                                        selected = selectedColor == choice,
                                        onClick = { selectedColor = choice },
                                        label = { Text(choice.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = themeColors.accentPrimary,
                                            selectedLabelColor = themeColors.backgroundPrimary
                                        ),
                                        modifier = Modifier.testTag("color_choice_${choice.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (selectedMode == GameMode.LOCAL_1V1) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text(
                                text = "Pemain Lokal (1 Perangkat)",
                                style = MaterialTheme.typography.titleMedium,
                                color = themeColors.accentPrimary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = whiteLocalName,
                                onValueChange = { whiteLocalName = it },
                                label = { Text("Nama Pemain Putih") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("local_white_name_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = blackLocalName,
                                onValueChange = { blackLocalName = it },
                                label = { Text("Nama Pemain Hitam") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("local_black_name_input")
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Putar Papan Otomatis",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = themeColors.textPrimary
                                    )
                                    Text(
                                        text = "Balik orientasi papan setiap berganti giliran",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = themeColors.textSecondary
                                    )
                                }
                                Switch(
                                    checked = autoRotateLocal,
                                    onCheckedChange = { autoRotateLocal = it },
                                    modifier = Modifier.testTag("local_auto_rotate_switch")
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text(
                                text = "Pertandingan Online 1 vs 1",
                                style = MaterialTheme.typography.titleMedium,
                                color = themeColors.accentPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Semua langkah divalidasi secara sah dan disinkronkan secara langsung melalui Cloud.",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = onlinePlayerName,
                                onValueChange = { onlinePlayerName = it },
                                label = { Text("Nama Tampilan Anda") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("online_name_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onQuickOnlineMatch(onlinePlayerName, selectedTimePreset)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = themeColors.accentPrimary,
                                        contentColor = themeColors.backgroundPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("online_quick_match_button")
                                ) {
                                    Text("Cari Lawan", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onCreateOnlineRoom(onlinePlayerName, selectedTimePreset)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("online_create_room_button")
                                ) {
                                    Text("Buat Ruangan", color = themeColors.textPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = joinRoomCodeInput,
                                    onValueChange = {
                                        joinRoomCodeInput = it.uppercase()
                                        joinRoomError = null
                                    },
                                    label = { Text("Kode Ruangan (mis. LV-1024)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("online_join_code_input")
                                )
                                Button(
                                    onClick = {
                                        val res = onJoinOnlineRoom(joinRoomCodeInput, onlinePlayerName, selectedTimePreset)
                                        if (res.isFailure) {
                                            joinRoomError = res.exceptionOrNull()?.message ?: "Gagal bergabung ke ruangan"
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("online_join_room_button")
                                ) {
                                    Text("Gabung")
                                }
                            }

                            if (joinRoomError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = joinRoomError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeColors.statusDanger
                                )
                            }
                        }
                    }
                }
            }

            // Pilihan Waktu Catur & Bantuan Pemula
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = "Waktu Catur (Menit + Tambahan Detik)",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.accentPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeControlPreset.ALL_PRESETS.forEach { preset ->
                                FilterChip(
                                    selected = selectedTimePreset.id == preset.id,
                                    onClick = { selectedTimePreset = preset },
                                    label = {
                                        Text("${preset.name} (${preset.category.label})")
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = themeColors.accentPrimary,
                                        selectedLabelColor = themeColors.backgroundPrimary
                                    ),
                                    modifier = Modifier.testTag("tc_${preset.id}")
                                )
                            }
                        }

                        if (selectedMode != GameMode.ONLINE_1V1) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mode Bantuan Pemula",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = themeColors.textPrimary
                                    )
                                    Text(
                                        text = "Tampilkan langkah sah, indikator ancaman, dan tips pelatih",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = themeColors.textSecondary
                                    )
                                }
                                Switch(
                                    checked = beginnerMode,
                                    onCheckedChange = { beginnerMode = it },
                                    modifier = Modifier.testTag("setup_beginner_mode_switch")
                                )
                            }
                        }
                    }
                }
            }

            // Tombol Mulai Permainan
            if (selectedMode != GameMode.ONLINE_1V1) {
                item {
                    Button(
                        onClick = {
                            if (selectedMode == GameMode.VS_COMPUTER) {
                                onStartVsComputer(
                                    selectedDifficulty,
                                    selectedColor,
                                    selectedTimePreset,
                                    beginnerMode,
                                    (aiThinkSeconds * 1000).toLong()
                                )
                            } else {
                                onStartLocal1v1(
                                    whiteLocalName,
                                    blackLocalName,
                                    selectedTimePreset,
                                    autoRotateLocal,
                                    beginnerMode
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.accentPrimary,
                            contentColor = themeColors.backgroundPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_configured_match_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mulai Permainan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ModeTabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalChessThemeColors.current
    val bg = if (selected) themeColors.accentPrimary else themeColors.surfaceCard
    val fg = if (selected) themeColors.backgroundPrimary else themeColors.textPrimary

    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, if (selected) themeColors.accentPrimary else themeColors.surfaceBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// LAYAR PERMAINAN CATUR AKTIF — BERSIH, RAPI, DAN MUDAH DIMENGERTI
// ============================================================================

@Composable
fun ActiveChessGameScreen(
    matchState: ActiveMatchUiState,
    profile: PlayerProfileEntity,
    onBackToMenu: () -> Unit,
    onSquareClicked: (Square) -> Unit,
    onPieceDragged: (Square, Square) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onTogglePause: () -> Unit,
    onFlipBoard: () -> Unit,
    onToggleBeginnerMode: () -> Unit,
    onRequestHint: () -> Unit,
    onRequestUndo: () -> Unit,
    onRespondUndoConsent: (Boolean) -> Unit,
    onOfferOrClaimDraw: () -> Unit,
    onRespondDrawOffer: (Boolean) -> Unit,
    onResign: () -> Unit,
    onRestartMatch: () -> Unit,
    onAnalyzeGame: () -> Unit,
    onImportFenPgn: (String) -> Boolean
) {
    val themeColors = LocalChessThemeColors.current
    val clipboard = LocalClipboardManager.current
    val board = matchState.boardState

    var showResignDialog by remember { mutableStateOf(false) }
    var showFenPgnDialog by remember { mutableStateOf(false) }
    var fenPgnInputText by remember { mutableStateOf("") }
    var fenPgnFeedback by remember { mutableStateOf<String?>(null) }

    val topPlayerColor = if (matchState.isBoardFlipped) PieceColor.WHITE else PieceColor.BLACK
    val bottomPlayerColor = topPlayerColor.opposite

    val threatenedFriendlySquares = remember(board, matchState.beginnerModeEnabled) {
        if (matchState.beginnerModeEnabled) {
            ChessEngine.getThreatenedFriendlySquares(board)
        } else emptySet()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Baris Atas: Navigasi & Tombol Cepat
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToMenu,
                        modifier = Modifier.testTag("game_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = themeColors.textPrimary
                        )
                    }
                    Column {
                        Text(
                            text = matchState.gameMode.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.textPrimary
                        )
                        val subLabel = when (matchState.gameMode) {
                            GameMode.VS_COMPUTER -> "AI ${matchState.aiDifficulty.displayName} (${matchState.aiDifficulty.eloEstimate})"
                            GameMode.LOCAL_1V1 -> "1 Perangkat • Giliran ${board.turn.displayName}"
                            GameMode.ONLINE_1V1 -> "Ruangan ${matchState.onlineAuthoritativeRoomCode ?: "Online"}"
                        }
                        Text(
                            text = subLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.accentPrimary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onToggleBeginnerMode,
                        modifier = Modifier.testTag("toggle_beginner_assist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Bantuan Pemula",
                            tint = if (matchState.beginnerModeEnabled) themeColors.accentPrimary else themeColors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = { showFenPgnDialog = true },
                        modifier = Modifier.testTag("fen_pgn_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "FEN / PGN",
                            tint = themeColors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.testTag("pause_game_button")
                    ) {
                        Icon(
                            imageVector = if (matchState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Jeda",
                            tint = themeColors.textPrimary
                        )
                    }
                }
            }

            // 2. Panel Pemain Atas (Lawan)
            PlayerClockAndCapturesBar(
                playerName = if (topPlayerColor == PieceColor.WHITE) matchState.whitePlayerName else matchState.blackPlayerName,
                color = topPlayerColor,
                isTurn = board.turn == topPlayerColor && !board.isGameOver,
                isThinking = matchState.isAiThinking && topPlayerColor != matchState.humanColor,
                timeRemainingMs = if (topPlayerColor == PieceColor.WHITE) matchState.whiteTimeRemainingMs else matchState.blackTimeRemainingMs,
                isUnlimitedTime = matchState.timeControl.isUnlimited,
                capturedPieces = if (topPlayerColor == PieceColor.WHITE) board.capturedByWhite else board.capturedByBlack,
                materialDiff = board.materialBalanceFor(topPlayerColor)
            )

            // 3. Papan Catur 8x8 Interaktif
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentAlignment = Alignment.Center
            ) {
                InteractiveChessboard(
                    boardState = board,
                    selectedSquare = matchState.selectedSquare,
                    legalMovesForSelected = matchState.legalMovesForSelected,
                    flipped = matchState.isBoardFlipped,
                    showLegalIndicators = profile.showLegalMoveIndicators,
                    beginnerModeEnabled = matchState.beginnerModeEnabled,
                    threatenedSquares = threatenedFriendlySquares,
                    hintMove = matchState.hintMove,
                    animationDurationMs = when (profile.animationIntensity) {
                        "SUBTLE" -> 90
                        "DYNAMIC" -> 240
                        else -> 180
                    },
                    interactive = !matchState.isPaused && !board.isGameOver,
                    onSquareClicked = onSquareClicked,
                    onPieceDragged = onPieceDragged,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Panel Pemain Bawah (Anda)
            PlayerClockAndCapturesBar(
                playerName = if (bottomPlayerColor == PieceColor.WHITE) matchState.whitePlayerName else matchState.blackPlayerName,
                color = bottomPlayerColor,
                isTurn = board.turn == bottomPlayerColor && !board.isGameOver,
                isThinking = matchState.isAiThinking && bottomPlayerColor != matchState.humanColor,
                timeRemainingMs = if (bottomPlayerColor == PieceColor.WHITE) matchState.whiteTimeRemainingMs else matchState.blackTimeRemainingMs,
                isUnlimitedTime = matchState.timeControl.isUnlimited,
                capturedPieces = if (bottomPlayerColor == PieceColor.WHITE) board.capturedByWhite else board.capturedByBlack,
                materialDiff = board.materialBalanceFor(bottomPlayerColor)
            )

            // 5. Pita Riwayat Langkah (SAN) & Pesan Pelatih
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MoveHistoryHorizontalStrip(moves = board.moveHistory)

                if (matchState.beginnerModeEnabled || board.isInCheck || board.isGameOver) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (board.isInCheck) {
                                themeColors.statusDanger.copy(alpha = 0.16f)
                            } else {
                                themeColors.surfaceCard
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (board.isInCheck) themeColors.statusDanger else themeColors.surfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = matchState.beginnerCoachMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (matchState.beginnerModeEnabled && !board.isGameOver) {
                                TextButton(
                                    onClick = onRequestHint,
                                    modifier = Modifier.testTag("beginner_hint_button")
                                ) {
                                    Text("Saran", color = themeColors.accentPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 6. Baris Kontrol Bawah
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionControlBtn(
                        label = "Urung",
                        icon = Icons.AutoMirrored.Filled.Undo,
                        onClick = onRequestUndo,
                        tag = "game_undo_button",
                        modifier = Modifier.weight(1f)
                    )
                    ActionControlBtn(
                        label = "Putar",
                        icon = Icons.Default.RotateRight,
                        onClick = onFlipBoard,
                        tag = "game_flip_button",
                        modifier = Modifier.weight(1f)
                    )
                    ActionControlBtn(
                        label = if (board.canClaimFiftyMoveDraw || board.canClaimThreefoldDraw) "Klaim Remis" else "Remis",
                        icon = Icons.Default.Handshake,
                        onClick = onOfferOrClaimDraw,
                        tag = "game_draw_button",
                        modifier = Modifier.weight(1f)
                    )
                    ActionControlBtn(
                        label = "Ulang",
                        icon = Icons.Default.Refresh,
                        onClick = onRestartMatch,
                        tag = "game_restart_button",
                        modifier = Modifier.weight(1f)
                    )
                    ActionControlBtn(
                        label = "Menyerah",
                        icon = Icons.Default.Flag,
                        onClick = { showResignDialog = true },
                        tag = "game_resign_button",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Dialog Promosi Pion
        if (matchState.pendingPromotionBaseMove != null) {
            PawnPromotionDialog(
                moverColor = board.turn,
                beginnerModeEnabled = matchState.beginnerModeEnabled,
                onSelectPromotion = onSelectPromotion,
                onDismiss = onDismissPromotion
            )
        }

        // Dialog Persetujuan Urung Langkah (Lokal 1v1)
        if (matchState.undoConsentPendingFrom != null) {
            AlertDialog(
                onDismissRequest = { onRespondUndoConsent(false) },
                title = { Text("Permintaan Urungkan Langkah") },
                text = {
                    Text("Pemain lawan meminta izin untuk mengurungkan langkah terakhir. Apakah ${matchState.undoConsentPendingFrom.displayName} setuju?")
                },
                confirmButton = {
                    Button(onClick = { onRespondUndoConsent(true) }) {
                        Text("Setuju")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onRespondUndoConsent(false) }) {
                        Text("Tolak")
                    }
                }
            )
        }

        // Dialog Tawaran Remis
        if (matchState.drawOfferPendingFrom != null) {
            AlertDialog(
                onDismissRequest = { onRespondDrawOffer(false) },
                title = { Text("Tawaran Remis") },
                text = {
                    Text("${matchState.drawOfferPendingFrom.displayName} menawarkan hasil Remis (Seri). Apakah Anda menerima?")
                },
                confirmButton = {
                    Button(onClick = { onRespondDrawOffer(true) }) {
                        Text("Terima Remis")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onRespondDrawOffer(false) }) {
                        Text("Lanjut Main")
                    }
                }
            )
        }

        // Dialog Konfirmasi Menyerah
        if (showResignDialog) {
            AlertDialog(
                onDismissRequest = { showResignDialog = false },
                title = { Text("Menyerah dari Permainan?") },
                text = { Text("Apakah Anda yakin ingin menyerah pada pertandingan ini?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showResignDialog = false
                            onResign()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.statusDanger)
                    ) {
                        Text("Menyerah")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResignDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog Ekspor / Impor FEN & PGN
        if (showFenPgnDialog) {
            val currentFen = FenPgnCodec.toFen(board)
            val currentPgn = FenPgnCodec.exportPgn(
                state = board,
                whiteName = matchState.whitePlayerName,
                blackName = matchState.blackPlayerName
            )
            AlertDialog(
                onDismissRequest = { showFenPgnDialog = false },
                title = { Text("Bagikan & Impor FEN / PGN") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "FEN Saat Ini:",
                            style = MaterialTheme.typography.labelLarge,
                            color = themeColors.accentPrimary
                        )
                        Text(
                            text = currentFen,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFamily
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(currentFen))
                                    fenPgnFeedback = "FEN disalin ke papan klip!"
                                }
                            ) {
                                Text("Salin FEN")
                            }
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(currentPgn))
                                    fenPgnFeedback = "PGN disalin ke papan klip!"
                                }
                            ) {
                                Text("Salin PGN")
                            }
                        }

                        OutlinedTextField(
                            value = fenPgnInputText,
                            onValueChange = { fenPgnInputText = it },
                            label = { Text("Tempel FEN atau PGN untuk dimuat") },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (fenPgnFeedback != null) {
                            Text(
                                text = fenPgnFeedback!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.accentPrimary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (fenPgnInputText.isNotBlank()) {
                                val ok = onImportFenPgn(fenPgnInputText)
                                if (ok) {
                                    showFenPgnDialog = false
                                    fenPgnInputText = ""
                                    fenPgnFeedback = null
                                } else {
                                    fenPgnFeedback = "Format FEN atau PGN tidak valid."
                                }
                            } else {
                                showFenPgnDialog = false
                            }
                        }
                    ) {
                        Text(if (fenPgnInputText.isNotBlank()) "Muat Posisi" else "Tutup")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFenPgnDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Modal Hasil Akhir Permainan
        if (board.isGameOver) {
            GameOverSummaryDialog(
                matchState = matchState,
                onPlayAgain = onRestartMatch,
                onAnalyzeGame = onAnalyzeGame,
                onBackToMenu = onBackToMenu
            )
        }
    }
}

@Composable
private fun PlayerClockAndCapturesBar(
    playerName: String,
    color: PieceColor,
    isTurn: Boolean,
    isThinking: Boolean,
    timeRemainingMs: Long,
    isUnlimitedTime: Boolean,
    capturedPieces: List<com.example.engine.ChessPiece>,
    materialDiff: Int
) {
    val themeColors = LocalChessThemeColors.current
    val totalSeconds = (timeRemainingMs / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    val clockText = if (isUnlimitedTime) "∞" else "%02d:%02d".format(mins, secs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isTurn) themeColors.surfaceElevated else themeColors.surfaceCard)
            .border(
                width = if (isTurn) 1.5.dp else 1.dp,
                color = if (isTurn) themeColors.accentPrimary else themeColors.surfaceBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(themeColors.backgroundSecondary),
                contentAlignment = Alignment.Center
            ) {
                StauntonPieceIcon(
                    piece = com.example.engine.ChessPiece(PieceType.KING, color),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = playerName,
                        style = MaterialTheme.typography.titleSmall,
                        color = themeColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isThinking) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• Berpikir...",
                            style = MaterialTheme.typography.labelMedium,
                            color = themeColors.accentPrimary
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    capturedPieces.take(10).forEach { cap ->
                        StauntonPieceIcon(piece = cap, modifier = Modifier.size(14.dp))
                    }
                    if (materialDiff > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+$materialDiff",
                            style = MaterialTheme.typography.labelMedium,
                            color = themeColors.accentPrimary
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (!isUnlimitedTime && timeRemainingMs < 30_000L) {
                        themeColors.statusDanger.copy(alpha = 0.25f)
                    } else {
                        themeColors.backgroundPrimary
                    }
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = clockText,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFamily,
                color = if (!isUnlimitedTime && timeRemainingMs < 30_000L) {
                    themeColors.statusDanger
                } else if (isTurn) {
                    themeColors.accentPrimary
                } else {
                    themeColors.textSecondary
                }
            )
        }
    }
}

@Composable
private fun MoveHistoryHorizontalStrip(moves: List<com.example.engine.ChessMove>) {
    val themeColors = LocalChessThemeColors.current
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(themeColors.backgroundSecondary)
            .padding(horizontal = 12.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (moves.isEmpty()) {
            Text(
                text = "Notasi langkah (SAN) akan muncul di sini saat permainan dimulai",
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.textSecondary
            )
        } else {
            val movePairs = moves.chunked(2)
            movePairs.forEachIndexed { index, pair ->
                val whiteSan = pair.getOrNull(0)?.san ?: ""
                val blackSan = pair.getOrNull(1)?.san ?: ""
                Text(
                    text = "${index + 1}. $whiteSan $blackSan".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = if (index == movePairs.lastIndex) themeColors.accentPrimary else themeColors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun ActionControlBtn(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalChessThemeColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(themeColors.surfaceCard)
            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = themeColors.textPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = themeColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GameOverSummaryDialog(
    matchState: ActiveMatchUiState,
    onPlayAgain: () -> Unit,
    onAnalyzeGame: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    val board = matchState.boardState
    val winner = board.winner
    val headline = when {
        board.termination.isDraw -> "PERMAINAN REMIS"
        winner == PieceColor.WHITE -> "PUTIH MENANG!"
        winner == PieceColor.BLACK -> "HITAM MENANG!"
        else -> "PERMAINAN SELESAI"
    }

    AlertDialog(
        onDismissRequest = {},
        containerColor = themeColors.surfaceCard,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = themeColors.accentPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = board.termination.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = themeColors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${matchState.whitePlayerName} (Putih) vs ${matchState.blackPlayerName} (Hitam)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.textSecondary
                )
                Text(
                    text = "Total Langkah: ${(board.moveHistory.size + 1) / 2} • Durasi: ${matchState.elapsedMatchSeconds} detik",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.textSecondary
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAnalyzeGame,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.accentPrimary,
                        contentColor = themeColors.backgroundPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_over_analyze_button")
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analisis Permainan", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPlayAgain,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("game_over_rematch_button")
                    ) {
                        Text("Main Lagi", color = themeColors.textPrimary)
                    }
                    OutlinedButton(
                        onClick = onBackToMenu,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("game_over_menu_button")
                    ) {
                        Text("Menu Utama", color = themeColors.textPrimary)
                    }
                }
            }
        }
    )
}
