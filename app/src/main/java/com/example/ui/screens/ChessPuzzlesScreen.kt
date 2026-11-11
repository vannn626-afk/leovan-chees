package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.content.ChessPuzzle
import com.example.data.content.ChessPuzzleCatalog
import com.example.data.content.PuzzleCategory
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PuzzleProgressEntity
import com.example.data.sound.ChessSoundManager
import com.example.engine.ChessEngine
import com.example.engine.ChessMove
import com.example.engine.FenPgnCodec
import com.example.engine.PieceColor
import com.example.engine.Square
import com.example.ui.components.InteractiveChessboard
import com.example.ui.theme.LocalChessThemeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChessPuzzlesScreen(
    profile: PlayerProfileEntity,
    puzzleProgress: List<PuzzleProgressEntity>,
    onBack: () -> Unit,
    onPuzzleSolved: (ChessPuzzle, Int, Boolean) -> Unit,
    onPuzzleMistake: () -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    val solvedSet = remember(puzzleProgress) {
        puzzleProgress.filter { it.solved }.map { it.puzzleId }.toSet()
    }

    val todayEpochDay = System.currentTimeMillis() / 86_400_000L
    val dailyPuzzle = remember(todayEpochDay) { ChessPuzzleCatalog.getDailyPuzzle(todayEpochDay) }

    var selectedCategory by remember { mutableStateOf<PuzzleCategory?>(null) }
    var activePuzzle by remember { mutableStateOf<ChessPuzzle?>(dailyPuzzle) }

    val filteredPuzzles = remember(selectedCategory) {
        if (selectedCategory == null) {
            ChessPuzzleCatalog.allPuzzles
        } else {
            ChessPuzzleCatalog.allPuzzles.filter { it.category == selectedCategory }
        }
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
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("puzzles_back_button")
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
                                text = "Teka-Teki & Tantangan",
                                style = MaterialTheme.typography.headlineMedium,
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = "${solvedSet.size} Selesai • Rating ${profile.puzzleScore}",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.accentPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(themeColors.surfaceCard)
                            .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Beruntun",
                            tint = themeColors.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile.currentPuzzleStreak}",
                            style = MaterialTheme.typography.labelLarge,
                            color = themeColors.textPrimary
                        )
                    }
                }
            }

            // Papan Teka-Teki Aktif
            if (activePuzzle != null) {
                item {
                    ActivePuzzleBoardCard(
                        puzzle = activePuzzle!!,
                        isDaily = activePuzzle!!.id == dailyPuzzle.id,
                        onSolved = { attempts ->
                            onPuzzleSolved(activePuzzle!!, attempts, activePuzzle!!.id == dailyPuzzle.id)
                        },
                        onMistake = onPuzzleMistake,
                        onNextPuzzle = {
                            val idx = filteredPuzzles.indexOfFirst { it.id == activePuzzle!!.id }
                            activePuzzle = if (idx in 0 until filteredPuzzles.lastIndex) {
                                filteredPuzzles[idx + 1]
                            } else {
                                filteredPuzzles.firstOrNull()
                            }
                        }
                    )
                }
            }

            // Filter Kategori
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Filter Kategori Taktik",
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColors.accentPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Semua (${ChessPuzzleCatalog.allPuzzles.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accentPrimary,
                                selectedLabelColor = themeColors.backgroundPrimary
                            )
                        )
                        PuzzleCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = themeColors.accentPrimary,
                                    selectedLabelColor = themeColors.backgroundPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Daftar Teka-Teki
            items(filteredPuzzles, key = { it.id }) { puzzle ->
                val isDone = solvedSet.contains(puzzle.id)
                val isSelected = activePuzzle?.id == puzzle.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) themeColors.surfaceElevated else themeColors.surfaceCard)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) themeColors.accentPrimary else themeColors.surfaceBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { activePuzzle = puzzle }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .testTag("puzzle_item_${puzzle.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (isDone) themeColors.statusSuccess else themeColors.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = puzzle.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = "${puzzle.category.displayName} • ${puzzle.difficulty.label} (${puzzle.ratingEstimate})",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.textSecondary
                            )
                        }
                    }
                    Text(
                        text = if (isDone) "Selesai" else "Main",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isDone) themeColors.statusSuccess else themeColors.accentPrimary
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ActivePuzzleBoardCard(
    puzzle: ChessPuzzle,
    isDaily: Boolean,
    onSolved: (Int) -> Unit,
    onMistake: () -> Unit,
    onNextPuzzle: () -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    val scope = rememberCoroutineScope()

    var boardState by remember(puzzle.id) { mutableStateOf(FenPgnCodec.parseFen(puzzle.initialFen)) }
    val solverColor = remember(puzzle.id) { boardState.turn }
    var currentSolutionIndex by remember(puzzle.id) { mutableIntStateOf(0) }
    var attempts by remember(puzzle.id) { mutableIntStateOf(1) }
    var selectedSquare by remember(puzzle.id) { mutableStateOf<Square?>(null) }
    var legalMovesForSelected by remember(puzzle.id) { mutableStateOf<List<ChessMove>>(emptyList()) }
    var hintMove by remember(puzzle.id) { mutableStateOf<ChessMove?>(null) }
    var isSolved by remember(puzzle.id) { mutableStateOf(false) }
    var statusMessage by remember(puzzle.id) {
        mutableStateOf("${solverColor.displayName} melangkah — ${puzzle.objectivePrompt}")
    }

    fun handleSquareClick(sq: Square) {
        if (isSolved || boardState.turn != solverColor) return

        val curSel = selectedSquare
        if (curSel != null) {
            val chosen = legalMovesForSelected.firstOrNull { it.to == sq }
            if (chosen != null) {
                val expectedUci = puzzle.solutionMovesUci.getOrNull(currentSolutionIndex)?.lowercase()?.trim()
                val actualUci = chosen.toUciString()

                if (expectedUci != null && (actualUci == expectedUci || actualUci.startsWith(expectedUci))) {
                    val afterPlayerMove = ChessEngine.makeMove(boardState, chosen, computeSan = true)
                    boardState = afterPlayerMove
                    selectedSquare = null
                    legalMovesForSelected = emptyList()
                    hintMove = null
                    currentSolutionIndex += 1

                    if (currentSolutionIndex >= puzzle.solutionMovesUci.size) {
                        isSolved = true
                        statusMessage = "Teka-Teki Terpecahkan! (${chosen.san}) — Kerja bagus!"
                        ChessSoundManager.playPuzzleSolvedSound()
                        onSolved(attempts)
                    } else {
                        statusMessage = "Langkah bagus (${chosen.san})! Lawan membalas..."
                        ChessSoundManager.playMoveSound()
                        val replyUci = puzzle.solutionMovesUci[currentSolutionIndex]
                        scope.launch {
                            delay(350L)
                            val replyMove = FenPgnCodec.parseUciMove(boardState, replyUci)
                            if (replyMove != null) {
                                boardState = ChessEngine.makeMove(boardState, replyMove, computeSan = true)
                                currentSolutionIndex += 1
                                statusMessage = "Lawan melangkah ${replyMove.san}. Temukan langkah lanjutan!"
                            }
                        }
                    }
                } else {
                    attempts += 1
                    selectedSquare = null
                    legalMovesForSelected = emptyList()
                    statusMessage = "${chosen.san} kurang tepat. Coba lagi! Petunjuk: ${puzzle.hintText}"
                    ChessSoundManager.playIllegalOrBlunderSound()
                    onMistake()
                }
                return
            }
        }

        val p = boardState.pieceAt(sq)
        if (p != null && p.color == boardState.turn) {
            selectedSquare = sq
            legalMovesForSelected = ChessEngine.getLegalMovesForSquare(boardState, sq)
        } else {
            selectedSquare = null
            legalMovesForSelected = emptyList()
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDaily) "TEKA-TEKI HARIAN • ${puzzle.category.displayName.uppercase()}" else puzzle.category.displayName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = themeColors.accentPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = puzzle.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = themeColors.textPrimary
                    )
                }
                Text(
                    text = "${puzzle.difficulty.label} • ${puzzle.ratingEstimate}",
                    style = MaterialTheme.typography.labelLarge,
                    color = themeColors.accentSecondary
                )
            }

            InteractiveChessboard(
                boardState = boardState,
                selectedSquare = selectedSquare,
                legalMovesForSelected = legalMovesForSelected,
                flipped = solverColor == PieceColor.BLACK,
                showLegalIndicators = true,
                beginnerModeEnabled = true,
                hintMove = hintMove,
                onSquareClicked = { handleSquareClick(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSolved) themeColors.statusSuccess else themeColors.textPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        boardState = FenPgnCodec.parseFen(puzzle.initialFen)
                        currentSolutionIndex = 0
                        selectedSquare = null
                        legalMovesForSelected = emptyList()
                        hintMove = null
                        isSolved = false
                        statusMessage = "${solverColor.displayName} melangkah — ${puzzle.objectivePrompt}"
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ulang", color = themeColors.textPrimary)
                }

                if (!isSolved) {
                    OutlinedButton(
                        onClick = {
                            val nextUci = puzzle.solutionMovesUci.getOrNull(currentSolutionIndex)
                            if (nextUci != null) {
                                hintMove = FenPgnCodec.parseUciMove(boardState, nextUci)
                                statusMessage = "Petunjuk: ${puzzle.hintText}"
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("puzzle_hint_button")
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = themeColors.accentPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Petunjuk", color = themeColors.accentPrimary)
                    }

                    OutlinedButton(
                        onClick = {
                            var s = FenPgnCodec.parseFen(puzzle.initialFen)
                            for (u in puzzle.solutionMovesUci) {
                                val m = FenPgnCodec.parseUciMove(s, u) ?: break
                                s = ChessEngine.makeMove(s, m, computeSan = true)
                            }
                            boardState = s
                            isSolved = true
                            statusMessage = "Solusi: ${s.moveHistory.joinToString(" ") { it.san }}"
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("puzzle_solution_button")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Solusi", color = themeColors.textPrimary)
                    }
                } else {
                    Button(
                        onClick = onNextPuzzle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.accentPrimary,
                            contentColor = themeColors.backgroundPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(2f)
                            .testTag("next_puzzle_button")
                    ) {
                        Text("Teka-Teki Berikutnya", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
