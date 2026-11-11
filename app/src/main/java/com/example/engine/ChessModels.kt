package com.example.engine

enum class PieceColor(val displayName: String) {
    WHITE("Putih"),
    BLACK("Hitam");

    val opposite: PieceColor
        get() = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(
    val displayName: String,
    val sanSymbol: String,
    val materialValue: Int,
    val centipawnValue: Int,
    val beginnerRuleSummary: String
) {
    PAWN(
        displayName = "Pion",
        sanSymbol = "",
        materialValue = 1,
        centipawnValue = 100,
        beginnerRuleSummary = "Maju 1 petak (atau 2 petak dari baris awal), memakan 1 petak secara diagonal ke depan, dan promosi di baris ke-8."
    ),
    KNIGHT(
        displayName = "Kuda",
        sanSymbol = "N",
        materialValue = 3,
        centipawnValue = 320,
        beginnerRuleSummary = "Melangkah membentuk huruf L (2 petak satu arah lalu 1 petak berbelok). Satu-satunya bidak yang bisa melompati bidak lain."
    ),
    BISHOP(
        displayName = "Gajah",
        sanSymbol = "B",
        materialValue = 3,
        centipawnValue = 330,
        beginnerRuleSummary = "Melangkah secara diagonal sejauh mungkin selama jalur tidak terhalang, selalu berada di warna petak yang sama."
    ),
    ROOK(
        displayName = "Benteng",
        sanSymbol = "R",
        materialValue = 5,
        centipawnValue = 500,
        beginnerRuleSummary = "Melangkah lurus secara horizontal dan vertikal sejauh mungkin. Juga berperan dalam langkah khusus Rokade."
    ),
    QUEEN(
        displayName = "Menteri",
        sanSymbol = "Q",
        materialValue = 9,
        centipawnValue = 900,
        beginnerRuleSummary = "Melangkah lurus maupun diagonal ke segala arah—menggabungkan kekuatan Benteng dan Gajah."
    ),
    KING(
        displayName = "Raja",
        sanSymbol = "K",
        materialValue = 0,
        centipawnValue = 20000,
        beginnerRuleSummary = "Melangkah 1 petak ke segala arah selama petak tujuan aman dari ancaman lawan. Dapat melakukan Rokade sekali per permainan."
    )
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor
) {
    fun toFenChar(): Char {
        val c = when (type) {
            PieceType.PAWN -> 'p'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.ROOK -> 'r'
            PieceType.QUEEN -> 'q'
            PieceType.KING -> 'k'
        }
        return if (color == PieceColor.WHITE) c.uppercaseChar() else c
    }

    companion object {
        fun fromFenChar(c: Char): ChessPiece? {
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = when (c.lowercaseChar()) {
                'p' -> PieceType.PAWN
                'n' -> PieceType.KNIGHT
                'b' -> PieceType.BISHOP
                'r' -> PieceType.ROOK
                'q' -> PieceType.QUEEN
                'k' -> PieceType.KING
                else -> return null
            }
            return ChessPiece(type, color)
        }
    }
}

/**
 * Represents a square on the 8x8 chessboard.
 * file: 0 ('a') .. 7 ('h')
 * rank: 0 ('1', White home rank) .. 7 ('8', Black home rank)
 */
data class Square(val file: Int, val rank: Int) {
    val isValid: Boolean
        get() = file in 0..7 && rank in 0..7

    val index: Int
        get() = rank * 8 + file

    val algebraic: String
        get() = "${('a' + file)}${rank + 1}"

    val isLightSquare: Boolean
        get() = (file + rank) % 2 != 0

