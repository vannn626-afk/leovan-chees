package com.example.engine

object FenPgnCodec {

    fun parseFen(fen: String): BoardState {
        val parts = fen.trim().split(Regex("\\s+"))
        require(parts.size >= 4) { "Invalid FEN string: $fen" }

        val boardPart = parts[0]
        val turnPart = parts[1]
        val castlingPart = parts[2]
        val epPart = parts[3]
        val halfMove = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullMove = parts.getOrNull(5)?.toIntOrNull() ?: 1

        val squares = MutableList<ChessPiece?>(64) { null }
        val ranks = boardPart.split("/")
        require(ranks.size == 8) { "FEN must contain 8 ranks: $fen" }

        for (fenRankIdx in 0 until 8) {
            val boardRank = 7 - fenRankIdx
            var file = 0
            for (ch in ranks[fenRankIdx]) {
                if (ch.isDigit()) {
                    file += ch.digitToInt()
                } else {
                    val piece = ChessPiece.fromFenChar(ch)
                        ?: throw IllegalArgumentException("Invalid FEN piece char: $ch")
                    if (file in 0..7) {
                        squares[boardRank * 8 + file] = piece
                    }
                    file++
                }
            }
        }

        val turn = if (turnPart.lowercase() == "b") PieceColor.BLACK else PieceColor.WHITE
        val castlingRights = CastlingRights(
            whiteKingside = castlingPart.contains('K'),
            whiteQueenside = castlingPart.contains('Q'),
            blackKingside = castlingPart.contains('k'),
            blackQueenside = castlingPart.contains('q')
        )
        val epSquare = if (epPart == "-") null else Square.fromAlgebraic(epPart)
        val hash = ZobristHasher.computeHash(squares, turn, castlingRights, epSquare)
        val inCheck = ChessEngine.isKingInCheck(squares, turn)

        val initialState = BoardState(
            squares = squares,
            turn = turn,
            castlingRights = castlingRights,
            enPassantTarget = epSquare,
            halfMoveClock = halfMove,
            fullMoveNumber = fullMove,
            zobristHash = hash,
            positionHistoryHashes = listOf(hash),
            isInCheck = inCheck
        )

        val legalMoves = ChessEngine.generateLegalMoves(initialState)
        var termination = GameTermination.NONE
        var winner: PieceColor? = null
        if (legalMoves.isEmpty()) {
            if (inCheck) {
                termination = GameTermination.CHECKMATE
                winner = turn.opposite
            } else {
                termination = GameTermination.STALEMATE
            }
        } else if (ChessEngine.isInsufficientMaterial(squares)) {
            termination = GameTermination.INSUFFICIENT_MATERIAL
        }

        return initialState.copy(
            termination = termination,
            winner = winner
        )
    }

