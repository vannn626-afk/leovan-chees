package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        PlayerProfileEntity::class,
        SavedGameEntity::class,
        LessonProgressEntity::class,
        PuzzleProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LeovanDatabase : RoomDatabase() {
    abstract fun chessDao(): ChessDao

    companion object {
        @Volatile
        private var INSTANCE: LeovanDatabase? = null

        fun getInstance(context: Context): LeovanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LeovanDatabase::class.java,
                    "leovan_cheesy.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}

class LeovanRepository(private val dao: ChessDao) {
    val profileFlow: Flow<PlayerProfileEntity?> = dao.observeProfile()
    val ongoingGameFlow: Flow<SavedGameEntity?> = dao.observeOngoingGame()
    val completedGamesFlow: Flow<List<SavedGameEntity>> = dao.observeCompletedGames()
    val lessonProgressFlow: Flow<List<LessonProgressEntity>> = dao.observeLessonProgress()
    val puzzleProgressFlow: Flow<List<PuzzleProgressEntity>> = dao.observePuzzleProgress()

    suspend fun getOrCreateProfile(): PlayerProfileEntity {
        val existing = dao.getProfileOnce()
        if (existing != null) return existing
        val defaultProfile = PlayerProfileEntity()
        dao.upsertProfile(defaultProfile)
        return defaultProfile
    }

    suspend fun updateProfile(transform: (PlayerProfileEntity) -> PlayerProfileEntity) {
        val current = getOrCreateProfile()
        dao.upsertProfile(transform(current))
    }

    suspend fun saveOngoingGame(game: SavedGameEntity) {
        dao.clearOngoingGames()
        dao.insertOrUpdateGame(game.copy(isOngoing = true))
    }

    suspend fun clearOngoingGame() {
        dao.clearOngoingGames()
    }

    suspend fun recordCompletedGame(game: SavedGameEntity) {
        dao.clearOngoingGames()
        dao.insertOrUpdateGame(game.copy(id = 0L, isOngoing = false))
    }

    suspend fun markLessonCompleted(lessonId: String, chapterId: Int) {
        dao.upsertLessonProgress(
            LessonProgressEntity(
                lessonId = lessonId,
                chapterId = chapterId,
                isCompleted = true
            )
        )
    }

    suspend fun markPuzzleSolved(puzzleId: String, category: String, difficulty: String, attempts: Int) {
        dao.upsertPuzzleProgress(
            PuzzleProgressEntity(
                puzzleId = puzzleId,
                category = category,
                difficulty = difficulty,
                isSolved = true,
                attempts = attempts
            )
        )
    }

    suspend fun resetAllLocalData() {
        dao.clearAllGames()
        dao.clearLessonProgress()
        dao.clearPuzzleProgress()
        dao.upsertProfile(PlayerProfileEntity())
    }
}