    companion object {
        fun fromIndex(index: Int): Square = Square(index % 8, index / 8)

        fun fromAlgebraic(str: String): Square? {
            val clean = str.trim().lowercase()
            if (clean.length != 2) return null
            val file = clean[0] - 'a'
            val rank = clean[1] - '1'
            if (file !in 0..7 || rank !in 0..7) return null
            return Square(file, rank)
        }
    }
}

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun toFenString(): String {
        val sb = StringBuilder()
        if (whiteKingside) sb.append('K')
        if (whiteQueenside) sb.append('Q')
        if (blackKingside) sb.append('k')
        if (blackQueenside) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    fun toBitmask(): Int {
        var mask = 0
        if (whiteKingside) mask = mask or 1
        if (whiteQueenside) mask = mask or 2
        if (blackKingside) mask = mask or 4
        if (blackQueenside) mask = mask or 8
        return mask
    }
}

enum class MoveFlag {
    NORMAL,
    DOUBLE_PAWN_PUSH,
    KINGSIDE_CASTLE,
    QUEENSIDE_CASTLE,
    EN_PASSANT,
    PROMOTION
}

data class ChessMove(
    val from: Square,
    val to: Square,
    val movedPiece: ChessPiece,
    val capturedPiece: ChessPiece? = null,
    val promotionPiece: PieceType? = null,
    val flag: MoveFlag = MoveFlag.NORMAL,
    val san: String = "",
    val resultingFen: String = "",
    val evalCpAfter: Int? = null
) {
    fun toUciString(): String {
        val promo = when (promotionPiece) {
            PieceType.QUEEN -> "q"
            PieceType.ROOK -> "r"
            PieceType.BISHOP -> "b"
            PieceType.KNIGHT -> "n"
            else -> ""
        }
        return "${from.algebraic}${to.algebraic}$promo"
    }
}

enum class GameTermination(val title: String, val isDraw: Boolean) {
    NONE("Sedang Berlangsung", false),
    CHECKMATE("Skakmat", false),
    STALEMATE("Remis (Pat / Stalemate)", true),
    INSUFFICIENT_MATERIAL("Remis (Materi Tidak Cukup)", true),
    THREEFOLD_REPETITION("Remis (Pengulangan 3 Kali)", true),
    FIFTY_MOVE_RULE("Remis (Aturan 50 Langkah)", true),
    SEVENTY_FIVE_MOVE_RULE("Remis Otomatis (Aturan 75 Langkah)", true),
    DEAD_POSITION("Remis (Posisi Mati)", true),
    DRAW_BY_AGREEMENT("Remis (Kesepakatan Bersama)", true),
    RESIGNATION("Menang (Lawan Menyerah)", false),
    TIMEOUT("Menang Waktu", false)
}

data class BoardState(
    val squares: List<ChessPiece?>,
    val turn: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val zobristHash: Long = 0L,
    val positionHistoryHashes: List<Long> = emptyList(),
    val moveHistory: List<ChessMove> = emptyList(),
    val capturedByWhite: List<ChessPiece> = emptyList(),
    val capturedByBlack: List<ChessPiece> = emptyList(),
    val isInCheck: Boolean = false,
    val termination: GameTermination = GameTermination.NONE,
    val winner: PieceColor? = null,
    val canClaimFiftyMoveDraw: Boolean = false,
    val canClaimThreefoldDraw: Boolean = false
) {
    fun pieceAt(square: Square): ChessPiece? {
        if (!square.isValid) return null
        return squares[square.index]
    }

    fun pieceAt(file: Int, rank: Int): ChessPiece? {
        if (file !in 0..7 || rank !in 0..7) return null
        return squares[rank * 8 + file]
    }

    fun findKingSquare(color: PieceColor): Square? {
        for (i in 0 until 64) {
            val p = squares[i]
            if (p != null && p.color == color && p.type == PieceType.KING) {
                return Square.fromIndex(i)
            }
        }
        return null
    }

    val isGameOver: Boolean
        get() = termination != GameTermination.NONE

    fun materialBalanceFor(color: PieceColor): Int {
        var myPoints = 0
        var oppPoints = 0
        for (p in squares) {
            if (p != null && p.type != PieceType.KING) {
                if (p.color == color) myPoints += p.type.materialValue
                else oppPoints += p.type.materialValue
            }
        }
        return myPoints - oppPoints
    }
}

enum class AiDifficulty(
    val levelNumber: Int,
    val displayName: String,
    val eloEstimate: Int,
    val maxDepth: Int,
    val defaultThinkTimeMs: Long,
    val randomnessCpWindow: Int,
    val description: String
) {
    BEGINNER(1, "Pemula", 600, 1, 450L, 180, "Cocok untuk baru belajar langkah dasar; terkadang membuat celah taktik."),
    EASY(2, "Mudah", 950, 2, 650L, 95, "Mengembangkan bidak dan memakan bidak gratis; santai dan ramah pemula."),
    MEDIUM(3, "Menengah", 1350, 3, 900L, 35, "Menerapkan prinsip pembukaan, kendali pusat, dan kalkulasi 3 langkah."),
    HARD(4, "Sulit", 1700, 4, 1300L, 10, "Taktis tingkat klub dengan pencarian kombo dan keamanan Raja yang solid."),
    EXPERT(5, "Ahli", 2050, 5, 1800L, 0, "Kalkulasi posisi & taktik mendalam dengan penguasaan babak akhir."),
    MASTER(6, "Master", 2400, 6, 2600L, 0, "Kekuatan penuh pencarian alpha-beta dan evaluasi posisi tanpa kompromi.")
}

enum class TimeControlCategory(val label: String) {
    BULLET("Peluru (Bullet)"),
    BLITZ("Kilat (Blitz)"),
    RAPID("Cepat (Rapid)"),
    CLASSICAL("Klasik"),
    UNLIMITED("Tanpa Batas"),
    CUSTOM("Kustom")
}

data class TimeControlPreset(
    val id: String,
    val name: String,
    val category: TimeControlCategory,
    val initialTimeMinutes: Int,
    val incrementSeconds: Int
) {
    val isUnlimited: Boolean
        get() = category == TimeControlCategory.UNLIMITED || initialTimeMinutes <= 0

    val initialTimeMs: Long
        get() = initialTimeMinutes * 60_000L

    val incrementMs: Long
        get() = incrementSeconds * 1_000L

    companion object {
        val ALL_PRESETS = listOf(
            TimeControlPreset("bullet_1_0", "1 + 0", TimeControlCategory.BULLET, 1, 0),
            TimeControlPreset("bullet_2_1", "2 + 1", TimeControlCategory.BULLET, 2, 1),
            TimeControlPreset("blitz_3_0", "3 + 0", TimeControlCategory.BLITZ, 3, 0),
            TimeControlPreset("blitz_3_2", "3 + 2", TimeControlCategory.BLITZ, 3, 2),
            TimeControlPreset("blitz_5_0", "5 + 0", TimeControlCategory.BLITZ, 5, 0),
            TimeControlPreset("blitz_5_3", "5 + 3", TimeControlCategory.BLITZ, 5, 3),
            TimeControlPreset("rapid_10_0", "10 + 0", TimeControlCategory.RAPID, 10, 0),
            TimeControlPreset("rapid_10_5", "10 + 5", TimeControlCategory.RAPID, 10, 5),
            TimeControlPreset("rapid_15_10", "15 + 10", TimeControlCategory.RAPID, 15, 10),
            TimeControlPreset("classical_30_0", "30 + 0", TimeControlCategory.CLASSICAL, 30, 0),
            TimeControlPreset("unlimited", "Tanpa Batas", TimeControlCategory.UNLIMITED, 0, 0)
        )
    }
}
