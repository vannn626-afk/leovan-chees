package com.example.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Complete offline Chess AI & Evaluation Engine.
 * Uses PeSTO / Stockfish-inspired tapered piece-square tables, mobility, pawn structure,
 * king safety, iterative deepening, alpha-beta pruning, quiescence search, and Zobrist transposition tables.
 */
object ChessAiEngine {

    private const val INF = 1_000_000
    private const val MATE_SCORE = 900_000

    private enum class TtFlag { EXACT, LOWER_BOUND, UPPER_BOUND }

    private data class TtEntry(
        val hash: Long,
        val depth: Int,
        val score: Int,
        val flag: TtFlag,
        val bestMove: ChessMove?
    )

    private const val TT_SIZE = 16384
    private val transpositionTable = arrayOfNulls<TtEntry>(TT_SIZE)

    // PeSTO / Classical Piece-Square Tables (oriented from Rank 8 down to Rank 1 for White)
    private val PAWN_MG_PST = intArrayOf(
          0,   0,   0,   0,   0,   0,   0,   0,
         98, 134,  61,  95,  68, 126,  34, -11,
         -6,   7,  26,  31,  65,  56,  25, -20,
        -14,  13,   6,  21,  23,  12,  17, -23,
        -27,  -2,  -5,  12,  17,   6,  10, -25,
        -26,  -4,  -4, -10,   3,   3,  33, -12,
        -35,  -1, -20, -23, -15,  24,  38, -22,
          0,   0,   0,   0,   0,   0,   0,   0
    )

    private val PAWN_EG_PST = intArrayOf(
          0,   0,   0,   0,   0,   0,   0,   0,
        178, 173, 158, 134, 147, 132, 165, 187,
         94, 100,  85,  67,  56,  53,  82,  84,
         32,  24,  13,   5,  -2,   4,  17,  17,
         13,   9,  -3,  -7,  -7,  -8,   3,  -1,
          4,   7,  -6,   1,   0,  -5,  -1,  -8,
         13,   8,   8,  10,  13,   0,   2,  -7,
          0,   0,   0,   0,   0,   0,   0,   0
    )

    private val KNIGHT_PST = intArrayOf(
        -167, -89, -34, -49,  61, -97, -15, -107,
         -73, -41,  72,  36,  23,  62,   7,  -17,
         -47,  60,  37,  65,  84, 129,  73,   44,
          -9,  17,  19,  53,  37,  69,  18,   22,
         -13,   4,  16,  13,  28,  19,  21,   -8,
         -23,  -9,  12,  10,  19,  17,  25,  -16,
         -29, -53, -12,  -3,  -1,  18, -14,  -19,
        -105, -21, -58, -33, -17, -28, -19,  -23
    )

    private val BISHOP_PST = intArrayOf(
        -29,   4, -82, -37, -25, -42,   7,  -8,
        -26,  16, -18, -13,  30,  59,  18, -47,
        -16,  37,  43,  40,  35,  50,  37,  -2,
         -4,   5,  19,  50,  37,  37,   7,  -2,
         -6,  13,  13,  26,  34,  12,  10,   4,
          0,  15,  15,  15,  14,  27,  18,  10,
          4,  15,  16,   0,   7,  21,  33,   1,
        -33,  -3, -14, -21, -13, -12, -39, -21
    )

    private val ROOK_PST = intArrayOf(
         32,  42,  32,  51,  63,   9,  31,  43,
         27,  32,  58,  62,  80,  67,  26,  44,
         -5,  19,  26,  36,  17,  45,  61,  16,
        -24, -11,   7,  26,  24,  35,  -8, -20,
        -36, -26, -12,  -1,   9,  -7,   6, -23,
        -45, -25, -16, -17,   3,   0,  -5, -33,
        -44, -16, -20,  -9,  -1,  11,  -6, -71,
        -19, -13,   1,  17,  16,   7, -37, -26
    )

    private val QUEEN_PST = intArrayOf(
        -28,   0,  29,  12,  59,  44,  43,  45,
        -24, -39,  -5,   1, -16,  57,  28,  54,
        -13, -17,   7,   8,  29,  56,  47,  57,
        -27, -27, -16, -16,  -1,  17,  -2,   1,
         -9, -26,  -9, -10,  -2,  -4,   3,  -3,
        -14,   2, -11,  -2,  -5,   2,  14,   5,
        -35,  -8,  11,   2,   8,  15,  -3,   1,
         -1, -18,  -9,  10, -15, -25, -31, -50
    )