    fun toFen(state: BoardState): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val piece = state.squares[rank * 8 + file]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(piece.toFenChar())
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (rank > 0) sb.append('/')
        }

        val turnStr = if (state.turn == PieceColor.WHITE) "w" else "b"
        val castlingStr = state.castlingRights.toFenString()
        val epStr = state.enPassantTarget?.algebraic ?: "-"
        return "$sb $turnStr $castlingStr $epStr ${state.halfMoveClock} ${state.fullMoveNumber}"
    }

    fun formatSan(
        stateBefore: BoardState,
        move: ChessMove,
        allLegalMoves: List<ChessMove>,
        givesCheck: Boolean,
        givesCheckmate: Boolean
    ): String {
        val suffix = when {
            givesCheckmate -> "#"
            givesCheck -> "+"
            else -> ""
        }
        if (move.flag == MoveFlag.KINGSIDE_CASTLE) return "O-O$suffix"
        if (move.flag == MoveFlag.QUEENSIDE_CASTLE) return "O-O-O$suffix"

        val sb = StringBuilder()
        val pieceType = move.movedPiece.type
        val isCapture = move.capturedPiece != null || move.flag == MoveFlag.EN_PASSANT

        if (pieceType == PieceType.PAWN) {
            if (isCapture) {
                sb.append('a' + move.from.file)
                sb.append('x')
            }
            sb.append(move.to.algebraic)
            if (move.flag == MoveFlag.PROMOTION && move.promotionPiece != null) {
                sb.append('=').append(move.promotionPiece.sanSymbol)
            }
        } else {
            sb.append(pieceType.sanSymbol)
            // Disambiguate if another piece of the same type can also legally move to move.to
            val ambiguous = allLegalMoves.filter {
                it.from != move.from &&
                    it.to == move.to &&
                    it.movedPiece.type == pieceType
            }
            if (ambiguous.isNotEmpty()) {
                val sameFile = ambiguous.any { it.from.file == move.from.file }
                val sameRank = ambiguous.any { it.from.rank == move.from.rank }
                when {
                    !sameFile -> sb.append('a' + move.from.file)
                    !sameRank -> sb.append(move.from.rank + 1)
                    else -> sb.append(move.from.algebraic)
                }
            }
            if (isCapture) sb.append('x')
            sb.append(move.to.algebraic)
        }

        sb.append(suffix)
        return sb.toString()
    }

    fun parseUciMove(state: BoardState, uci: String): ChessMove? {
        val clean = uci.trim().lowercase()
        if (clean.length < 4) return null
        val from = Square.fromAlgebraic(clean.substring(0, 2)) ?: return null
        val to = Square.fromAlgebraic(clean.substring(2, 4)) ?: return null
        val promoChar = clean.getOrNull(4)
        val promoType = when (promoChar) {
            'q' -> PieceType.QUEEN
            'r' -> PieceType.ROOK
            'b' -> PieceType.BISHOP
            'n' -> PieceType.KNIGHT
            else -> null
        }

        return ChessEngine.generateLegalMoves(state).firstOrNull { move ->
            move.from == from && move.to == to &&
                (move.promotionPiece == null || move.promotionPiece == (promoType ?: PieceType.QUEEN))
        }
    }

    fun parseSanMove(state: BoardState, sanToken: String): ChessMove? {
        val cleaned = sanToken.trim()
            .replace("+", "")
            .replace("#", "")
            .replace("!", "")
            .replace("?", "")
            .replace("0-0-0", "O-O-O")
            .replace("0-0", "O-O")

        val legalMoves = ChessEngine.generateLegalMoves(state)
        for (move in legalMoves) {
            val candidateSan = formatSan(state, move, legalMoves, givesCheck = false, givesCheckmate = false)
            if (candidateSan == cleaned) return move
        }
        // Fallback to UCI format if user entered e2e4
        return parseUciMove(state, cleaned)
    }

    fun exportPgn(
        state: BoardState,
        whiteName: String = "White",
        blackName: String = "Black",
        eventName: String = "LEOVAN CHEESY Classic Match",
        dateStr: String = "2026.11.10"
    ): String {
        val resultTag = when {
            state.winner == PieceColor.WHITE -> "1-0"
            state.winner == PieceColor.BLACK -> "0-1"
            state.termination.isDraw -> "1/2-1/2"
            else -> "*"
        }
        val sb = StringBuilder()
        sb.appendLine("[Event \"$eventName\"]")
        sb.appendLine("[Site \"LEOVAN CHEESY Android\"]")
        sb.appendLine("[Date \"$dateStr\"]")
        sb.appendLine("[White \"$whiteName\"]")
        sb.appendLine("[Black \"$blackName\"]")
        sb.appendLine("[Result \"$resultTag\"]")
        sb.appendLine()

        val moves = state.moveHistory
        for (i in moves.indices step 2) {
            val moveNum = (i / 2) + 1
            val whiteSan = moves[i].san
            val blackSan = moves.getOrNull(i + 1)?.san
            if (blackSan != null) {
                sb.append("$moveNum. $whiteSan $blackSan ")
            } else {
                sb.append("$moveNum. $whiteSan ")
            }
        }
        sb.append(resultTag)
        return sb.toString().trim()
    }

    fun importPgn(pgnText: String): BoardState? {
        return try {
            val lines = pgnText.lines()
            var customFen: String? = null
            val moveBodyBuilder = StringBuilder()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("[")) {
                    if (trimmed.startsWith("[FEN \"")) {
                        customFen = trimmed.substringAfter("[FEN \"").substringBefore("\"]")
                    }
                } else {
                    moveBodyBuilder.append(trimmed).append(" ")
                }
            }

            var currentState = parseFen(customFen ?: ChessEngine.INITIAL_FEN)
            val bodyNoComments = moveBodyBuilder.toString()
                .replace(Regex("\\{[^}]*\\}"), " ")
                .replace(Regex("\\([^)]*\\)"), " ")

            val tokens = bodyNoComments.split(Regex("\\s+")).filter { it.isNotBlank() }
            for (rawToken in tokens) {
                if (rawToken in listOf("1-0", "0-1", "1/2-1/2", "*")) break
                val token = rawToken.replace(Regex("^\\d+\\.+"), "").trim()
                if (token.isEmpty()) continue
                val move = parseSanMove(currentState, token) ?: return null
                currentState = ChessEngine.makeMove(currentState, move, computeSan = true)
            }
            currentState
        } catch (e: Exception) {
            null
        }
    }
}
