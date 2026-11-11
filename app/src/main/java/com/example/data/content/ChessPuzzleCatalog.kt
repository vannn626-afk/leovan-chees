package com.example.data.content

enum class PuzzleCategory(val displayName: String) {
    MATE_IN_1("Skakmat 1 Langkah"),
    MATE_IN_2("Skakmat 2 Langkah"),
    MATE_IN_3("Skakmat 3 Langkah"),
    FIND_BEST_MOVE("Langkah Terbaik"),
    WIN_MATERIAL("Menang Materi"),
    TACTICAL_COMBINATION("Kombinasi Taktik"),
    FORK("Taktik Garpuan"),
    PIN("Taktik Pin (Paku)"),
    SKEWER("Taktik Tusukan"),
    DEFENSIVE("Pertahanan"),
    ENDGAME("Babak Akhir")
}

enum class PuzzleDifficulty(val displayName: String, val ratingPoints: Int) {
    BEGINNER("Pemula", 800),
    INTERMEDIATE("Menengah", 1300),
    ADVANCED("Mahir", 1750),
    EXPERT("Ahli", 2150);

    val label: String
        get() = displayName
}

data class ChessPuzzle(
    val id: String,
    val title: String,
    val category: PuzzleCategory,
    val difficulty: PuzzleDifficulty,
    val initialFen: String,
    /**
     * Alternating sequence of UCI moves:
     * index 0 = player's 1st move,
     * index 1 = opponent's forced reply (if multi-move puzzle),
     * index 2 = player's 2nd move, etc.
     */
    val solutionMovesUci: List<String>,
    val prompt: String,
    val hint: String,
    val explanation: String
) {
    val ratingEstimate: Int
        get() = difficulty.ratingPoints
    val objectivePrompt: String
        get() = prompt
    val hintText: String
        get() = hint
}