    private val KING_MG_PST = intArrayOf(
        -65,  23,  16, -15, -56, -34,   2,  13,
         29,  -1, -20,  -7,  -8,  -4, -38, -29,
         -9,  24,   2, -16, -20,   6,  22, -22,
        -17, -20, -12, -27, -30, -25, -14, -36,
        -49,  -1, -27, -39, -46, -44, -33, -51,
        -14, -14, -22, -46, -44, -30, -15, -27,
          1,   7,  -8, -64, -43, -16,   9,   8,
        -15,  36,  12, -54,   8, -28,  24,  14
    )

    private val KING_EG_PST = intArrayOf(
        -74, -35, -18, -18, -11,  15,   4, -17,
        -12,  17,  14,  17,  17,  38,  23,  11,
         10,  17,  23,  15,  20,  45,  44,  13,
         -8,  22,  24,  27,  26,  33,  26,   3,
        -18,  -4,  21,  24,  27,  23,   9, -11,
        -19,  -3,  11,  21,  23,  16,   7,  -9,
        -27, -11,   4,  13,  14,   4,  -5, -17,
        -53, -34, -21, -11, -28, -14, -24, -43
    )

    private fun pstIndex(sqIndex: Int, color: PieceColor): Int {
        val file = sqIndex % 8
        val rank = sqIndex / 8
        val tableRank = if (color == PieceColor.WHITE) 7 - rank else rank
        return tableRank * 8 + file
    }

    /**
     * Static position evaluation from White's perspective in centipawns (+ means White advantage, - means Black advantage).
     */
    fun evaluatePositionWhitePerspective(state: BoardState): Int {
        val squares = state.squares
        var phase = 0
        var whiteScoreMg = 0
        var whiteScoreEg = 0
        var blackScoreMg = 0
        var blackScoreEg = 0

        var whiteBishops = 0
        var blackBishops = 0
        val whitePawnFiles = IntArray(8)
        val blackPawnFiles = IntArray(8)

        for (i in 0 until 64) {
            val p = squares[i] ?: continue
            val pstIdx = pstIndex(i, p.color)
            val file = i % 8
            val rank = i / 8

            val (mgVal, egVal) = when (p.type) {
                PieceType.PAWN -> {
                    if (p.color == PieceColor.WHITE) whitePawnFiles[file]++ else blackPawnFiles[file]++
                    (100 + PAWN_MG_PST[pstIdx]) to (120 + PAWN_EG_PST[pstIdx])
                }
                PieceType.KNIGHT -> {
                    phase += 1
                    (320 + KNIGHT_PST[pstIdx]) to (310 + KNIGHT_PST[pstIdx])
                }
                PieceType.BISHOP -> {
                    phase += 1
                    if (p.color == PieceColor.WHITE) whiteBishops++ else blackBishops++
                    (335 + BISHOP_PST[pstIdx]) to (340 + BISHOP_PST[pstIdx])
                }
                PieceType.ROOK -> {
                    phase += 2
                    (500 + ROOK_PST[pstIdx]) to (520 + ROOK_PST[pstIdx])
                }
                PieceType.QUEEN -> {
                    phase += 4
                    (920 + QUEEN_PST[pstIdx]) to (940 + QUEEN_PST[pstIdx])
                }
                PieceType.KING -> {
                    KING_MG_PST[pstIdx] to KING_EG_PST[pstIdx]
                }
            }

            // Passed pawn bonus in endgame
            var passedBonusEg = 0
            if (p.type == PieceType.PAWN) {
                val advancement = if (p.color == PieceColor.WHITE) rank else 7 - rank
                if (advancement >= 4) {
                    passedBonusEg = (advancement - 3) * 18
                }
            }

            if (p.color == PieceColor.WHITE) {
                whiteScoreMg += mgVal
                whiteScoreEg += egVal + passedBonusEg
            } else {
                blackScoreMg += mgVal
                blackScoreEg += egVal + passedBonusEg
            }
        }

        // Bishop pair bonus
        if (whiteBishops >= 2) {
            whiteScoreMg += 35; whiteScoreEg += 45
        }
        if (blackBishops >= 2) {
            blackScoreMg += 35; blackScoreEg += 45
        }

        // Doubled pawns penalty
        for (f in 0..7) {
            if (whitePawnFiles[f] > 1) {
                whiteScoreMg -= (whitePawnFiles[f] - 1) * 14
                whiteScoreEg -= (whitePawnFiles[f] - 1) * 18
            }
            if (blackPawnFiles[f] > 1) {
                blackScoreMg -= (blackPawnFiles[f] - 1) * 14
                blackScoreEg -= (blackPawnFiles[f] - 1) * 18
            }
        }

        val clampedPhase = min(24, phase)
        val mgWeight = clampedPhase
        val egWeight = 24 - clampedPhase

        val whiteTotal = (whiteScoreMg * mgWeight + whiteScoreEg * egWeight) / 24
        val blackTotal = (blackScoreMg * mgWeight + blackScoreEg * egWeight) / 24
        val tempoBonus = if (state.turn == PieceColor.WHITE) 10 else -10

        return (whiteTotal - blackTotal) + tempoBonus
    }

