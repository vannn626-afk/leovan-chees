package com.example.engine

object ZobristHasher {
    // [square 0..63][color 0..1][pieceType 0..5]
    private val pieceKeys = Array(64) { Array(2) { LongArray(6) } }
    private var blackToMoveKey: Long = 0L
    private val castlingKeys = LongArray(16)
    private val enPassantFileKeys = LongArray(8)

    init {
        var seed = 0x517CC1B727220A95L
        fun nextRand(): Long {
            seed = seed xor (seed shl 13)
            seed = seed xor (seed ushr 7)
            seed = seed xor (seed shl 17)
            return seed
        }

        for (sq in 0 until 64) {
            for (c in 0..1) {
                for (pt in 0..5) {
                    pieceKeys[sq][c][pt] = nextRand()
                }
            }
        }
        blackToMoveKey = nextRand()
        for (i in 0 until 16) {
            castlingKeys[i] = nextRand()
        }
        for (f in 0 until 8) {
            enPassantFileKeys[f] = nextRand()
        }
    }

    fun computeHash(
        squares: List<ChessPiece?>,
        turn: PieceColor,
        castlingRights: CastlingRights,
        enPassantTarget: Square?
    ): Long {
        var hash = 0L
        for (sq in 0 until 64) {
            val piece = squares[sq] ?: continue
            val cIdx = if (piece.color == PieceColor.WHITE) 0 else 1
            val ptIdx = piece.type.ordinal
            hash = hash xor pieceKeys[sq][cIdx][ptIdx]
        }
        if (turn == PieceColor.BLACK) {
            hash = hash xor blackToMoveKey
        }
        hash = hash xor castlingKeys[castlingRights.toBitmask()]
        if (enPassantTarget != null) {
            hash = hash xor enPassantFileKeys[enPassantTarget.file]
        }
        return hash
    }
}
