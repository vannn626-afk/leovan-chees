package com.example.data.content

data class ImprovementMilestone(
    val id: String,
    val stage: String,
    val title: String,
    val description: String
)

data class AcademyLesson(
    val id: String,
    val chapterId: Int,
    val title: String,
    val subtitle: String,
    val explanation: String,
    val keyTakeaways: List<String>,
    val initialFen: String,
    val demonstrationMovesUci: List<String>,
    val exercisePrompt: String,
    val exerciseSolutionUci: String,
    val successFeedback: String,
    val mistakeFeedback: String
) {
    val summary: String
        get() = subtitle
    val exerciseHint: String
        get() = mistakeFeedback
    val keyTakeaway: String
        get() = successFeedback
}

data class AcademyChapter(
    val chapterNumber: Int,
    val title: String,
    val description: String,
    val lessons: List<AcademyLesson>
) {
    val id: Int
        get() = chapterNumber
    val number: Int
        get() = chapterNumber
    val subtitle: String
        get() = description
}

object ChessAcademyCatalog {
    val chapters: List<AcademyChapter> = listOf(
        AcademyChapter(
            chapterNumber = 1,
            title = "Chapter 1 — Chess Basics",
            description = "Introduction to chess, board coordinates, piece identification, initial setup, and turn order.",
            lessons = listOf(
                AcademyLesson(
                    id = "c1_l1_intro",
                    chapterId = 1,
                    title = "1.1 Introduction & Initial Setup",
                    subtitle = "The 64-square battlefield and starting army",
                    explanation = "Chess is played on an 8×8 grid of 64 alternating light and dark squares. Always orient the board so a light square sits in your bottom-right corner ('white on the right'). White's army starts on ranks 1 and 2; Black's army mirrors it on ranks 7 and 8. The Queen always starts on her own color: White Queen on d1 (light), Black Queen on d8 (dark).",
                    keyTakeaways = listOf(
                        "Bottom-right corner square is always a light square (h1 for White, a8 for Black).",
                        "Pawns occupy the second rank in front of the major and minor pieces.",
                        "White always makes the first move of the game."
                    ),
                    initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    demonstrationMovesUci = listOf("e2e4", "e7e5", "g1f3", "b8c6"),
                    exercisePrompt = "White always plays first. Make the classic King's Pawn opening move by advancing the e2 pawn two squares to e4.",
                    exerciseSolutionUci = "e2e4",
                    successFeedback = "Excellent! 1.e4 stakes an immediate claim in the center and opens diagonals for your Queen and King's Bishop.",
                    mistakeFeedback = "Select the White pawn on e2 and advance it two squares to e4."
                ),
                AcademyLesson(
                    id = "c1_l2_coordinates",
                    chapterId = 1,
                    title = "1.2 Board Coordinates & Turns",
                    subtitle = "Files (a–h), Ranks (1–8), and Algebraic Notation",
                    explanation = "Vertical columns are called files (labeled a through h from left to right from White's view). Horizontal rows are called ranks (numbered 1 to 8 from White's home row to Black's home row). Every square has a unique coordinate like e4, c6, or f7.",
                    keyTakeaways = listOf(
                        "Files run vertically (a–h); Ranks run horizontally (1–8).",
                        "Players alternate turns strictly—you must move one piece per turn.",
                        "Knowing coordinates helps you read chess books and review your games."
                    ),
                    initialFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                    demonstrationMovesUci = listOf("g1f3", "b8c6", "f1c4"),
                    exercisePrompt = "Develop your kingside Knight from g1 to square f3 to attack Black's e5 pawn.",
                    exerciseSolutionUci = "g1f3",
                    successFeedback = "Correct! Nf3 develops a minor piece toward the center and targets e5.",
                    mistakeFeedback = "Move the White Knight on g1 to the square f3."
                )
            )
        ),
        AcademyChapter(
            chapterNumber = 2,
            title = "Chapter 2 — Piece Movement",
            description = "Master how the Pawn, Rook, Knight, Bishop, Queen, and King move and capture.",
            lessons = listOf(
                AcademyLesson(
                    id = "c2_l1_rook_bishop",
                    chapterId = 2,
                    title = "2.1 The Rook & The Bishop",
                    subtitle = "Files, ranks, and long-range diagonals",
                    explanation = "The Rook moves any number of unobstructed squares horizontally along ranks or vertically along files. The Bishop moves any number of unobstructed squares diagonally, always staying on the same square color it started on.",
                    keyTakeaways = listOf(
                        "Rooks thrive on open files with no pawns blocking them.",
                        "Each player starts with one light-squared Bishop and one dark-squared Bishop."
                    ),
                    initialFen = "4k3/8/8/3p4/8/8/8/3R1B1K w - - 0 1",
                    demonstrationMovesUci = listOf("d1d5"),
                    exercisePrompt = "Use your Rook on d1 to capture the undefended Black pawn straight ahead on d5.",
                    exerciseSolutionUci = "d1d5",
                    successFeedback = "Well done! Rooks capture by landing directly on the square of the enemy piece along their file or rank.",
                    mistakeFeedback = "Move the White Rook from d1 straight up the d-file to d5."
                ),
                AcademyLesson(
                    id = "c2_l2_queen_knight",
                    chapterId = 2,
                    title = "2.2 The Queen & The Knight",
                    subtitle = "Combined ray power and the L-shaped jump",
                    explanation = "The Queen combines the powers of the Rook and Bishop, moving straight or diagonally any number of open squares. The Knight moves in an 'L' shape (two squares in one orthogonal direction, then one square perpendicular) and is the only piece that can jump over other pieces.",
                    keyTakeaways = listOf(
                        "The Queen is the most powerful attacking piece (worth 9 points).",
                        "A Knight always lands on a square of the opposite color from where it started."
                    ),
                    initialFen = "4k3/8/5p2/8/4N3/8/8/3Q3K w - - 0 1",
                    demonstrationMovesUci = listOf("e4f6"),
                    exercisePrompt = "Jump with your Knight on e4 to capture Black's pawn on f6, giving check at the same time!",
                    exerciseSolutionUci = "e4f6",
                    successFeedback = "Brilliant! Nxf6+ demonstrates the Knight's L-shaped leap and attacks the King.",
                    mistakeFeedback = "Move the Knight from e4 to f6 in an L-shape."
                ),
                AcademyLesson(
                    id = "c2_l3_pawn_king",
                    chapterId = 2,
                    title = "2.3 Pawns, King & Captures",
                    subtitle = "Forward marches, diagonal captures, and royal steps",
                    explanation = "Pawns move straight forward one square (or optionally two squares from their initial rank), but they capture one square diagonally forward. The King moves one square in any direction, provided the destination square is not attacked by an enemy piece.",
                    keyTakeaways = listOf(
                        "Pawns never move or capture backward.",
                        "The King can capture an enemy piece only if that piece is undefended."
                    ),
                    initialFen = "4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1",
                    demonstrationMovesUci = listOf("e4d5"),
                    exercisePrompt = "Capture the Black pawn on d5 diagonally using your White pawn on e4.",
                    exerciseSolutionUci = "e4d5",
                    successFeedback = "Spot on! exd5 captures diagonally and advances your pawn toward promotion.",
                    mistakeFeedback = "Move the White pawn on e4 diagonally to d5."
                )
            )
        ),
        AcademyChapter(
            chapterNumber = 3,
            title = "Chapter 3 — Special Rules",
            description = "Learn Castling, En Passant, Pawn Promotion, Check, Checkmate, and Stalemate.",
            lessons = listOf(
                AcademyLesson(
                    id = "c3_l1_castling",
                    chapterId = 3,
                    title = "3.1 Castling (Kingside & Queenside)",
                    subtitle = "Safeguarding the King and activating the Rook in one move",
                    explanation = "Castling is the only move where two pieces move at once: the King moves two squares toward a Rook, and that Rook hops over to the square the King crossed. You can castle only if neither the King nor that Rook has moved, the squares between them are empty, the King is not in check, and the King does not pass through or land on an attacked square.",
                    keyTakeaways = listOf(
                        "Kingside castling (O-O) moves the White King from e1 to g1 and Rook from h1 to f1.",
                        "Queenside castling (O-O-O) moves the White King from e1 to c1 and Rook from a1 to d1."
                    ),
                    initialFen = "r1bqk1nr/pppp1ppp/2n5/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                    demonstrationMovesUci = listOf("e1g1"),
                    exercisePrompt = "Castle Kingside (O-O) by moving your King on e1 two squares right to g1.",
                    exerciseSolutionUci = "e1g1",
                    successFeedback = "Perfect! Your King is tucked safely on g1 while your Rook enters the game on f1.",
                    mistakeFeedback = "Select the White King on e1 and move it two squares to g1."
                ),
                AcademyLesson(
                    id = "c3_l2_en_passant",
                    chapterId = 3,
                    title = "3.2 En Passant ('In Passing')",
                    subtitle = "The special pawn capture rule",
                    explanation = "When an opponent's pawn advances two squares from its starting rank and lands directly beside your pawn on the 5th rank (for White) or 4th rank (for Black), you may capture it diagonally as if it had only moved one square. You must exercise this right on the very next turn or lose it.",
                    keyTakeaways = listOf(
                        "En passant only applies to a pawn versus a pawn that just double-stepped.",
                        "It must be played immediately on the next half-move."
                    ),
                    initialFen = "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3",
                    demonstrationMovesUci = listOf("e5f6"),
                    exercisePrompt = "Black just played f7–f5 beside your e5 pawn. Capture it En Passant by moving your e5 pawn diagonally to f6!",
                    exerciseSolutionUci = "e5f6",
                    successFeedback = "Excellent! exf6 e.p. removes Black's f5 pawn from the board.",
                    mistakeFeedback = "Move the White pawn on e5 diagonally to the target square f6."
                ),
                AcademyLesson(
                    id = "c3_l3_promotion_checkmate",
                    chapterId = 3,
                    title = "3.3 Pawn Promotion, Check & Checkmate",
                    subtitle = "Transforming a pawn and ending the game",
                    explanation = "When a pawn reaches the farthest rank (8th rank for White, 1st rank for Black), it immediately promotes to a Queen, Rook, Bishop, or Knight. If an enemy King is under attack ('in Check') and has zero legal moves to escape, block, or capture the attacker, it is Checkmate and the game is won! If a player is NOT in check and has zero legal moves, it is a Stalemate (Draw).",
                    keyTakeaways = listOf(
                        "Promoting to a Queen is most common, though underpromotion to a Knight can occasionally deliver a fork.",
                        "Always verify you don't accidentally stalemate a lone enemy King when ahead!"
                    ),
                    initialFen = "6k1/4Pppp/8/8/8/8/8/6K1 w - - 0 1",
                    demonstrationMovesUci = listOf("e7e8q"),
                    exercisePrompt = "Advance your pawn from e7 to e8 and promote it to a Queen to deliver immediate back-rank Checkmate!",
                    exerciseSolutionUci = "e7e8q",
                    successFeedback = "Checkmate! e8=Q# promotes the pawn and traps the Black King behind its own pawns.",
                    mistakeFeedback = "Advance the pawn from e7 to e8 and choose Queen."
                )
            )
        ),
        AcademyChapter(
            chapterNumber = 4,
            title = "Chapter 4 — Beginning Strategy",
            description = "Control the center, develop pieces, protect the King, and understand material value.",
            lessons = listOf(
                AcademyLesson(
                    id = "c4_l1_center_development",
                    chapterId = 4,
                    title = "4.1 Control the Center & Develop Pieces",
                    subtitle = "Why squares d4, e4, d5, and e5 govern the board",
                    explanation = "Pieces placed in or aiming at the central squares (d4, e4, d5, e5) control more squares and can rapidly shift to either flank. In the opening, develop your Knights and Bishops once each before moving the Queen or moving the same piece twice.",
                    keyTakeaways = listOf(
                        "Knights before Bishops is a helpful rule of thumb.",
                        "Avoid bringing the Queen out too early where minor pieces can chase her with tempo."
                    ),
                    initialFen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
                    demonstrationMovesUci = listOf("f1c4", "g8f6", "d2d3"),
                    exercisePrompt = "Develop your light-squared Bishop from f1 to the active c4 diagonal aiming at f7.",
                    exerciseSolutionUci = "f1c4",
                    successFeedback = "Great development! Bc4 controls the center, eyes f7, and clears the path for Kingside castling.",
                    mistakeFeedback = "Move the White Bishop from f1 to c4."
                ),
                AcademyLesson(
                    id = "c4_l2_material_value",
                    chapterId = 4,
                    title = "4.2 Understanding Material Value",
                    subtitle = "Pawn = 1, Knight/Bishop = 3, Rook = 5, Queen = 9",
                    explanation = "Every piece has a relative point value based on its mobility: Pawn (1), Knight (3), Bishop (3), Rook (5), and Queen (9). Trading a minor piece (3) for a Rook (5) wins 'the Exchange' (+2). Always check whether a target is defended before capturing!",
                    keyTakeaways = listOf(
                        "Never trade a high-value piece for a defended lower-value piece without concrete compensation.",
                        "Two minor pieces (Knight + Bishop = 6) are usually stronger than a single Rook (5)."
                    ),
                    initialFen = "3r2k1/ppp2ppp/8/8/3n4/8/PPP1BPPP/3R2K1 w - - 0 1",
                    demonstrationMovesUci = listOf("d1d4"),
                    exercisePrompt = "Black's Knight on d4 is completely undefended (hanging). Capture it with your Rook on d1!",
                    exerciseSolutionUci = "d1d4",
                    successFeedback = "Clean capture! Rxd4 wins a full minor piece (+3 points) while keeping your Bishop safe.",
                    mistakeFeedback = "Use the White Rook on d1 to capture the hanging Black Knight on d4."
                )
            )
        ),
        AcademyChapter(
            chapterNumber = 5,
            title = "Chapter 5 — Tactics",
            description = "Master Forks, Pins, Skewers, Discovered Attacks, Deflections, Decoys, and Removing the Defender.",
            lessons = listOf(
                AcademyLesson(
                    id = "c5_l1_fork",
                    chapterId = 5,
                    title = "5.1 The Double Attack & Knight Fork",
                    subtitle = "Attacking two high-value targets simultaneously",
                    explanation = "A Fork occurs when a single piece attacks two or more enemy pieces at the same time. Knights are notorious for 'Royal Forks' against the enemy King and Queen or Rook because enemy Queens and Rooks cannot counter-attack an L-shaped jump.",
                    keyTakeaways = listOf(
                        "Look for undefended squares where your Knight checks the King and attacks another piece.",
                        "Pawns, Bishops, and Queens can also execute powerful forks."
                    ),
                    initialFen = "r3k3/ppp2ppp/8/3N4/8/8/PPP2PPP/4K3 w q - 0 1",
                    demonstrationMovesUci = listOf("d5c7"),
                    exercisePrompt = "Execute a Knight fork by jumping from d5 to c7, checking the Black King on e8 and attacking the Rook on a8!",
                    exerciseSolutionUci = "d5c7",
                    successFeedback = "Royal Knight Fork! Nxc7+ forces the Black King to move, allowing Nxa8 on the next move.",
                    mistakeFeedback = "Move the White Knight from d5 to c7."
                ),
                AcademyLesson(
                    id = "c5_l2_pin_skewer",
                    chapterId = 5,
                    title = "5.2 The Pin & The Skewer",
                    subtitle = "Exploiting pieces aligned on a rank, file, or diagonal",
                    explanation = "A Pin traps a defender in front of a more valuable piece behind it (an 'Absolute Pin' against the King makes moving the pinned piece illegal). A Skewer is the reverse: a high-value piece (like the King or Queen) is attacked in front, and when it moves away, the piece behind it is captured.",
                    keyTakeaways = listOf(
                        "Pile up pressure on a pinned piece using pawns and minor pieces.",
                        "Long-range pieces (Bishops, Rooks, Queens) are the only pieces that can pin or skewer."
                    ),
                    initialFen = "4k3/8/4q3/8/8/8/8/4R1K1 w - - 0 1",
                    demonstrationMovesUci = listOf("e1e6"),
                    exercisePrompt = "Black's Queen on e6 is aligned with the Black King on e8, and we already pinned it—now capture the pinned Queen on e6 with your Rook!",
                    exerciseSolutionUci = "e1e6",
                    successFeedback = "Decisive! Rxe6+ wins the Queen and checks the King simultaneously.",
                    mistakeFeedback = "Capture the Black Queen on e6 with your Rook on e1."
                ),
                AcademyLesson(
                    id = "c5_l3_discovered_removing_defender",
                    chapterId = 5,
                    title = "5.3 Discovered Attack & Removing the Defender",
                    subtitle = "Unleashing hidden batteries and eliminating key guards",
                    explanation = "A Discovered Attack happens when moving one of your pieces unblocks a line of attack from a Rook, Bishop, or Queen behind it. 'Removing the Defender' (or Deflection/Decoy) eliminates or lures away the piece protecting a critical square or target.",
                    keyTakeaways = listOf(
                        "A Discovered Check allows the unblocking piece to capture almost anything with impunity.",
                        "Ask: 'Which enemy piece is guarding my target, and how can I trade or deflect it?'"
                    ),
                    initialFen = "3qk1nr/ppp2ppp/8/3N4/8/8/PPP2PPP/3Q2K1 w - - 0 1",
                    demonstrationMovesUci = listOf("d5f6"),
                    exercisePrompt = "Move your Knight from d5 to f6 with Check, unveiling a discovered attack from your Queen on d1 against Black's Queen on d8!",
                    exerciseSolutionUci = "d5f6",
                    successFeedback = "Lethal discovered check! Nf6+ forces Black to respond to the check while your Queen on d1 captures the Black Queen on d8 next.",
                    mistakeFeedback = "Jump the Knight from d5 to f6 to give check and uncover your Queen on d1."
                )
            )
        ),
        AcademyChapter(
            chapterNumber = 6,
            title = "Chapter 6 — Opening Principles",
            description = "Study the Italian Game, Ruy Lopez, Queen's Gambit, Sicilian, French, Caro-Kann, and London System.",
            lessons = listOf(
                AcademyLesson(
                    id = "c6_l1_italian_ruy_lopez",
                    chapterId = 6,
                    title = "6.1 Open Games: Italian Game & Ruy Lopez",
                    subtitle = "Classical 1.e4 e5 master repertoires",
                    explanation = "After 1.e4 e5 2.Nf3 Nc6, playing 3.Bc4 enters the Italian Game (targeting the vulnerable f7 pawn and controlling d5), while 3.Bb5 enters the Ruy Lopez (pressuring the c6 Knight that defends e5). Both lead to rich strategic and tactical battles.",
                    keyTakeaways = listOf(
                        "Italian Game (3.Bc4): Rapid development and direct central pressure with c3 and d4.",
                        "Ruy Lopez (3.Bb5): Long-term strategic pressure favored by World Champions."
                    ),
                    initialFen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
                    demonstrationMovesUci = listOf("f1b5", "a7a6", "b5a4"),
                    exercisePrompt = "Enter the famous Ruy Lopez (Spanish Opening) by moving your Bishop from f1 to b5.",
                    exerciseSolutionUci = "f1b5",
                    successFeedback = "Classic! 3.Bb5 initiates the Ruy Lopez, challenging Black's defender of the e5 pawn.",
                    mistakeFeedback = "Move the White Bishop from f1 to b5."
                ),
                AcademyLesson(
                    id = "c6_l2_queens_gambit_london",
                    chapterId = 6,
                    title = "6.2 Queen's Gambit & London System",
                    subtitle = "Strategic 1.d4 openings for White",
                    explanation = "After 1.d4 d5, playing 2.c4 is the Queen's Gambit—White offers a flank pawn to deflect Black's central d5 pawn (if 2...dxc4, White easily regains the pawn while dominating the center). Alternatively, the London System (1.d4 followed by Bf4, e3, Nf3, c3) builds a rock-solid pyramid structure against almost any Black setup.",
                    keyTakeaways = listOf(
                        "The Queen's Gambit is not a true sacrifice; Black cannot profitably hold the c4 pawn.",
                        "The London System develops the dark-squared Bishop to f4 outside the pawn chain before playing e3."
                    ),
                    initialFen = "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2",
                    demonstrationMovesUci = listOf("c2c4", "e7e6", "b1c3"),
                    exercisePrompt = "Play the Queen's Gambit by advancing your c2 pawn two squares to c4 to challenge Black's d5 center.",
                    exerciseSolutionUci = "c2c4",
                    successFeedback = "Excellent! 2.c4 is the cornerstone of classical 1.d4 play.",
                    mistakeFeedback = "Advance the White pawn from c2 to c4."
                ),
                AcademyLesson(
                    id = "c6_l3_sicilian_french_carokann",
                    chapterId = 6,
                    title = "6.3 Sicilian, French & Caro-Kann Defenses",
                    subtitle = "Asymmetrical and resilient defenses against 1.e4",
                    explanation = "Against 1.e4, Black need not copy with 1...e5: the Sicilian Defense (1...c5) fights for d4 asymmetrically; the French Defense (1...e6 2.d4 d5) builds a counter-attacking chain; and the Caro-Kann Defense (1...c6 2.d4 d5) supports d5 while keeping the light-squared Bishop's diagonal open.",
                    keyTakeaways = listOf(
                        "1...c5 (Sicilian) is the highest-scoring fighting reply to 1.e4.",
                        "1...c6 (Caro-Kann) and 1...e6 (French) create resilient central fortresses."
                    ),
                    initialFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
                    demonstrationMovesUci = listOf("c7c5", "g1f3", "d7d6"),
                    exercisePrompt = "As Black, answer 1.e4 with the dynamic Sicilian Defense by moving your c7 pawn to c5.",
                    exerciseSolutionUci = "c7c5",
                    successFeedback = "Sharp choice! 1...c5 immediately contests the d4 square without symmetry.",
                    mistakeFeedback = "Move the Black pawn on c7 two squares to c5."
                )
            )
        ),
        AcademyChapter(
            chapterNumber = 7,
            title = "Chapter 7 — Middlegame Strategy",
            description = "Positional play, pawn structure, piece activity, weak squares, outposts, and creating plans.",
            lessons = listOf(
                AcademyLesson(
                    id = "c7_l1_outposts_weak_squares",
                    chapterId = 7,
                    title = "7.1 Weak Squares, Outposts & Piece Activity",
                    subtitle = "Anchoring Knights in enemy territory",
                    explanation = "A 'weak square' or 'hole' is a square that can no longer be guarded by an enemy pawn. Placing a Knight on a central weak square supported by your own pawn creates an 'Outpost'—dominating the board and cramping the opponent's position.",
                    keyTakeaways = listOf(
                        "Pawns cannot move backward, so every pawn advance leaves permanent squares behind it.",
                        "Knights on 5th- or 6th-rank central outposts often rival a Rook in strength."
                    ),
                    initialFen = "r4rk1/pp2ppbp/2np2p1/8/3NP3/2N5/PPP2PPP/R4RK1 w - - 0 1",
                    demonstrationMovesUci = listOf("d4c6", "b7c6", "c3d5"),
                    exercisePrompt = "Anchor your c3 Knight onto the powerful central outpost on d5 where no Black pawn on e6 or c6 can ever chase it away!",
                    exerciseSolutionUci = "c3d5",
                    successFeedback = "Masterful positional play! Nd5 occupies the central hole and pressures e7.",
                    mistakeFeedback = "Move the White Knight from c3 to the central outpost square d5."
                ),
                AcademyLesson(
                    id = "c7_l2_open_files_plans",
                    chapterId = 7,
                    title = "7.2 Pawn Structure, Open Files & Creating Plans",
                    subtitle = "Seizing the 7th rank with Rooks",
                    explanation = "When evaluating a middlegame position, examine the pawn structure (isolated, doubled, or backward pawns) and open files (files with zero pawns). The standard plan for Rooks is to control open files and penetrate to the 7th rank to attack unadvanced enemy pawns and trap the enemy King.",
                    keyTakeaways = listOf(
                        "Double your Rooks on an open file to win control of the entry squares.",
                        "A Rook on the 7th rank is a classic winning positional advantage."
                    ),
                    initialFen = "6k1/pp3ppp/8/8/8/8/PP3PPP/2R3K1 w - - 0 1",
                    demonstrationMovesUci = listOf("c1c7"),
                    exercisePrompt = "Invade the 7th rank along the open c-file by moving your Rook from c1 to c7!",
                    exerciseSolutionUci = "c1c7",
                    successFeedback = "Dominant! Rc7 attacks both b7 and f7 while restricting Black's King to the back rank.",
                    mistakeFeedback = "Move the White Rook from c1 to c7."
                )
            )
        ),
        AcademyChapter(
            chapterNumber = 8,
            title = "Chapter 8 — Endgame Mastery",
            description = "King and pawn endings, the Opposition, passed pawns, Rook endings, and basic checkmate patterns.",
            lessons = listOf(
                AcademyLesson(
                    id = "c8_l1_king_opposition",
                    chapterId = 8,
                    title = "8.1 King & Pawn Endings: The Opposition",
                    subtitle = "Leading with the King to escort a passed pawn",
                    explanation = "In the endgame, the King becomes an active fighting piece! In King and Pawn vs. King endings, your King must lead *in front* of the pawn. Taking 'Direct Opposition' means placing your King two squares directly facing the enemy King on the same file or rank with the opponent to move, forcing their King to give way.",
                    keyTakeaways = listOf(
                        "Always advance the King in front of a lone pawn to secure the queening square.",
                        "The side NOT to move when Kings stand one square apart holds the Opposition."
                    ),
                    initialFen = "4k3/8/8/4P3/4K3/8/8/8 w - - 0 1",
                    demonstrationMovesUci = listOf("e4d5", "e8d7", "e5e6"),
                    exercisePrompt = "Step your King forward in front of your pawn from e4 to d5 (or f5) to outflank the Black King—let's play Kd5!",
                    exerciseSolutionUci = "e4d5",
                    successFeedback = "Textbook endgame technique! Stepping the King to the 6th rank in front of the pawn guarantees promotion.",
                    mistakeFeedback = "Move the White King from e4 to d5."
                ),
                AcademyLesson(
                    id = "c8_l2_basic_mates_rook_endings",
                    chapterId = 8,
                    title = "8.2 Basic Checkmate Patterns: King & Rook Mate",
                    subtitle = "Delivering the box checkmate on the edge of the board",
                    explanation = "To checkmate a lone King with a King and Rook (or King and Queen), drive the enemy King to the edge of the board, align your King in opposition so the enemy King cannot escape toward the center, and deliver the final check along the edge rank or file with your Rook.",
                    keyTakeaways = listOf(
                        "A lone Rook cannot checkmate without the help of its own King.",
                        "Cut off ranks or files step-by-step like a shrinking box."
                    ),
                    initialFen = "4k3/R7/4K3/8/8/8/8/8 w - - 0 1",
                    demonstrationMovesUci = listOf("a7a8"),
                    exercisePrompt = "The Black King on e8 is trapped on the back rank by your King in opposition on e6. Deliver Checkmate with your Rook on a7!",
                    exerciseSolutionUci = "a7a8",
                    successFeedback = "Checkmate! Ra8# completes the classic King and Rook box mate.",
                    mistakeFeedback = "Move the White Rook from a7 to a8 to deliver checkmate."
                )
            )
        )
    )