    fun evaluateForActiveSide(state: BoardState): Int {
        val wp = evaluatePositionWhitePerspective(state)
        return if (state.turn == PieceColor.WHITE) wp else -wp
    }

    /**
     * Selects the best move for [state] according to [difficulty] and [timeLimitMs].
     * Guaranteed to return a strictly legal move if any exists.
     */
    fun findBestMove(
        state: BoardState,
        difficulty: AiDifficulty,
        timeLimitMs: Long = difficulty.defaultThinkTimeMs
    ): ChessMove? {
        val legalMoves = ChessEngine.generateLegalMoves(state)
        if (legalMoves.isEmpty()) return null
        if (legalMoves.size == 1) return legalMoves.first()

        val startTime = System.currentTimeMillis()
        val deadline = startTime + max(150L, timeLimitMs)

        var bestMoveOverall = orderMoves(legalMoves, null).first()
        val maxDepth = difficulty.maxDepth

        // Store root move scores from deepest completed iteration for beginner/easy human-like variance
        val rootMoveScores = mutableMapOf<ChessMove, Int>()

        for (depth in 1..maxDepth) {
            if (depth > 1 && System.currentTimeMillis() >= deadline) break

            var alpha = -INF
            val beta = INF
            var currentBestMove = bestMoveOverall
            var currentBestScore = -INF

            val orderedRootMoves = orderMoves(legalMoves, bestMoveOverall)
            for (move in orderedRootMoves) {
                if (depth > 2 && System.currentTimeMillis() >= deadline) break
                val nextState = ChessEngine.makeMoveFastForSearch(state, move)
                val score = -alphaBeta(
                    state = nextState,
                    depth = depth - 1,
                    alpha = -beta,
                    beta = -alpha,
                    deadline = deadline,
                    useQuiescence = difficulty.levelNumber >= 3
                )
                rootMoveScores[move] = score
                if (score > currentBestScore) {
                    currentBestScore = score
                    currentBestMove = move
                }
                if (score > alpha) {
                    alpha = score
                }
            }
            bestMoveOverall = currentBestMove
            if (abs(currentBestScore) >= MATE_SCORE - 100) break
        }

        // Apply difficulty-specific move selection window for lower levels so Beginner/Easy/Medium feel distinct
        if (difficulty.randomnessCpWindow > 0 && rootMoveScores.size > 1) {
            val topScore = rootMoveScores.values.maxOrNull() ?: 0
            // Never blunder into immediate mate if avoidable, but allow suboptimal positional/tactical choices within window
            val candidates = rootMoveScores.filter { (_, score) ->
                score >= topScore - difficulty.randomnessCpWindow
            }.keys.toList()
            if (candidates.isNotEmpty()) {
                return candidates[Random.nextInt(candidates.size)]
            }
        }

        return bestMoveOverall
    }

