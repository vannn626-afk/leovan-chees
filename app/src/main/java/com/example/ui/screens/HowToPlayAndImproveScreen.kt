package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.content.ChessAcademyCatalog
import com.example.data.local.PlayerProfileEntity
import com.example.engine.ChessEngine
import com.example.engine.ChessPiece
import com.example.engine.FenPgnCodec
import com.example.engine.PieceColor
import com.example.engine.PieceType
import com.example.engine.Square
import com.example.ui.components.InteractiveChessboard
import com.example.ui.components.StauntonPieceIcon
import com.example.ui.theme.LocalChessThemeColors

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    val themeColors = LocalChessThemeColors.current

    data class RuleGuideTopic(
        val title: String,
        val subtitle: String,
        val fen: String,
        val focusSquare: String,
        val pieceType: PieceType,
        val explanation: String
    )

    val topics = remember {
        listOf(
            RuleGuideTopic(
                title = "1. Papan Catur & Posisi Awal",
                subtitle = "64 petak (8×8) • Petak terang di sudut kanan bawah",
                fen = ChessEngine.INITIAL_FEN,
                focusSquare = "e2",
                pieceType = PieceType.KING,
                explanation = "Catur dimainkan di atas papan 8×8. Baris horizontal disebut Baris (1–8) dan lajur vertikal disebut Lajur (a–h). Putih selalu melangkah lebih dulu. Menteri selalu diletakkan sesuai warnanya (Menteri Putih di d1, Menteri Hitam di d8)."
            ),
            RuleGuideTopic(
                title = "2. Raja — Bidak Utama",
                subtitle = "Melangkah 1 petak ke segala arah yang aman",
                fen = "4k3/8/8/8/4K3/8/8/8 w - - 0 1",
                focusSquare = "e4",
                pieceType = PieceType.KING,
                explanation = "Raja dapat melangkah satu petak secara horizontal, vertikal, atau diagonal. Raja tidak boleh melangkah ke petak yang sedang diserang oleh bidak lawan."
            ),
            RuleGuideTopic(
                title = "3. Menteri (9 Poin)",
                subtitle = "Menggabungkan kekuatan Benteng & Gajah",
                fen = "6k1/8/8/8/3Q4/8/8/6K1 w - - 0 1",
                focusSquare = "d4",
                pieceType = PieceType.QUEEN,
                explanation = "Menteri adalah bidak paling kuat. Dari pusat papan, Menteri dapat mengontrol hingga 27 petak sekaligus secara lurus maupun diagonal."
            ),
            RuleGuideTopic(
                title = "4. Benteng (5 Poin)",
                subtitle = "Penguasa lajur dan baris lurus",
                fen = "6k1/8/8/8/3R4/8/8/6K1 w - - 0 1",
                focusSquare = "d4",
                pieceType = PieceType.ROOK,
                explanation = "Benteng bergerak lurus secara horizontal dan vertikal sejauh mungkin selama tidak terhalang. Sangat kuat di lajur terbuka dan saat melakukan Rokade."
            ),
            RuleGuideTopic(
                title = "5. Gajah (3 Poin)",
                subtitle = "Penembak jarak jauh diagonal",
                fen = "6k1/8/8/8/3B4/8/8/6K1 w - - 0 1",
                focusSquare = "d4",
                pieceType = PieceType.BISHOP,
                explanation = "Gajah bergerak secara diagonal sejauh mungkin dan selalu tetap berada pada warna petak awalnya sepanjang permainan."
            ),
            RuleGuideTopic(
                title = "6. Kuda (3 Poin)",
                subtitle = "Langkah bentuk L • Bisa melompati bidak lain",
                fen = "6k1/8/8/3p4/3N4/8/8/6K1 w - - 0 1",
                focusSquare = "d4",
                pieceType = PieceType.KNIGHT,
                explanation = "Kuda bergerak 2 petak ke satu arah lalu 1 petak berbelok (huruf L). Kuda adalah satu-satunya bidak yang dapat melompati bidak lain."
            ),
            RuleGuideTopic(
                title = "7. Pion (1 Poin) & En Passant",
                subtitle = "Maju lurus, makan diagonal, dan promosi di baris ke-8",
                fen = "6k1/8/8/3pP3/8/8/8/6K1 w - d6 0 1",
                focusSquare = "e5",
                pieceType = PieceType.PAWN,
                explanation = "Pion maju 1 petak (atau 2 petak dari posisi awal) dan memakan secara diagonal. Jika pion lawan melompat 2 petak melewati pion Anda di baris ke-5, Anda dapat memakannya secara En Passant!"
            ),
            RuleGuideTopic(
                title = "8. Rokade, Skak, Skakmat & Remis",
                subtitle = "Mengamankan Raja dan aturan akhir permainan",
                fen = "r3k2r/ppp2ppp/8/8/8/8/PPP2PPP/R3K2R w KQkq - 0 1",
                focusSquare = "e1",
                pieceType = PieceType.KING,
                explanation = "Rokade (O-O atau O-O-O) menggerakkan Raja 2 petak menuju Benteng dan melompatkan Benteng ke sebelahnya. Permainan berakhir dengan Skakmat (menang) atau Remis (Pat, pengulangan 3 kali, aturan 50 langkah, atau materi tidak cukup)."
            )
        )
    }

    var selectedTopicIdx by remember { mutableIntStateOf(0) }
    val activeTopic = topics[selectedTopicIdx]
    val demoBoard = remember(activeTopic) { FenPgnCodec.parseFen(activeTopic.fen) }
    val focusSq = remember(activeTopic) { Square.fromAlgebraic(activeTopic.focusSquare) }
    val demoMoves = remember(demoBoard, focusSq) {
        if (focusSq != null) ChessEngine.getLegalMovesForSquare(demoBoard, focusSq) else emptyList()
    }

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("how_to_play_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = themeColors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Cara Bermain Catur",
                            style = MaterialTheme.typography.headlineMedium,
                            color = themeColors.textPrimary
                        )
                        Text(
                            text = "Panduan Visual Aturan Internasional FIDE",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.accentPrimary
                        )
                    }
                }
            }

            // Kartu Demonstrasi Papan
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, themeColors.accentPrimary, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StauntonPieceIcon(
                                piece = ChessPiece(activeTopic.pieceType, PieceColor.WHITE),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = activeTopic.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = themeColors.textPrimary
                                )
                                Text(
                                    text = activeTopic.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeColors.accentPrimary
                                )
                            }
                        }

                        InteractiveChessboard(
                            boardState = demoBoard,
                            selectedSquare = focusSq,
                            legalMovesForSelected = demoMoves,
                            flipped = false,
                            showLegalIndicators = true,
                            beginnerModeEnabled = true,
                            interactive = false,
                            onSquareClicked = {},
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = activeTopic.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.textPrimary
                        )
                    }
                }
            }

            // Daftar Topik Panduan
            items(topics.indices.toList()) { idx ->
                val topic = topics[idx]
                val selected = idx == selectedTopicIdx
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) themeColors.surfaceElevated else themeColors.surfaceCard)
                        .border(
                            1.dp,
                            if (selected) themeColors.accentPrimary else themeColors.surfaceBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedTopicIdx = idx }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StauntonPieceIcon(
                        piece = ChessPiece(topic.pieceType, PieceColor.WHITE),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = themeColors.textPrimary
                        )
                        Text(
                            text = topic.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.textSecondary
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

// ============================================================================
// PANDUAN MENINGKATKAN KEMAMPUAN CATUR (PEMULA KE MASTER)
// ============================================================================

@Composable
fun ImproveYourChessScreen(
    profile: PlayerProfileEntity,
    onBack: () -> Unit,
    onToggleMilestone: (String) -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    val completedSet = remember(profile.completedChecklistIdsCsv) {
        profile.completedChecklistIdsCsv.split(",").filter { it.isNotBlank() }.toSet()
    }
    var selectedStage by remember { mutableIntStateOf(0) }
    val stages = listOf("Pemula", "Menengah", "Mahir")

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("improve_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = themeColors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Dari Pemula Menjadi Master",
                            style = MaterialTheme.typography.headlineMedium,
                            color = themeColors.textPrimary
                        )
                        Text(
                            text = "Peta Latihan & DaftarKebiasaan Juara",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.accentPrimary
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "5 Kebiasaan Inti Pemain Catur Kuat",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.accentPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val habits = listOf(
                            "1. Periksa Skak, Makan, dan Ancaman (CCT) milik Anda & lawan sebelum melangkah.",
                            "2. Kuasai pusat papan (e4, d4, e5, d5), kembangkan Kuda & Gajah, dan lakukan Rokade lebih awal.",
                            "3. Jangan memberikan bidak gratis—selalu cek apakah petak tujuan aman.",
                            "4. Latih 3–5 teka-teki taktik setiap hari agar pola Garpuan, Pin, dan Tusukan dikenali secara instan.",
                            "5. Gunakan fitur Analisis Pasca-Permainan setelah bertanding untuk belajar dari kesalahan."
                        )
                        habits.forEach { habit ->
                            Text(
                                text = habit,
                                style = MaterialTheme.typography.bodyMedium,
                                color = themeColors.textPrimary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    stages.forEachIndexed { idx, stage ->
                        FilterChip(
                            selected = selectedStage == idx,
                            onClick = { selectedStage = idx },
                            label = { Text(stage) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accentPrimary,
                                selectedLabelColor = themeColors.backgroundPrimary
                            )
                        )
                    }
                }
            }

            val stageEnumNames = listOf("Beginner", "Intermediate", "Advanced")
            val activeStageKey = stageEnumNames[selectedStage]
            val filteredMilestones = ChessAcademyCatalog.improvementMilestones.filter {
                it.stage == activeStageKey
            }

            items(filteredMilestones, key = { it.id }) { item ->
                val checked = completedSet.contains(item.id)
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (checked) themeColors.statusSuccess else themeColors.surfaceBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onToggleMilestone(item.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggleMilestone(item.id) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = themeColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.textSecondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
