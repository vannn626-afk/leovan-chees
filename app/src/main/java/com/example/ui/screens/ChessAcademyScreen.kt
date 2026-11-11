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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.content.AcademyChapter
import com.example.data.content.AcademyLesson
import com.example.data.content.ChessAcademyCatalog
import com.example.data.local.LessonProgressEntity
import com.example.data.sound.ChessSoundManager
import com.example.engine.ChessEngine
import com.example.engine.ChessMove
import com.example.engine.FenPgnCodec
import com.example.engine.Square
import com.example.ui.components.InteractiveChessboard
import com.example.ui.theme.LocalChessThemeColors

@Composable
fun ChessAcademyScreen(
    lessonProgress: List<LessonProgressEntity>,
    onBack: () -> Unit,
    onCompleteLesson: (AcademyLesson) -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    val completedSet = remember(lessonProgress) {
        lessonProgress.filter { it.completed }.map { it.lessonId }.toSet()
    }

    var activeLesson by remember { mutableStateOf<AcademyLesson?>(null) }

    if (activeLesson != null) {
        InteractiveLessonExerciseScreen(
            lesson = activeLesson!!,
            isAlreadyCompleted = completedSet.contains(activeLesson!!.id),
            onBackToChapters = { activeLesson = null },
            onLessonCompleted = { lesson ->
                onCompleteLesson(lesson)
            },
            onNextLesson = { current ->
                val allLessons = ChessAcademyCatalog.chapters.flatMap { it.lessons }
                val idx = allLessons.indexOfFirst { it.id == current.id }
                activeLesson = if (idx in 0 until allLessons.lastIndex) {
                    allLessons[idx + 1]
                } else null
            }
        )
        return
    }

    val totalLessons = ChessAcademyCatalog.totalLessonCount.coerceAtLeast(1)
    val completedCount = completedSet.size

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
                        modifier = Modifier.testTag("academy_back_button")
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
                            text = "Akademi Catur",
                            style = MaterialTheme.typography.headlineMedium,
                            color = themeColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kurikulum Interaktif • $completedCount dari $totalLessons Materi Selesai",
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
                            text = "Progres Belajar Anda",
                            style = MaterialTheme.typography.labelLarge,
                            color = themeColors.accentPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (completedCount.toFloat() / totalLessons).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = themeColors.accentPrimary,
                            trackColor = themeColors.backgroundSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pelajari aturan dasar, pembukaan, taktik, hingga strategi akhir permainan langsung di atas papan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }

            items(ChessAcademyCatalog.chapters, key = { it.id }) { chapter ->
                ChapterCard(
                    chapter = chapter,
                    completedIds = completedSet,
                    onSelectLesson = { activeLesson = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChapterCard(
    chapter: AcademyChapter,
    completedIds: Set<String>,
    onSelectLesson: (AcademyLesson) -> Unit
) {
    val themeColors = LocalChessThemeColors.current
    val chapterDoneCount = chapter.lessons.count { completedIds.contains(it.id) }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BAB ${chapter.number}",
                        style = MaterialTheme.typography.labelMedium,
                        color = themeColors.accentPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = chapter.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = themeColors.textSecondary
                    )
                }
                Text(
                    text = "$chapterDoneCount/${chapter.lessons.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (chapterDoneCount == chapter.lessons.size) themeColors.statusSuccess else themeColors.accentPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                chapter.lessons.forEach { lesson ->
                    val isDone = completedIds.contains(lesson.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(themeColors.surfaceElevated)
                            .border(
                                1.dp,
                                if (isDone) themeColors.statusSuccess.copy(alpha = 0.6f) else themeColors.surfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectLesson(lesson) }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("lesson_${lesson.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.School,
                                contentDescription = null,
                                tint = if (isDone) themeColors.statusSuccess else themeColors.accentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = lesson.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = themeColors.textPrimary
                                )
                                Text(
                                    text = lesson.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeColors.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDone) "Ulangi" else "Mulai",
                            style = MaterialTheme.typography.labelMedium,
                            color = themeColors.accentPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveLessonExerciseScreen(
    lesson: AcademyLesson,
    isAlreadyCompleted: Boolean,
    onBackToChapters: () -> Unit,
    onLessonCompleted: (AcademyLesson) -> Unit,
    onNextLesson: (AcademyLesson) -> Unit
) {
    val themeColors = LocalChessThemeColors.current

    var boardState by remember(lesson.id) {
        mutableStateOf(FenPgnCodec.parseFen(lesson.initialFen))
    }
    var selectedSquare by remember(lesson.id) { mutableStateOf<Square?>(null) }
    var legalMovesForSelected by remember(lesson.id) { mutableStateOf<List<ChessMove>>(emptyList()) }
    var solved by remember(lesson.id) { mutableStateOf(false) }
    var feedbackText by remember(lesson.id) { mutableStateOf(lesson.exercisePrompt) }
    var showHintMove by remember(lesson.id) { mutableStateOf<ChessMove?>(null) }

    fun handleSquareSelection(sq: Square) {
        if (solved) return
        val curSel = selectedSquare
        if (curSel != null) {
            val chosen = legalMovesForSelected.firstOrNull { it.to == sq }
            if (chosen != null) {
                val uci = chosen.toUciString()
                val expected = lesson.exerciseSolutionUci.lowercase().trim()
                if (uci == expected || uci.startsWith(expected)) {
                    boardState = ChessEngine.makeMove(boardState, chosen, computeSan = true)
                    selectedSquare = null
                    legalMovesForSelected = emptyList()
                    solved = true
                    feedbackText = "Tepat sekali! (${chosen.san}) — ${lesson.keyTakeaway}"
                    ChessSoundManager.playPuzzleSolvedSound()
                    onLessonCompleted(lesson)
                } else {
                    selectedSquare = null
                    legalMovesForSelected = emptyList()
                    feedbackText = "Belum tepat (${chosen.san}). Coba lagi: ${lesson.exerciseHint}"
                    ChessSoundManager.playIllegalOrBlunderSound()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBackToChapters) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Daftar Bab",
                            tint = themeColors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = themeColors.textPrimary
                        )
                        Text(
                            text = if (solved || isAlreadyCompleted) "Materi Selesai" else "Latihan Interaktif",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (solved || isAlreadyCompleted) themeColors.statusSuccess else themeColors.accentPrimary
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, themeColors.surfaceBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = lesson.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.textPrimary
                        )
                    }
                }
            }

            item {
                InteractiveChessboard(
                    boardState = boardState,
                    selectedSquare = selectedSquare,
                    legalMovesForSelected = legalMovesForSelected,
                    flipped = false,
                    showLegalIndicators = true,
                    beginnerModeEnabled = true,
                    hintMove = showHintMove,
                    onSquareClicked = { handleSquareSelection(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (solved) {
                            themeColors.statusSuccess.copy(alpha = 0.16f)
                        } else {
                            themeColors.surfaceElevated
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.5.dp,
                            if (solved) themeColors.statusSuccess else themeColors.accentPrimary,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (solved) "LATIHAN SELESAI" else "TUGAS PRAKTIK",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (solved) themeColors.statusSuccess else themeColors.accentPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = feedbackText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    boardState = FenPgnCodec.parseFen(lesson.initialFen)
                                    selectedSquare = null
                                    legalMovesForSelected = emptyList()
                                    solved = false
                                    showHintMove = null
                                    feedbackText = lesson.exercisePrompt
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ulangi", color = themeColors.textPrimary)
                            }

                            if (!solved) {
                                OutlinedButton(
                                    onClick = {
                                        val solMove = FenPgnCodec.parseUciMove(boardState, lesson.exerciseSolutionUci)
                                        showHintMove = solMove
                                        feedbackText = "Petunjuk: ${lesson.exerciseHint}"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = themeColors.accentPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Petunjuk", color = themeColors.accentPrimary)
                                }
                            } else {
                                Button(
                                    onClick = { onNextLesson(lesson) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = themeColors.accentPrimary,
                                        contentColor = themeColors.backgroundPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Materi Berikutnya", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
