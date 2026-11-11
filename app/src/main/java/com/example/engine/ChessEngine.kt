package com.example.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

object ChessEngine {
    const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    private val KNIGHT_OFFSETS = arrayOf(
        intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
        intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2)
    )

    private val KING_OFFSETS = arrayOf(
        intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 0), intArrayOf(1, -1),
        intArrayOf(0, -1), intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1)
    )

    private val BISHOP_DIRS = arrayOf(
        intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, -1), intArrayOf(-1, 1)
    )

    private val ROOK_DIRS = arrayOf(
        intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(-1, 0)
    )

    private val QUEEN_DIRS = arrayOf(
        intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 0), intArrayOf(1, -1),
        intArrayOf(0, -1), intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1)
    )

    private val PROMOTION_TYPES = listOf(
        PieceType.QUEEN,
        PieceType.ROOK,
        PieceType.BISHOP,
        PieceType.KNIGHT
    )

    fun createInitialState(): BoardState = FenPgnCodec.parseFen(INITIAL_FEN)

    /**
     * Checks whether [targetSquare] is attacked by any piece of [attackerColor] on [squares].
     */
    fun isSquareAttacked(
        squares: List<ChessPiece?>,
        targetSquare: Square,
        attackerColor: PieceColor
    ): Boolean {
        val tf = targetSquare.file
        val tr = targetSquare.rank

        // 1. Pawn attacks: an attacker pawn on (tf - 1, tr - pawnDir) or (tf + 1, tr - pawnDir) attacks (tf, tr)
        val pawnAttackRank = if (attackerColor == PieceColor.WHITE) tr - 1 else tr + 1
        if (pawnAttackRank in 0..7) {
            for (df in intArrayOf(-1, 1)) {
                val pf = tf + df
                if (pf in 0..7) {
                    val p = squares[pawnAttackRank * 8 + pf]
                    if (p != null && p.color == attackerColor && p.type == PieceType.PAWN) {
                        return true
                    }
                }
            }
        }

        // 2. Knight attacks
        for (offset in KNIGHT_OFFSETS) {
            val nf = tf + offset[0]
            val nr = tr + offset[1]
            if (nf in 0..7 && nr in 0..7) {
                val p = squares[nr * 8 + nf]
                if (p != null && p.color == attackerColor && p.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }

        // 3. King attacks
        for (offset in KING_OFFSETS) {
            val kf = tf + offset[0]
            val kr = tr + offset[1]
            if (kf in 0..7 && kr in 0..7) {
                val p = squares[kr * 8 + kf]
                if (p != null && p.color == attackerColor && p.type == PieceType.KING) {
                    return true
                }
            }
        }

        // 4. Diagonal rays (Bishop / Queen)
        for (dir in BISHOP_DIRS) {
            var f = tf + dir[0]
            var r = tr + dir[1]
            while (f in 0..7 && r in 0..7) {
                val p = squares[r * 8 + f]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                f += dir[0]
                r += dir[1]
            }
        }

        // 5. Orthogonal rays (Rook / Queen)
        for (dir in ROOK_DIRS) {
            var f = tf + dir[0]
            var r = tr + dir[1]
            while (f in 0..7 && r in 0..7) {
                val p = squares[r * 8 + f]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                f += dir[0]
                r += dir[1]
            }
        }

        return false
    }

    fun isKingInCheck(squares: List<ChessPiece?>, color: PieceColor): Boolean {
        for (i in 0 until 64) {
            val p = squares[i]
            if (p != null && p.color == color && p.type == PieceType.KING) {
                return isSquareAttacked(squares, Square.fromIndex(i), color.opposite)
            }
        }
        return false
    }

    /**
     * Generates all strictly legal moves for the active player in [state].
     */
    fun generateLegalMoves(state: BoardState): List<ChessMove> {
        val pseudoMoves = generatePseudoLegalMoves(state)
        val legalMoves = ArrayList<ChessMove>(pseudoMoves.size)
        for (move in pseudoMoves) {
            val nextSquares = applyMoveToSquaresOnly(state.squares, move)
            if (!isKingInCheck(nextSquares, state.turn)) {
                legalMoves.add(move)
            }
        }
        return legalMoves
    }

    fun getLegalMovesForSquare(state: BoardState, from: Square): List<ChessMove> {
        if (state.isGameOver) return emptyList()
        val piece = state.pieceAt(from) ?: return emptyList()
        if (piece.color != state.turn) return emptyList()
        return generateLegalMoves(state).filter { it.from == from }
    }

    private fun generatePseudoLegalMoves(state: BoardState): List<ChessMove> {
        val moves = ArrayList<ChessMove>(48)
        val color = state.turn
        val squares = state.squares

        for (idx in 0 until 64) {
            val piece = squares[idx] ?: continue
            if (piece.color != color) continue
            val from = Square.fromIndex(idx)

            when (piece.type) {
                PieceType.PAWN -> generatePawnMoves(state, from, piece, moves)
                PieceType.KNIGHT -> {
                    for (offset in KNIGHT_OFFSETS) {
                        val to = Square(from.file + offset[0], from.rank + offset[1])
                        if (!to.isValid) continue
                        val target = squares[to.index]
                        if (target == null || target.color != color) {
                            moves.add(ChessMove(from = from, to = to, movedPiece = piece, capturedPiece = target))
                        }
                    }
                }
                PieceType.BISHOP -> generateSlidingMoves(squares, from, piece, BISHOP_DIRS, moves)
                PieceType.ROOK -> generateSlidingMoves(squares, from, piece, ROOK_DIRS, moves)
                PieceType.QUEEN -> generateSlidingMoves(squares, from, piece, QUEEN_DIRS, moves)
                PieceType.KING -> generateKingMoves(state, from, piece, moves)
            }
        }
        return moves
    }

    private fun generatePawnMoves(
        state: BoardState,
        from: Square,
        piece: ChessPiece,
        moves: MutableList<ChessMove>
    ) {
        val color = piece.color
        val dir = if (color == PieceColor.WHITE) 1 else -1
        val startRank = if (color == PieceColor.WHITE) 1 else 6
        val promoRank = if (color == PieceColor.WHITE) 7 else 0
        val squares = state.squares

        // 1. Single step forward
        val oneStep = Square(from.file, from.rank + dir)
        if (oneStep.isValid && squares[oneStep.index] == null) {
            if (oneStep.rank == promoRank) {
                for (promoType in PROMOTION_TYPES) {
                    moves.add(
                        ChessMove(
                            from = from,
                            to = oneStep,
                            movedPiece = piece,
                            promotionPiece = promoType,
                            flag = MoveFlag.PROMOTION
                        )
                    )
                }
            } else {
                moves.add(ChessMove(from = from, to = oneStep, movedPiece = piece))
                // 2. Double step from initial rank
                if (from.rank == startRank) {
                    val twoStep = Square(from.file, from.rank + dir * 2)
                    if (twoStep.isValid && squares[twoStep.index] == null) {
                        moves.add(
                            ChessMove(
                                from = from,
                                to = twoStep,
                                movedPiece = piece,
                                flag = MoveFlag.DOUBLE_PAWN_PUSH
                            )
                        )
                    }
                }
            }
        }

        // 3. Diagonal captures & En Passant
        for (df in intArrayOf(-1, 1)) {
            val capSq = Square(from.file + df, from.rank + dir)
            if (!capSq.isValid) continue
            val targetPiece = squares[capSq.index]
            if (targetPiece != null && targetPiece.color != color) {
                if (capSq.rank == promoRank) {
                    for (promoType in PROMOTION_TYPES) {
                        moves.add(
                            ChessMove(
                                from = from,
                                to = capSq,
                                movedPiece = piece,
                                capturedPiece = targetPiece,
                                promotionPiece = promoType,
                                flag = MoveFlag.PROMOTION
                            )
                        )
                    }
                } else {
                    moves.add(
                        ChessMove(
                            from = from,
                            to = capSq,
                            movedPiece = piece,
                            capturedPiece = targetPiece
                        )
                    )
                }
            } else if (state.enPassantTarget != null && capSq == state.enPassantTarget) {
                val capturedPawnSq = Square(capSq.file, from.rank)
                val capturedPawn = squares[capturedPawnSq.index]
                if (capturedPawn != null && capturedPawn.color != color && capturedPawn.type == PieceType.PAWN) {
                    moves.add(
                        ChessMove(
                            from = from,
                            to = capSq,
                            movedPiece = piece,
                            capturedPiece = capturedPawn,
                            flag = MoveFlag.EN_PASSANT
                        )
                    )
                }
            }
        }
    }

    private fun generateSlidingMoves(
        squares: List<ChessPiece?>,
        from: Square,
        piece: ChessPiece,
        dirs: Array<IntArray>,
        moves: MutableList<ChessMove>
    ) {
        for (dir in dirs) {
            var f = from.file + dir[0]
            var r = from.rank + dir[1]
            while (f in 0..7 && r in 0..7) {
                val to = Square(f, r)
                val target = squares[to.index]
                if (target == null) {
                    moves.add(ChessMove(from = from, to = to, movedPiece = piece))
                } else {
                    if (target.color != piece.color) {
                        moves.add(ChessMove(from = from, to = to, movedPiece = piece, capturedPiece = target))
                    }
                    break
                }
                f += dir[0]
                r += dir[1]
            }
        }
    }

    private fun generateKingMoves(
        state: BoardState,
        from: Square,
        piece: ChessPiece,
        moves: MutableList<ChessMove>
    ) {
        val color = piece.color
        val squares = state.squares
        for (offset in KING_OFFSETS) {
            val to = Square(from.file + offset[0], from.rank + offset[1])
            if (!to.isValid) continue
            val target = squares[to.index]
            if (target == null || target.color != color) {
                moves.add(ChessMove(from = from, to = to, movedPiece = piece, capturedPiece = target))
            }
        }

        // Castling rules:
        // 1. King is on starting square (e1 for White, e8 for Black)
        // 2. King is not currently in check
        // 3. Corresponding castling right is true and Rook is present on corner square
        // 4. Squares between King and Rook are empty
        // 5. Squares the King passes through and lands on are not attacked by enemy
        val homeRank = if (color == PieceColor.WHITE) 0 else 7
        if (from.file == 4 && from.rank == homeRank && !isSquareAttacked(squares, from, color.opposite)) {
            val canKingside = if (color == PieceColor.WHITE) state.castlingRights.whiteKingside else state.castlingRights.blackKingside
            val canQueenside = if (color == PieceColor.WHITE) state.castlingRights.whiteQueenside else state.castlingRights.blackQueenside

            if (canKingside) {
                val f1 = Square(5, homeRank)
                val g1 = Square(6, homeRank)
                val h1Piece = squares[Square(7, homeRank).index]
                if (h1Piece == ChessPiece(PieceType.ROOK, color) &&
                    squares[f1.index] == null &&
                    squares[g1.index] == null &&
                    !isSquareAttacked(squares, f1, color.opposite) &&
                    !isSquareAttacked(squares, g1, color.opposite)
                ) {
                    moves.add(
                        ChessMove(
                            from = from,
                            to = g1,
                            movedPiece = piece,
                            flag = MoveFlag.KINGSIDE_CASTLE
                        )
                    )
                }
            }

            if (canQueenside) {
                val d1 = Square(3, homeRank)
                val c1 = Square(2, homeRank)
                val b1 = Square(1, homeRank)
                val a1Piece = squares[Square(0, homeRank).index]
                if (a1Piece == ChessPiece(PieceType.ROOK, color) &&
                    squares[d1.index] == null &&
                    squares[c1.index] == null &&
                    squares[b1.index] == null &&
                    !isSquareAttacked(squares, d1, color.opposite) &&
                    !isSquareAttacked(squares, c1, color.opposite)
                ) {
                    moves.add(
                        ChessMove(
                            from = from,
                            to = c1,
                            movedPiece = piece,
                            flag = MoveFlag.QUEENSIDE_CASTLE
                        )
                    )
                }
            }
        }
    }

    internal fun applyMoveToSquaresOnly(
        squares: List<ChessPiece?>,
        move: ChessMove
    ): List<ChessPiece?> {
        val next = squares.toMutableList()
        next[move.from.index] = null
        when (move.flag) {
            MoveFlag.EN_PASSANT -> {
                val capturedPawnSq = Square(move.to.file, move.from.rank)
                next[capturedPawnSq.index] = null
                next[move.to.index] = move.movedPiece
            }
            MoveFlag.KINGSIDE_CASTLE -> {
                val rank = move.from.rank
                val rook = next[Square(7, rank).index]
                next[Square(7, rank).index] = null
                next[Square(5, rank).index] = rook
                next[move.to.index] = move.movedPiece
            }
            MoveFlag.QUEENSIDE_CASTLE -> {
                val rank = move.from.rank
                val rook = next[Square(0, rank).index]
                next[Square(0, rank).index] = null
                next[Square(3, rank).index] = rook
                next[move.to.index] = move.movedPiece
            }
            MoveFlag.PROMOTION -> {
                val promotedType = move.promotionPiece ?: PieceType.QUEEN
                next[move.to.index] = ChessPiece(promotedType, move.movedPiece.color)
            }
            else -> {
                next[move.to.index] = move.movedPiece
            }
        }
        return next
    }

    /**
     * Executes a legal move on [state] and returns the complete updated [BoardState]
     * including SAN notation, FEN, Zobrist repetition checks, 50/75-move rules, and checkmate/stalemate detection.
     */
    fun makeMove(state: BoardState, move: ChessMove, computeSan: Boolean = true): BoardState {
        val legalMoves = if (computeSan) generateLegalMoves(state) else emptyList()
        val nextSquares = applyMoveToSquaresOnly(state.squares, move)
        val nextTurn = state.turn.opposite

        // Update castling rights if King or Rook moves or is captured
        var wk = state.castlingRights.whiteKingside
        var wq = state.castlingRights.whiteQueenside
        var bk = state.castlingRights.blackKingside
        var bq = state.castlingRights.blackQueenside

        if (move.movedPiece.type == PieceType.KING) {
            if (move.movedPiece.color == PieceColor.WHITE) {
                wk = false; wq = false
            } else {
                bk = false; bq = false
            }
        }
        if (move.from == Square(0, 0) || move.to == Square(0, 0)) wq = false
        if (move.from == Square(7, 0) || move.to == Square(7, 0)) wk = false
        if (move.from == Square(0, 7) || move.to == Square(0, 7)) bq = false
        if (move.from == Square(7, 7) || move.to == Square(7, 7)) bk = false

        val nextCastling = CastlingRights(wk, wq, bk, bq)

        val nextEpTarget = if (move.flag == MoveFlag.DOUBLE_PAWN_PUSH) {
            val midRank = (move.from.rank + move.to.rank) / 2
            Square(move.from.file, midRank)
        } else {
            null
        }

        val isPawnMoveOrCapture = move.movedPiece.type == PieceType.PAWN || move.capturedPiece != null
        val nextHalfMove = if (isPawnMoveOrCapture) 0 else state.halfMoveClock + 1
        val nextFullMove = if (state.turn == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber

        val nextCapWhite = if (state.turn == PieceColor.WHITE && move.capturedPiece != null) {
            state.capturedByWhite + move.capturedPiece
        } else state.capturedByWhite

        val nextCapBlack = if (state.turn == PieceColor.BLACK && move.capturedPiece != null) {
            state.capturedByBlack + move.capturedPiece
        } else state.capturedByBlack

        val nextHash = ZobristHasher.computeHash(nextSquares, nextTurn, nextCastling, nextEpTarget)
        val nextHistoryHashes = state.positionHistoryHashes + nextHash
        val nextInCheck = isKingInCheck(nextSquares, nextTurn)

        val tempState = BoardState(
            squares = nextSquares,
            turn = nextTurn,
            castlingRights = nextCastling,
            enPassantTarget = nextEpTarget,
            halfMoveClock = nextHalfMove,
            fullMoveNumber = nextFullMove,
            zobristHash = nextHash,
            positionHistoryHashes = nextHistoryHashes,
            capturedByWhite = nextCapWhite,
            capturedByBlack = nextCapBlack,
            isInCheck = nextInCheck
        )

        val opponentLegalMoves = generateLegalMoves(tempState)
        val repetitionCount = nextHistoryHashes.count { it == nextHash }

        var termination = GameTermination.NONE
        var winner: PieceColor? = null

        if (opponentLegalMoves.isEmpty()) {
            if (nextInCheck) {
                termination = GameTermination.CHECKMATE
                winner = state.turn
            } else {
                termination = GameTermination.STALEMATE
            }
        } else if (isInsufficientMaterial(nextSquares)) {
            termination = GameTermination.INSUFFICIENT_MATERIAL
        } else if (nextHalfMove >= 150) {
            // 75 full moves = 150 half-moves automatic draw
            termination = GameTermination.SEVENTY_FIVE_MOVE_RULE
        } else if (repetitionCount >= 3) {
            termination = GameTermination.THREEFOLD_REPETITION
        }

        val sanText = if (computeSan) {
            FenPgnCodec.formatSan(state, move, legalMoves, nextInCheck, opponentLegalMoves.isEmpty() && nextInCheck)
        } else move.san

        val finalFen = FenPgnCodec.toFen(tempState)
        val recordedMove = move.copy(
            san = sanText,
            resultingFen = finalFen
        )

        return tempState.copy(
            moveHistory = state.moveHistory + recordedMove,
            termination = termination,
            winner = winner,
            canClaimFiftyMoveDraw = nextHalfMove >= 100 && termination == GameTermination.NONE,
            canClaimThreefoldDraw = repetitionCount >= 3 && termination == GameTermination.NONE
        )
    }

    /**
     * Evaluates FIDE insufficient material / dead position rules:
     * - K vs K
     * - K+B vs K
     * - K+N vs K
     * - K+B vs K+B with bishops on the same square color
     */
    fun isInsufficientMaterial(squares: List<ChessPiece?>): Boolean {
        val nonKingPieces = ArrayList<Pair<Square, ChessPiece>>(8)
        for (i in 0 until 64) {
            val p = squares[i] ?: continue
            if (p.type != PieceType.KING) {
                if (p.type == PieceType.PAWN || p.type == PieceType.ROOK || p.type == PieceType.QUEEN) {
                    return false
                }
                nonKingPieces.add(Square.fromIndex(i) to p)
            }
        }
        if (nonKingPieces.isEmpty()) return true // K vs K
        if (nonKingPieces.size == 1) {
            val type = nonKingPieces[0].second.type
            if (type == PieceType.BISHOP || type == PieceType.KNIGHT) return true // K+B vs K or K+N vs K
        }
        if (nonKingPieces.size == 2) {
            val (sq1, p1) = nonKingPieces[0]
            val (sq2, p2) = nonKingPieces[1]
            if (p1.type == PieceType.BISHOP && p2.type == PieceType.BISHOP &&
                p1.color != p2.color && sq1.isLightSquare == sq2.isLightSquare
            ) {
                return true // K+B vs K+B on same color squares
            }
        }
        return false
    }

    /**
     * Returns squares of all friendly pieces currently under attack by the opponent (for Beginner Mode).
     */
    fun getThreatenedFriendlySquares(state: BoardState, friendlyColor: PieceColor = state.turn): Set<Square> {
        val threatened = mutableSetOf<Square>()
        val enemyColor = friendlyColor.opposite
        for (i in 0 until 64) {
            val p = state.squares[i] ?: continue
            if (p.color == friendlyColor) {
                val sq = Square.fromIndex(i)
                if (isSquareAttacked(state.squares, sq, enemyColor)) {
                    threatened.add(sq)
                }
            }
        }
        return threatened
    }

    /**
     * Explains in clear, beginner-friendly language why moving from [from] to [to] is illegal.
     */
    fun explainIllegalMoveAttempt(state: BoardState, from: Square, to: Square): String {
        if (from == to) return "Select a different destination square to move your piece."
        val piece = state.pieceAt(from) ?: return "There is no piece on ${from.algebraic}."
        if (piece.color != state.turn) {
            return "It is currently ${state.turn.displayName}'s turn to move."
        }
        val target = state.pieceAt(to)
        if (target != null && target.color == piece.color) {
            return "Illegal move: Square ${to.algebraic} is already occupied by your own ${target.type.displayName}."
        }

        // Check if pseudo-legal movement pattern matches first
        val pseudoMoves = generatePseudoLegalMoves(state).filter { it.from == from && it.to == to }
        if (pseudoMoves.isNotEmpty()) {
            // The move follows piece geometry, so it must expose or leave the King in check!
            return if (state.isInCheck) {
                "Illegal move: Your King is in check! You must get out of check by moving the King, blocking the attack, or capturing the attacking piece."
            } else {
                "Illegal move: Moving your ${piece.type.displayName} to ${to.algebraic} would expose your King to check (pinned piece or attacked King square)."
            }
        }

        val df = to.file - from.file
        val dr = to.rank - from.rank
        val absDf = abs(df)
        val absDr = abs(dr)

        return when (piece.type) {
            PieceType.PAWN -> {
                val forwardDir = if (piece.color == PieceColor.WHITE) 1 else -1
                if (dr * forwardDir <= 0) {
                    "Illegal move: Pawns can only move forward toward the opponent's side, never backward or sideways."
                } else if (df == 0 && target != null) {
                    "Illegal move: Pawns cannot capture straight ahead; they capture one square diagonally forward."
                } else if (absDf == 1 && dr == forwardDir && target == null) {
                    "Illegal move: Pawns can only move diagonally when capturing an enemy piece (or via legal En Passant)."
                } else if (df == 0 && absDr == 2) {
                    "Illegal move: A Pawn may only advance two squares on its very first move from its starting rank when both squares are empty."
                } else {
                    "Illegal move: ${PieceType.PAWN.beginnerRuleSummary}"
                }
            }
            PieceType.KNIGHT -> "Illegal move: Knights move in an L-shape (2 squares in one direction and 1 square perpendicular)."
            PieceType.BISHOP -> {
                if (absDf == absDr && isPathBlocked(state.squares, from, to)) {
                    "Illegal move: Bishops cannot jump over other pieces blocking the diagonal path."
                } else {
                    "Illegal move: Bishops can only move diagonally along squares of the same color."
                }
            }
            PieceType.ROOK -> {
                if ((df == 0 || dr == 0) && isPathBlocked(state.squares, from, to)) {
                    "Illegal move: Rooks cannot jump over pieces blocking their rank or file."
                } else {
                    "Illegal move: Rooks move straight horizontally along ranks or vertically along files."
                }
            }
            PieceType.QUEEN -> {
                if ((df == 0 || dr == 0 || absDf == absDr) && isPathBlocked(state.squares, from, to)) {
                    "Illegal move: The Queen cannot jump over pieces blocking her path."
                } else {
                    "Illegal move: The Queen moves straight horizontally, vertically, or diagonally—not in Knight jumps."
                }
            }
            PieceType.KING -> {
                if (absDr == 0 && absDf == 2) {
                    if (state.isInCheck) {
                        "Illegal castling: You cannot castle while your King is currently in check."
                    } else {
                        "Illegal castling: Castling requires an unmoved King and Rook, empty squares between them, and the King must not pass through or land on an attacked square."
                    }
                } else if (max(absDf, absDr) == 1) {
                    "Illegal move: The King cannot move to ${to.algebraic} because that square is attacked by an opponent piece."
                } else {
                    "Illegal move: The King moves only one square in any direction (except when legally castling)."
                }
            }
        }
    }

    private fun isPathBlocked(squares: List<ChessPiece?>, from: Square, to: Square): Boolean {
        val stepF = (to.file - from.file).sign
        val stepR = (to.rank - from.rank).sign
        var f = from.file + stepF
        var r = from.rank + stepR
        while (f != to.file || r != to.rank) {
            if (squares[r * 8 + f] != null) return true
            f += stepF
            r += stepR
        }
        return false
    }

    /**
     * Standard Perft (Performance Test) move path enumeration to validate strict FIDE move generation.
     */
    fun perft(state: BoardState, depth: Int): Long {
        if (depth <= 0) return 1L
        val legalMoves = generateLegalMoves(state)
        if (depth == 1) return legalMoves.size.toLong()
        var nodes = 0L
        for (move in legalMoves) {
            val nextState = makeMoveFastForSearch(state, move)
            nodes += perft(nextState, depth - 1)
        }
        return nodes
    }

    /**
     * Fast state transition for AI search & Perft without string formatting overhead.
     */
    fun makeMoveFastForSearch(state: BoardState, move: ChessMove): BoardState {
        val nextSquares = applyMoveToSquaresOnly(state.squares, move)
        val nextTurn = state.turn.opposite

        var wk = state.castlingRights.whiteKingside
        var wq = state.castlingRights.whiteQueenside
        var bk = state.castlingRights.blackKingside
        var bq = state.castlingRights.blackQueenside

        if (move.movedPiece.type == PieceType.KING) {
            if (move.movedPiece.color == PieceColor.WHITE) {
                wk = false; wq = false
            } else {
                bk = false; bq = false
            }
        }
        if (move.from == Square(0, 0) || move.to == Square(0, 0)) wq = false
        if (move.from == Square(7, 0) || move.to == Square(7, 0)) wk = false
        if (move.from == Square(0, 7) || move.to == Square(0, 7)) bq = false
        if (move.from == Square(7, 7) || move.to == Square(7, 7)) bk = false

        val nextCastling = CastlingRights(wk, wq, bk, bq)
        val nextEpTarget = if (move.flag == MoveFlag.DOUBLE_PAWN_PUSH) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else null

        val isPawnOrCap = move.movedPiece.type == PieceType.PAWN || move.capturedPiece != null
        val nextHalfMove = if (isPawnOrCap) 0 else state.halfMoveClock + 1
        val nextFullMove = if (state.turn == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber
        val nextHash = ZobristHasher.computeHash(nextSquares, nextTurn, nextCastling, nextEpTarget)

        return BoardState(
            squares = nextSquares,
            turn = nextTurn,
            castlingRights = nextCastling,
            enPassantTarget = nextEpTarget,
            halfMoveClock = nextHalfMove,
            fullMoveNumber = nextFullMove,
            zobristHash = nextHash,
            positionHistoryHashes = state.positionHistoryHashes + nextHash,
            isInCheck = isKingInCheck(nextSquares, nextTurn)
        )
    }
}
