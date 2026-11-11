package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.ChessEngine
import com.example.engine.GameAnalysisReport
import com.example.engine.MoveClassification
import com.example.ui.components.InteractiveChessboard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalChessThemeColors
import com.example.viewmodel.ActiveMatchUiState

@Composable
fun GameAnalysisScreen(
    matchState: ActiveMatchUiState,
    report: GameAnalysisReport?,
    isAnalyzing: Boolean,
    onBack: () -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    val moveHistory = matchState.boardState.moveHistory

    // Indeks ply yang sedang ditinjau: 0 = posisi awal, N = setelah langkah ke-N
    var selectedPlyIndex by remember(moveHistory.size) { mutableIntStateOf(moveHistory.size) }

    val boardAtSelectedPly = remember(moveHistory, selectedPlyIndex) {
        var b = ChessEngine.createInitialState()
        for (i in 0 until selectedPlyIndex.coerceIn(0, moveHistory.size)) {
            b = ChessEngine.makeMove(b, moveHistory[i], computeSan = false)
        }
        b
    }

    val currentAnalyzedPly = report?.analyzedPlies?.getOrNull(selectedPlyIndex - 1)

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
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("analysis_back_button")
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
                            text = "Analisis Pasca-Permainan",
                            style = MaterialTheme.typography.headlineMedium,
                            color = themeColors.textPrimary
                        )
                        Text(
                            text = report?.engineIdentifier ?: "Menganalisis langkah demi langkah...",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.accentPrimary
                        )
                    }
                }
            }

            if (isAnalyzing) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = themeColors.accentPrimary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Menghitung akurasi, saran langkah terbaik, dan grafik evaluasi...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = themeColors.textPrimary
                            )
                        }
                    }
                }
            }

            // Ringkasan Akurasi & Klasifikasi Langkah
            if (report != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "${matchState.whitePlayerName} (Putih)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = themeColors.textSecondary
                                    )
                                    Text(
                                        text = "${"%.1f".format(report.whiteAccuracyPercent)}% Akurasi",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = themeColors.textPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${matchState.blackPlayerName} (Hitam)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = themeColors.textSecondary
                                    )
                                    Text(
                                        text = "${"%.1f".format(report.blackAccuracyPercent)}% Akurasi",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = themeColors.textPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            EvaluationAdvantageChart(
                                evalCurveCp = report.evaluationCurveWhiteCp,
                                selectedPly = selectedPlyIndex
                            )
                        }
                    }
                }
            }

            // Papan Replay & Bar Evaluasi Vertikal
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val evalCp = currentAnalyzedPly?.evalAfterWhiteCp ?: 0
                    VerticalEvaluationBar(evalWhiteCp = evalCp)

                    Box(modifier = Modifier.weight(1f)) {
                        InteractiveChessboard(
                            boardState = boardAtSelectedPly,
                            selectedSquare = null,
                            legalMovesForSelected = emptyList(),
                            flipped = matchState.isBoardFlipped,
                            showLegalIndicators = false,
                            beginnerModeEnabled = false,
                            hintMove = currentAnalyzedPly?.bestEngineAlternativeMove,
                            interactive = false,
                            onSquareClicked = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Kontrol Pemutaran Langkah
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (currentAnalyzedPly != null) {
                            val badgeColor = classificationColor(currentAnalyzedPly.classification)
                            val badgeLabel = classificationIndonesianLabel(currentAnalyzedPly.classification)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Langkah ${currentAnalyzedPly.plyIndex}: ${currentAnalyzedPly.move.san}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = themeColors.textPrimary
                                )
                                Text(
                                    text = "${currentAnalyzedPly.classification.badge} $badgeLabel",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = badgeColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentAnalyzedPly.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { selectedPlyIndex = 0 },
                                enabled = selectedPlyIndex > 0,
                                modifier = Modifier.testTag("analysis_first_ply")
                            ) {
                                Icon(Icons.Default.FirstPage, contentDescription = "Awal", tint = themeColors.textPrimary)
                            }
                            IconButton(
                                onClick = { selectedPlyIndex = (selectedPlyIndex - 1).coerceAtLeast(0) },
                                enabled = selectedPlyIndex > 0,
                                modifier = Modifier.testTag("analysis_prev_ply")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Sebelumnya", tint = themeColors.textPrimary)
                            }
                            Text(
                                text = "Langkah $selectedPlyIndex / ${moveHistory.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFamily,
                                color = themeColors.accentPrimary
                            )
                            IconButton(
                                onClick = { selectedPlyIndex = (selectedPlyIndex + 1).coerceAtMost(moveHistory.size) },
                                enabled = selectedPlyIndex < moveHistory.size,
                                modifier = Modifier.testTag("analysis_next_ply")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Berikutnya", tint = themeColors.textPrimary)
                            }
                            IconButton(
                                onClick = { selectedPlyIndex = moveHistory.size },
                                enabled = selectedPlyIndex < moveHistory.size,
                                modifier = Modifier.testTag("analysis_last_ply")
                            ) {
                                Icon(Icons.Default.LastPage, contentDescription = "Akhir", tint = themeColors.textPrimary)
                            }
                        }
                    }
                }
            }

            // Daftar Evaluasi Langkah demi Langkah
            if (report != null && report.analyzedPlies.isNotEmpty()) {
                item {
                    Text(
                        text = "Daftar Evaluasi Langkah",
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColors.accentPrimary
                    )
                }

                itemsIndexed(report.analyzedPlies) { idx, ply ->
                    val isSelected = selectedPlyIndex == idx + 1
                    val cColor = classificationColor(ply.classification)
                    val cLabel = classificationIndonesianLabel(ply.classification)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) themeColors.surfaceElevated else themeColors.surfaceCard)
                            .border(
                                1.dp,
                                if (isSelected) themeColors.accentPrimary else themeColors.surfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedPlyIndex = idx + 1 }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${(idx / 2) + 1}${if (idx % 2 == 0) "." else "..."} ${ply.move.san}",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = themeColors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = cLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = cColor
                            )
                        }
                        val evalPawns = ply.evalAfterWhiteCp / 100.0
                        Text(
                            text = "%+.2f".format(evalPawns),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun classificationIndonesianLabel(classification: MoveClassification): String {
    return when (classification) {
        MoveClassification.BEST_MOVE -> "Langkah Terbaik"
        MoveClassification.GOOD_MOVE -> "Langkah Solid"
        MoveClassification.INACCURACY -> "Kurang Akurat"
        MoveClassification.MISTAKE -> "Kesalahan"
        MoveClassification.BLUNDER -> "Blunder"
        MoveClassification.MISSED_OPPORTUNITY -> "Peluang Terlewat"
    }
}

@Composable
private fun classificationColor(classification: MoveClassification): Color {
    val themeColors = LocalChessThemeColors.current
    return when (classification) {
        MoveClassification.BEST_MOVE -> themeColors.statusSuccess
        MoveClassification.GOOD_MOVE -> themeColors.accentSecondary
        MoveClassification.INACCURACY -> themeColors.statusWarning
        MoveClassification.MISTAKE -> Color(0xFFFF7043)
        MoveClassification.BLUNDER -> themeColors.statusDanger
        MoveClassification.MISSED_OPPORTUNITY -> Color(0xFF00E5FF)
    }
}

@Composable
private fun VerticalEvaluationBar(evalWhiteCp: Int) {
    val themeColors = LocalChessThemeColors.current
    val clamped = evalWhiteCp.coerceIn(-1000, 1000)
    val whiteFraction = ((clamped + 1000f) / 2000f).coerceIn(0.05f, 0.95f)

    Canvas(
        modifier = Modifier
            .width(14.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(7.dp))
            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(7.dp))
    ) {
        val blackHeight = size.height * (1f - whiteFraction)
        drawRect(
            color = themeColors.evalBlackBar,
            topLeft = Offset.Zero,
            size = Size(size.width, blackHeight)
        )
        drawRect(
            color = themeColors.evalWhiteBar,
            topLeft = Offset(0f, blackHeight),
            size = Size(size.width, size.height - blackHeight)
        )
    }
}

@Composable
private fun EvaluationAdvantageChart(
    evalCurveCp: List<Int>,
    selectedPly: Int
) {
    val themeColors = LocalChessThemeColors.current
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(themeColors.backgroundSecondary)
            .padding(6.dp)
    ) {
        val midY = size.height / 2f
        drawLine(
            color = themeColors.surfaceBorder,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.5f
        )
        if (evalCurveCp.size < 2) return@Canvas

        val path = Path()
        val maxIdx = (evalCurveCp.size - 1).coerceAtLeast(1)
        evalCurveCp.forEachIndexed { i, cp ->
            val x = (i.toFloat() / maxIdx) * size.width
            val norm = (cp.coerceIn(-800, 800) / 800f)
            val y = midY - norm * (midY * 0.88f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = themeColors.accentPrimary,
            style = Stroke(width = 3f)
        )

        val selIdx = selectedPly.coerceIn(0, maxIdx)
        val selX = (selIdx.toFloat() / maxIdx) * size.width
        drawLine(
            color = themeColors.statusSuccess,
            start = Offset(selX, 0f),
            end = Offset(selX, size.height),
            strokeWidth = 2f
        )
    }
}
