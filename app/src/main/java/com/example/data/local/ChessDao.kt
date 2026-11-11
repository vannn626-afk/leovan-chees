package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChessDao {
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM saved_games WHERE isOngoing = 1 ORDER BY timestampMs DESC LIMIT 1")
    fun observeOngoingGame(): Flow<SavedGameEntity?>

    @Query("SELECT * FROM saved_games WHERE isOngoing = 0 ORDER BY timestampMs DESC LIMIT 100")
    fun observeCompletedGames(): Flow<List<SavedGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGame(game: SavedGameEntity): Long

    @Query("DELETE FROM saved_games WHERE isOngoing = 1")
    suspend fun clearOngoingGames()

    @Query("DELETE FROM saved_games")
    suspend fun clearAllGames()

    @Query("SELECT * FROM lesson_progress")
    fun observeLessonProgress(): Flow<List<LessonProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonProgress(progress: LessonProgressEntity)

    @Query("DELETE FROM lesson_progress")
    suspend fun clearLessonProgress()

    @Query("SELECT * FROM puzzle_progress")
    fun observePuzzleProgress(): Flow<List<PuzzleProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPuzzleProgress(progress: PuzzleProgressEntity)

    @Query("DELETE FROM puzzle_progress")
    suspend fun clearPuzzleProgress()
}