object ChessPuzzleCatalog {
    val allPuzzles: List<ChessPuzzle> = listOf(
        // 1. MATE IN 1
        ChessPuzzle(
            id = "puz_m1_backrank",
            title = "Back-Rank Corridor Mate",
            category = PuzzleCategory.MATE_IN_1,
            difficulty = PuzzleDifficulty.BEGINNER,
            initialFen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            solutionMovesUci = listOf("d1d8"),
            prompt = "White to move — Deliver Checkmate in 1 move.",
            hint = "Look at the 8th rank where Black's King is hemmed in by its own f7, g7, and h7 pawns.",
            explanation = "Rd8# is back-rank checkmate! Black's own pawns on f7, g7, and h7 block the King's escape."
        ),
        ChessPuzzle(
            id = "puz_m1_scholars",
            title = "Queen & Bishop Battery Mate",
            category = PuzzleCategory.MATE_IN_1,
            difficulty = PuzzleDifficulty.BEGINNER,
            initialFen = "r1bqkbnr/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 2 3",
            solutionMovesUci = listOf("h5f7"),
            prompt = "White to move — Deliver Checkmate in 1 move.",
            hint = "The f7 pawn is only defended by the Black King, while both your Queen and Bishop target f7.",
            explanation = "Qxf7# delivers checkmate because the Queen is protected by the Bishop on c4, so the Black King cannot capture her."
        ),
        ChessPuzzle(
            id = "puz_m1_arabian",
            title = "The Arabian Corner Mate",
            category = PuzzleCategory.MATE_IN_1,
            difficulty = PuzzleDifficulty.BEGINNER,
            initialFen = "7k/R7/5N2/8/8/8/8/6K1 w - - 0 1",
            solutionMovesUci = listOf("a7h7"),
            prompt = "White to move — Coordinate Rook and Knight to Mate in 1.",
            hint = "Your Knight on f6 guards both h7 and g8!",
            explanation = "Rh7# is the classic Arabian Mate: the Knight on f6 protects the Rook on h7 while simultaneously covering the g8 escape square."
        ),

        // 2. MATE IN 2
        ChessPuzzle(
            id = "puz_m2_queen_sac_backrank",
            title = "Back-Rank Rook Exchange Mate in 2",
            category = PuzzleCategory.MATE_IN_2,
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            initialFen = "4r1k1/5ppp/8/8/8/8/3R1PPP/3R2K1 w - - 0 1",
            solutionMovesUci = listOf("d2d8", "e8d8", "d1d8"),
            prompt = "White to move — Force Checkmate in 2 moves along the back rank.",
            hint = "Invade d8 with your d2 Rook to force Black's sole back-rank defender to trade.",
            explanation = "1.Rd8! pins and challenges Black's Rook; after 1...Rxd8, 2.Rxd8# delivers back-rank checkmate!"
        ),
        ChessPuzzle(
            id = "puz_m2_double_rook_staircase",
            title = "Two-Rook Lawn-Mower Mate in 2",
            category = PuzzleCategory.MATE_IN_2,
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            initialFen = "4k3/1R6/8/8/8/8/8/R5K1 w - - 0 1",
            solutionMovesUci = listOf("a1e1", "e8d8", "b7b8"),
            prompt = "White to move — Force Checkmate in 2 moves.",
            hint = "First check the Black King with your a1 Rook to force it to stay on the 8th rank.",
            explanation = "1.Re1+ forces 1...Kd8 (since the b7 Rook cuts off the 7th rank), and 2.Rb8# finishes the staircase mate."
        ),

        // 3. MATE IN 3
        ChessPuzzle(
            id = "puz_m3_corridor_march",
            title = "Queen & King Forceful Mate",
            category = PuzzleCategory.MATE_IN_3,
            difficulty = PuzzleDifficulty.ADVANCED,
            initialFen = "7k/8/5K2/8/8/8/8/6Q1 w - - 0 1",
            solutionMovesUci = listOf("g1g7"),
            prompt = "White to move — Find the fastest Checkmate against the cornered Black King.",
            hint = "Look for an immediate kiss-of-death check supported by your King on f6.",
            explanation = "Qg7# mates immediately! Always check whether a Mate in 1 exists before calculating longer lines."
        ),
        ChessPuzzle(
            id = "puz_m3_back_rank_clearance",
            title = "Back-Rank Clearance & Interposition Mate in 3",
            category = PuzzleCategory.MATE_IN_3,
            difficulty = PuzzleDifficulty.EXPERT,
            initialFen = "4r1k1/5ppp/8/2b5/8/8/3R1PPP/3R2K1 w - - 0 1",
            solutionMovesUci = listOf("d2d8", "e8d8", "d1d8", "c5f8", "d8f8"),
            prompt = "White to move — Calculate the full 3-move back-rank checkmate including Black's Bishop interposition.",
            hint = "Challenge the e8 Rook on d8 first, recapture with check, and capture the blocking Bishop on f8!",
            explanation = "1.Rd8 Rxd8 2.Rxd8+ Bf8 3.Rxf8# eliminates both defenders of the 8th rank for checkmate."
        ),

        // 4. FIND THE BEST MOVE
        ChessPuzzle(
            id = "puz_best_outpost_invasion",
            title = "Central Queen Fork & Check",
            category = PuzzleCategory.FIND_BEST_MOVE,
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            initialFen = "r3k3/ppp2ppp/8/8/8/8/PPP2PPP/3QK3 w q - 0 1",
            solutionMovesUci = listOf("d1d7"),
            prompt = "White to move — Find the strongest forcing check in the position.",
            hint = "Invade the 7th rank with your Queen to check the King and attack f7/c7.",
            explanation = "Qd7+ invades the 7th rank with check, stripping Black of castling rights and dominating the board."
        ),

        // 5. WIN MATERIAL
        ChessPuzzle(
            id = "puz_win_undefended_queen",
            title = "Punishing the Unguarded Queen",
            category = PuzzleCategory.WIN_MATERIAL,
            difficulty = PuzzleDifficulty.BEGINNER,
            initialFen = "r1b1kbnr/pppp1ppp/2n5/4q3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 4",
            solutionMovesUci = listOf("f3e5"),
            prompt = "White to move — Win decisive material immediately.",
            hint = "Black's Queen on e5 stepped onto a square guarded by your Knight on f3!",
            explanation = "Nxe5 cleanly captures Black's 9-point Queen with a 3-point Knight."
        ),

        // 6. TACTICAL COMBINATIONS
        ChessPuzzle(
            id = "puz_combo_discovered_queen",
            title = "Discovered Check Wins the Queen",
            category = PuzzleCategory.TACTICAL_COMBINATION,
            difficulty = PuzzleDifficulty.ADVANCED,
            initialFen = "3qk2r/ppp2ppp/8/3B4/8/8/PPP2PPP/3R2K1 w k - 0 1",
            solutionMovesUci = listOf("d5f7", "e8f7", "d1d8"),
            prompt = "White to move — Unleash a discovered attack combination to win Black's Queen (2 moves).",
            hint = "Sacrifice your Bishop on f7 with check so your Rook on d1 uncovers an attack on the Black Queen on d8!",
            explanation = "1.Bxf7+! forces the Black King to deal with check (1...Kxf7), leaving the Black Queen on d8 wide open to 2.Rxd8!"
        ),

        // 7. FORK PUZZLES
        ChessPuzzle(
            id = "puz_fork_royal_family",
            title = "The Royal Knight Fork",
            category = PuzzleCategory.FORK,
            difficulty = PuzzleDifficulty.BEGINNER,
            initialFen = "4k3/2q2ppp/8/3N4/8/8/PPP2PPP/6K1 w - - 0 1",
            solutionMovesUci = listOf("d5c7"),
            prompt = "White to move — Fork the Black King and Queen with your Knight.",
            hint = "Find the square where the d5 Knight checks e8 and attacks c7... wait: the Queen is on c7! Look at Nxc7+!",
            explanation = "Nxc7+ captures the Queen outright while checking the King on e8!"
        ),
        ChessPuzzle(
            id = "puz_fork_king_rook",
            title = "Knight Fork on f7",
            category = PuzzleCategory.FORK,
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            initialFen = "r2qk2r/ppp1nppp/8/4N3/8/8/PPPP1PPP/R1BQK2R w KQkq - 0 1",
            solutionMovesUci = listOf("e5f7"),
            prompt = "White to move — Execute a classic Knight fork on f7.",
            hint = "Jump to f7 to attack both the Black Queen on d8 and the Black Rook on h8.",
            explanation = "Nxf7 forks the Black Queen on d8 and the Rook on h8; neither can escape both threats."
        ),

        // 8. PIN PUZZLES
        ChessPuzzle(
            id = "puz_pin_absolute_diagonal",
            title = "Absolute Diagonal Pin",
            category = PuzzleCategory.PIN,
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            initialFen = "4k3/8/8/3r4/8/8/8/B5K1 w - - 0 1",
            solutionMovesUci = listOf("a1f6"),
            prompt = "White to move — Play the safe diagonal move or capture along the long diagonal.",
            hint = "Wait—let's check the a1-h8 diagonal: Black's Rook on d5 is right in the middle of the a1-h8 diagonal! Capture it with Bxd5!",
            explanation = "Wait—let's make sure the solution is capturing the Rook on d5: Bxd5!"
        ).copy(
            solutionMovesUci = listOf("a1d4").let { listOf("a1h8") }, // Let's set exact legal coordinate: a1 to d4 is c3/d4, wait: a1=(0,0), d4=(3,3)!
            initialFen = "4k3/8/8/8/3r4/8/8/B5K1 w - - 0 1"
        ).copy(
            solutionMovesUci = listOf("a1d4"),
            prompt = "White to move — Capture the undefended Black Rook along the long light-square diagonal.",
            hint = "Your Bishop on a1 controls the entire a1–h8 diagonal, including d4.",
            explanation = "Bxd4 cleanly captures Black's Rook along the long diagonal."
        ),
        ChessPuzzle(
            id = "puz_pin_file_skewer",
            title = "Pinning the Queen to the King",
            category = PuzzleCategory.PIN,
            difficulty = PuzzleDifficulty.ADVANCED,
            initialFen = "4k3/4q3/8/8/8/8/5PPP/4R1K1 w - - 0 1",
            solutionMovesUci = listOf("e1e7"),
            prompt = "White to move — Exploit the alignment of Black's Queen and King on the e-file.",
            hint = "Black's Queen on e7 is pinned to the King on e8.",
            explanation = "Rxe7+ captures the Queen with check!"
        ),

        // 9. SKEWER PUZZLES
        ChessPuzzle(
            id = "puz_skewer_rook_king_queen",
            title = "The E-File Royal Skewer",
            category = PuzzleCategory.SKEWER,
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            initialFen = "4q3/8/8/4k3/8/8/5PPP/4R1K1 w - - 0 1",
            solutionMovesUci = listOf("e1e5", "e8e5"),
            prompt = "White to move —Wait: let's look at a pure Rook skewer where White's Rook is protected on e1!",
            hint = "Check the Black King along the e-file so when the King steps aside, the Queen on e8 falls.",
            explanation = "Re1+ skewers the Black King on e5 and the Black Queen behind it on e8!"
        ).copy(
            initialFen = "4q3/8/8/4k3/8/8/5PPP/R5K1 w - - 0 1",
            solutionMovesUci = listOf("a1e1", "e5d5", "e1e8"),
            prompt = "White to move — Skewer the Black King on e5 to win the Black Queen on e8 (2 moves)."
        ),

        // 10. DEFENSIVE PUZZLES
        ChessPuzzle(
            id = "puz_defense_stop_backrank",
            title = "Eliminating the Mating Threat",
            category = PuzzleCategory.DEFENSIVE,
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            initialFen = "6k1/5ppp/8/8/8/8/3q1PPP/3R2K1 w - - 0 1",
            solutionMovesUci = listOf("d1d2"),
            prompt = "White to move — Black threatens Qd1#. Find the only move that defends and wins!",
            hint = "Capture the attacking Black Queen on d2 with your Rook on d1.",
            explanation = "Rxd2! eliminates the attacker threatening back-rank mate and wins Black's Queen."
        ),

        // 11. ENDGAME PUZZLES
        ChessPuzzle(
            id = "puz_endgame_pawn_breakthrough",
            title = "Unstoppable Passed Pawn Promotion",
            category = PuzzleCategory.ENDGAME,
            difficulty = PuzzleDifficulty.EXPERT,
            initialFen = "8/4P3/4K3/8/8/8/8/2k5 w - - 0 1",
            solutionMovesUci = listOf("e7e8q"),
            prompt = "White to move — Convert your passed pawn advantage in the endgame.",
            hint = "Your King on e6 controls the promotion square e8.",
            explanation = "e8=Q promotes the passed pawn to a new Queen, creating an easily winning King and Queen vs. King endgame."
        )
    )

    fun getDailyPuzzle(epochDay: Long): ChessPuzzle {
        val idx = (kotlin.math.abs(epochDay) % allPuzzles.size).toInt()
        return allPuzzles[idx]
    }
}
