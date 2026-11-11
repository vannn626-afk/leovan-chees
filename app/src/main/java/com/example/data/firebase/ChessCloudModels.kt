package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val playerName: String = "Grandmaster",
    val preferredTheme: String = "CLASSIC_WOOD",
    val preferredPieceColor: String = "WHITE",
    val totalGamesPlayed: Long = 0L,
    val totalWins: Long = 0L,
    val totalLosses: Long = 0L,
    val totalDraws: Long = 0L,
    val puzzlesSolvedCount: Long = 0L,
    val currentPuzzleStreak: Long = 0L,
    val bestPuzzleStreak: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudMatchRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameMode: String = "VS_COMPUTER",
    val whiteName: String = "White",
    val blackName: String = "Black",
    val playerColor: String = "WHITE",
    val resultBanner: String = "",
    val outcomeForPlayer: String = "DRAW",
    val pgnText: String = "",
    val finalFen: String = "",
    val totalMoves: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChessRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val guestUid: String = "",
    val guestName: String = "",
    val status: String = "waiting",
    val fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    val turn: String = "WHITE",
    val plyCount: Long = 0L,
    val lastMoveUci: String = "",
    val moveHistoryUci: String = "",
    val resultBanner: String = "",
    val winnerUid: String = "",
    val drawOfferBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