    val totalLessonCount: Int
        get() = chapters.sumOf { it.lessons.size }

    val improvementMilestones: List<ImprovementMilestone> = listOf(
        ImprovementMilestone(
            id = "beg_1",
            stage = "Beginner",
            title = "Kuasai Gerakan 6 Bidak & Nilai Poinnya",
            description = "Pahami langkah Pion (1), Kuda (3), Gajah (3), Benteng (5), Menteri (9), dan Raja."
        ),
        ImprovementMilestone(
            id = "beg_2",
            stage = "Beginner",
            title = "Pahami Aturan Khusus: Rokade, En Passant & Promosi",
            description = "Lakukan rokade lebih awal untuk mengamankan Raja dan kenali tangkapan En Passant."
        ),
        ImprovementMilestone(
            id = "beg_3",
            stage = "Beginner",
            title = "Hindari Bidak Menggantung (Gratis)",
            description = "Sebelum melepas langkah, pastikan bidak Anda tidak bisa dimakan gratis oleh lawan."
        ),
        ImprovementMilestone(
            id = "int_1",
            stage = "Intermediate",
            title = "Kuasai Taktik Garpuan, Pin (Paku), dan Tusukan",
            description = "Selesaikan minimal 15 teka-teki taktik untuk mengenali pola kombinasi 2 langkah."
        ),
        ImprovementMilestone(
            id = "int_2",
            stage = "Intermediate",
            title = "Terapkan 3 Prinsip Pembukaan Catur",
            description = "Kendalikan pusat papan, kembangkan Kuda & Gajah sebelum Menteri, dan hubungkan kedua Benteng."
        ),
        ImprovementMilestone(
            id = "int_3",
            stage = "Intermediate",
            title = "Kuasai Skakmat Dasar (Raja + Menteri / Benteng)",
            description = "Latih menggiring Raja lawan ke tepi papan menggunakan oposisi Raja dan Benteng."
        ),
        ImprovementMilestone(
            id = "adv_1",
            stage = "Advanced",
            title = "Evaluasi Struktur Pion & Petak Lemah (Outpost)",
            description = "Tempatkan Kuda di petak pusat yang tidak bisa diusir pion lawan dan kuasai lajur terbuka dengan Benteng."
        ),
        ImprovementMilestone(
            id = "adv_2",
            stage = "Advanced",
            title = "Teknik Babak Akhir: Oposisi Raja & Pion Bebas",
            description = "Pimpin pion bebas dengan Raja di depannya untuk mengamankan petak promosi."
        ),
        ImprovementMilestone(
            id = "adv_3",
            stage = "Advanced",
            title = "Analisis Setiap Pertandingan Selesai",
            description = "Tinjau akurasi dan grafik evaluasi untuk menemukan langkah terbaik yang terlewat."
        )
    )
}