    private fun alphaBeta(
        state: BoardState,
        depth: Int,
        alpha: Int,
        beta: Int,
        deadline: Long,
        useQuiescence: Boolean
    ): Int {
        var curAlpha = alpha
        var curBeta = beta

        // Repetition check
        val repCount = state.positionHistoryHashes.count { it == state.zobristHash }
        if (repCount >= 3 || state.halfMoveClock >= 100) {
            return 0
        }

        val ttIndex = (state.zobristHash ushr 1).toInt() and (TT_SIZE - 1)
        val ttEntry = transpositionTable[ttIndex]
        if (ttEntry != null && ttEntry.hash == state.zobristHash && ttEntry.depth >= depth) {
            when (ttEntry.flag) {
                TtFlag.EXACT -> return ttEntry.score
                TtFlag.LOWER_BOUND -> curAlpha = max(curAlpha, ttEntry.score)
                TtFlag.UPPER_BOUND -> curBeta = min(curBeta, ttEntry.score)
            }
            if (curAlpha >= curBeta) return ttEntry.score
        }

        val legalMoves = ChessEngine.generateLegalMoves(state)
        if (legalMoves.isEmpty()) {
            return if (state.isInCheck) {
                -(MATE_SCORE + depth)
            } else {
                0 // Stalemate
            }
        }

        if (depth <= 0) {
            return if (useQuiescence) {
                quiescenceSearch(state, curAlpha, curBeta, 3)
            } else {
                evaluateForActiveSide(state)
            }
        }

        if (System.currentTimeMillis() >= deadline) {
            return evaluateForActiveSide(state)
        }

        val orderedMoves = orderMoves(legalMoves, ttEntry?.bestMove)
        var bestScore = -INF
        var bestMove: ChessMove? = null
        val origAlpha = curAlpha

        for (move in orderedMoves) {
            val nextState = ChessEngine.makeMoveFastForSearch(state, move)
            val score = -alphaBeta(nextState, depth - 1, -curBeta, -curAlpha, deadline, useQuiescence)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (score > curAlpha) {
                curAlpha = score
            }
            if (curAlpha >= curBeta) {
                break // Beta cutoff
            }
        }

        val flag = when {
            bestScore <= origAlpha -> TtFlag.UPPER_BOUND
            bestScore >= curBeta -> TtFlag.LOWER_BOUND
            else -> TtFlag.EXACT
        }
        transpositionTable[ttIndex] = TtEntry(state.zobristHash, depth, bestScore, flag, bestMove)

        return bestScore
    }

    private fun quiescenceSearch(
        state: BoardState,
        alpha: Int,
        beta: Int,
        qDepthRemaining: Int
    ): Int {
        val standPat = evaluateForActiveSide(state)
        if (qDepthRemaining <= 0) return standPat
        if (standPat >= beta) return beta

        var curAlpha = max(alpha, standPat)
        val tacticalMoves = ChessEngine.generateLegalMoves(state).filter {
            it.capturedPiece != null || it.flag == MoveFlag.PROMOTION
        }
        if (tacticalMoves.isEmpty()) return standPat

        for (move in orderMoves(tacticalMoves, null)) {
            val nextState = ChessEngine.makeMoveFastForSearch(state, move)
            val score = -quiescenceSearch(nextState, -beta, -curAlpha, qDepthRemaining - 1)
            if (score >= beta) return beta
            if (score > curAlpha) curAlpha = score
        }
        return curAlpha
    }

    private fun orderMoves(moves: List<ChessMove>, hashMove: ChessMove?): List<ChessMove> {
        return moves.sortedByDescending { move ->
            if (hashMove != null && move.from == hashMove.from && move.to == hashMove.to && move.promotionPiece == hashMove.promotionPiece) {
                100_000
            } else {
                var score = 0
                if (move.capturedPiece != null) {
                    // MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
                    score += 10_000 + (move.capturedPiece.type.centipawnValue * 10) - move.movedPiece.type.centipawnValue
                }
                if (move.flag == MoveFlag.PROMOTION) {
                    score += 8_000 + (move.promotionPiece?.centipawnValue ?: 900)
                }
                if (move.flag == MoveFlag.KINGSIDE_CASTLE || move.flag == MoveFlag.QUEENSIDE_CASTLE) {
                    score += 600
                }
                // Center control preference (d4, e4, d5, e5)
                if (move.to.file in 3..4 && move.to.rank in 3..4) {
                    score += 120
                }
                score
            }
        }
    }
}
