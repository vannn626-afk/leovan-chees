package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val playerName: String = "Grandmaster Player",
    val avatarIndex: Int = 0,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val gamesDrawn: Int = 0,
    val puzzlesSolved: Int = 0,
    val puzzleScore: Int = 1200,
    val currentPuzzleStreak: Int = 0,
    val bestPuzzleStreak: Int = 0,
    val lastDailyPuzzleEpochDay: Long = -1L,
    val totalPlayTimeSeconds: Long = 0L,
    val selectedTheme: String = "CLASSIC_WOOD",
    val boardAutoRotateLocal: Boolean = true,
    val defaultBoardFlipped: Boolean = false,
    val showLegalMoveIndicators: Boolean = true,
    val beginnerAssistanceEnabled: Boolean = false,
    val soundEffectsEnabled: Boolean = true,
    val musicEnabled: Boolean = false,
    val animationIntensity: String = "STANDARD", // SUBTLE, STANDARD, DYNAMIC
    val defaultTimeControlId: String = "rapid_10_0",
    val allowUndoVsAi: Boolean = true,
    val onlineServerUrl: String = "wss://chess-relay.leovanstudio.org/ws",
    val completedChecklistIdsCsv: String = ""
) {
    val winRatePercent: Int
        get() = if (gamesPlayed > 0) ((gamesWon * 100.0) / gamesPlayed).toInt() else 0
}

@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val isOngoing: Boolean = false,
    val gameMode: String = "VS_COMPUTER", // VS_COMPUTER, LOCAL_1V1, ONLINE_1V1
    val whitePlayerName: String = "White",
    val blackPlayerName: String = "Black",
    val playerColor: String = "WHITE",
    val aiDifficulty: String = "MEDIUM",
    val currentFen: String = "",
    val pgnMoves: String = "",
    val uciMoveListCsv: String = "",
    val timeControlId: String = "rapid_10_0",
    val whiteTimeRemainingMs: Long = 600_000L,
    val blackTimeRemainingMs: Long = 600_000L,
    val incrementMs: Long = 0L,
    val totalMoves: Int = 0,
    val durationSeconds: Long = 0L,
    val winnerColor: String? = null,
    val terminationReason: String = "NONE",
    val beginnerModeUsed: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val chapterId: Int,
    val isCompleted: Boolean = true,
    val completedAtMs: Long = System.currentTimeMillis()
) {
    @get:androidx.room.Ignore
    val completed: Boolean
        get() = isCompleted
}

@Entity(tableName = "puzzle_progress")
data class PuzzleProgressEntity(
    @PrimaryKey val puzzleId: String,
    val category: String,
    val difficulty: String,
    val isSolved: Boolean = true,
    val attempts: Int = 1,
    val solvedAtMs: Long = System.currentTimeMillis()
) {
    @get:androidx.room.Ignore
    val solved: Boolean
        get() = isSolved
}
