package com.example.data.content

data class HowToPlayTopic(
    val number: Int,
    val title: String,
    val summary: String,
    val detailedExplanation: String,
    val rulesBulletPoints: List<String>,
    val interactiveFen: String,
    val demoMovesUci: List<String>
)

data class TrainingLevel(
    val levelNumber: Int,
    val title: String,
    val subtitle: String,
    val corePillars: List<String>,
    val dailyPracticePlan: List<String>,
    val weeklyTrainingPlan: List<String>,
    val recommendedExercises: List<String>,
    val milestoneIds: List<Pair<String, String>>
)

object MasteryGuideCatalog {

    val howToPlayTopics: List<HowToPlayTopic> = listOf(
        HowToPlayTopic(
            number = 1,
            title = "1. Objective of Chess",
            summary = "Trap the opponent's King in Checkmate while protecting your own.",
            detailedExplanation = "Chess is a two-player strategy board game played between White and Black. The ultimate goal is not to capture every piece, but to place the opponent's King under inescapable attack—known as Checkmate.",
            rulesBulletPoints = listOf(
                "White always moves first, and players alternate one move per turn.",
                "A game ends immediately when a King is checkmated, a player resigns, time runs out, or a draw rule is triggered."
            ),
            interactiveFen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            demoMovesUci = listOf("d1d8")
        ),
        HowToPlayTopic(
            number = 2,
            title = "2. Chessboard Orientation",
            summary = "'White on the Right' and the 8×8 coordinate grid.",
            detailedExplanation = "The chessboard consists of 64 alternating light and dark squares arranged in 8 vertical files (a–h) and 8 horizontal ranks (1–8). Always make sure the bottom-right corner square closest to each player is a light square.",
            rulesBulletPoints = listOf(
                "Files a through d make up the Queenside; files e through h make up the Kingside.",
                "Ranks 1–4 belong to White's half of the board; ranks 5–8 belong to Black's half."
            ),
            interactiveFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            demoMovesUci = listOf("e2e4", "e7e5")
        ),
        HowToPlayTopic(
            number = 3,
            title = "3. Initial Piece Setup",
            summary = "Rooks in the corners, Knights next, Bishops inside, and Queen on her own color.",
            detailedExplanation = "Each side begins with 16 pieces: 8 Pawns on the second rank (Rank 2 for White, Rank 7 for Black) and 8 major/minor pieces on the back rank (Rank 1 for White, Rank 8 for Black).",
            rulesBulletPoints = listOf(
                "Back rank from left to right (a to h): Rook, Knight, Bishop, Queen, King, Bishop, Knight, Rook.",
                "White Queen starts on the light square d1; Black Queen starts on the dark square d8."
            ),
            interactiveFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            demoMovesUci = listOf("d2d4", "d7d5", "c2c4", "e7e6")
        ),
        HowToPlayTopic(
            number = 4,
            title = "4. How Every Piece Moves",
            summary = "Distinct movement geometry for all six chess piece types.",
            detailedExplanation = "Every chess piece has a signature movement pattern: Rooks move along ranks and files; Bishops move along diagonals; Queens combine both; Knights jump in an L-shape; Kings step one square in any direction; and Pawns march forward.",
            rulesBulletPoints = listOf(
                "Only the Knight can jump over intervening pieces.",
                "No piece may ever land on a square occupied by a friendly piece."
            ),
            interactiveFen = "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
            demoMovesUci = listOf("b1c3", "g8f6", "d2d3")
        ),
        HowToPlayTopic(
            number = 5,
            title = "5. How Capturing Works",
            summary = "Replace an enemy piece on its square and remove it from the game.",
            detailedExplanation = "Pieces capture enemy pieces by moving onto the square occupied by the opponent's piece. All pieces capture the same way they move, except Pawns, which move straight forward but capture one square diagonally forward.",
            rulesBulletPoints = listOf(
                "Capturing is optional in chess—unlike checkers, you are only forced to capture if it is the sole way out of check.",
                "The King is never actually captured off the board; the game stops at Checkmate."
            ),
            interactiveFen = "r1bqkbnr/ppp2ppp/2np4/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 0 4",
            demoMovesUci = listOf("d4e5", "d6e5", "d1d8", "e8d8")
        ),
        HowToPlayTopic(
            number = 6,
            title = "6. Check",
            summary = "Direct attack on the King that must be parried immediately.",
            detailedExplanation = "When a King is under immediate attack by one or more opponent pieces, it is in Check. A player is strictly forbidden from making any move that leaves or places their own King in check.",
            rulesBulletPoints = listOf(
                "Remember the 'CPR' defense against Check: Capture the attacker, Protect (block the line of attack), or Run (move the King to a safe square).",
                "Against a Knight check or a Double Check, blocking is impossible—the King must move (or capture the single Knight)."
            ),
            interactiveFen = "rnbqk1nr/pppp1ppp/8/4p3/1b2P3/2P5/PP1P1PPP/RNBQKBNR w KQkq - 1 3",
            demoMovesUci = listOf("c3b4")
        ),
        HowToPlayTopic(
            number = 7,
            title = "7. Checkmate",
            summary = "The King is in check and has zero legal moves to escape.",
            detailedExplanation = "If a player's King is in check and there is no legal way to move the King to safety, block the check, or capture the checking piece, the position is Checkmate and the attacking player wins immediately.",
            rulesBulletPoints = listOf(
                "Checkmate ends the game regardless of how much material either player has left.",
                "In algebraic notation, Checkmate is denoted by the '#' symbol (e.g., Qxf7#)."
            ),
            interactiveFen = "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1",
            demoMovesUci = listOf("e1e8")
        ),
        HowToPlayTopic(
            number = 8,
            title = "8. Stalemate",
            summary = "No legal moves available while NOT in check results in an immediate Draw.",
            detailedExplanation = "If the player whose turn it is to move is NOT in check, but every possible move would place their King into check (leaving zero legal moves), the game ends immediately in a Stalemate—which is a Draw (half a point each).",
            rulesBulletPoints = listOf(
                "When winning easily, make sure each move either gives check or leaves the enemy King at least one safe square.",
                "When losing, look for clever sacrifices that leave your remaining pieces completely immobilized for a Stalemate rescue!"
            ),
            interactiveFen = "7k/5K2/8/8/8/8/8/6Q1 w - - 0 1",
            demoMovesUci = listOf("g1g6")
        ),
        HowToPlayTopic(
            number = 9,
            title = "9. Castling",
            summary = "Move the King two squares toward a Rook and hop the Rook over.",
            detailedExplanation = "Once per game, each King can perform Castling to reach safety and activate a Rook simultaneously. Move the King two squares toward the Rook (e1 to g1 or c1; e8 to g8 or c8), and place that Rook on the square the King crossed.",
            rulesBulletPoints = listOf(
                "Neither the King nor the chosen Rook may have moved earlier in the game.",
                "All squares between the King and Rook must be empty.",
                "The King cannot castle out of check, through check, or into check."
            ),
            interactiveFen = "r3k2r/pppq1ppp/2npbn2/2b1p3/2B1P3/2NPBN2/PPPQ1PPP/R3K2R w KQkq - 6 7",
            demoMovesUci = listOf("e1g1", "e8c8")
        ),
        HowToPlayTopic(
            number = 10,
            title = "10. En Passant",
            summary = "Capturing an enemy pawn 'in passing' right after its two-square advance.",
            detailedExplanation = "If your pawn has advanced to the 5th rank (for White) or 4th rank (for Black) and an enemy pawn on an adjacent file advances two squares in a single move to land right next to your pawn, you may capture it diagonally onto the square it skipped over.",
            rulesBulletPoints = listOf(
                "You must play the En Passant capture on the very next turn; if you play another move first, the right expires.",
                "Only pawns can capture En Passant, and only pawns that just double-stepped can be captured."
            ),
            interactiveFen = "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3",
            demoMovesUci = listOf("e5f6")
        ),
        HowToPlayTopic(
            number = 11,
            title = "11. Pawn Promotion",
            summary = "A pawn reaching the 8th (or 1st) rank transforms into a Queen, Rook, Bishop, or Knight.",
            detailedExplanation = "Pawns cannot move backward, so when a pawn reaches the farthest rank on the board, it is immediately promoted to a Queen, Rook, Bishop, or Knight of the same color on that promotion square.",
            rulesBulletPoints = listOf(
                "You can promote to a second (or third!) Queen even if your original Queen is still on the board.",
                "Choosing a Knight, Rook, or Bishop instead of a Queen is called 'underpromotion' (useful to avoid stalemate or give a Knight fork)."
            ),
            interactiveFen = "8/4P1k1/8/8/8/8/8/4K3 w - - 0 1",
            demoMovesUci = listOf("e7e8q")
        ),
        HowToPlayTopic(
            number = 12,
            title = "12. Winning and Drawing",
            summary = "All official ways a chess match is won or drawn.",
            detailedExplanation = "A player wins by Checkmate, opponent Resignation, or opponent Timeout (when the opponent's clock expires and you have sufficient mating material). Draws occur by Stalemate, Mutual Agreement, Threefold Repetition, the 50/75-Move Rule, or Insufficient Material.",
            rulesBulletPoints = listOf(
                "Threefold Repetition: Identical position with the same side to move and same rights occurs 3 times.",
                "50-Move / 75-Move Rule: 50 consecutive full moves by both players without any pawn move or capture.",
                "Insufficient Material: Neither side has enough pieces to force checkmate (e.g., K vs K, K+B vs K, K+N vs K)."
            ),
            interactiveFen = "4k3/8/8/8/8/8/8/4K1N1 w - - 0 1",
            demoMovesUci = listOf("g1f3")
        )
    )

    val trainingLevels: List<TrainingLevel> = listOf(
        TrainingLevel(
            levelNumber = 1,
            title = "LEVEL 1 — BEGINNER",
            subtitle = "Board vision, legal rules, piece safety, and opening fundamentals",
            corePillars = listOf(
                "Learn all piece movements, castling, en passant, and promotion fluently.",
                "Understand Check, Checkmate, and Stalemate patterns.",
                "Eliminate one-move hanging pieces by checking opponent threats every turn.",
                "Apply the three golden opening principles: Center, Development, King Safety."
            ),
            dailyPracticePlan = listOf(
                "10 mins: Solve 5 Beginner Mate-in-1 and Win-Material puzzles.",
                "15 mins: Play one Rapid (10+0 or 15+10) match with Beginner Mode or Coach review.",
                "5 mins: Review any move where an undefended piece was lost."
            ),
            weeklyTrainingPlan = listOf(
                "Complete Chapters 1, 2, 3, and 4 in Chess Academy.",
                "Practice the King & Rook and King & Queen basic checkmates against the computer.",
                "Play 5 unhurried Rapid games focusing on zero one-move blunders."
            ),
            recommendedExercises = listOf(
                "Before every move, ask: 'What did my opponent's last move threaten?'",
                "Castle before move 10 in every game you play this week."
            ),
            milestoneIds = listOf(
                "m1_rules" to "Master all piece movements & special rules (Castling, En Passant, Promotion)",
                "m1_mates" to "Execute Back-Rank and Queen+Helper checkmates without hints",
                "m1_opening" to "Develop all minor pieces and castle within the first 10 moves"
            )
        ),
        TrainingLevel(
            levelNumber = 2,
            title = "LEVEL 2 — IMPROVING PLAYER",
            subtitle = "Fundamental tactics, forcing moves, and 2-move calculation",
            corePillars = listOf(
                "Learn basic tactical motifs: Forks, Pins, Skewers, and Discovered Attacks.",
                "Practice scanning for 'Checks, Captures, and Threats' (CCT) on every turn.",
                "Calculate short 2-to-3 ply forcing variations before touching a piece.",
                "Develop pieces efficiently and coordinate Rooks on open files."
            ),
            dailyPracticePlan = listOf(
                "15 mins: Solve 8 Tactical Puzzles (Forks, Pins, Skewers, Mate in 2).",
                "20 mins: Play one Rapid 10+5 or 15+10 match against Medium/Hard AI.",
                "10 mins: Run Post-Game Engine Analysis to inspect every Mistake and Blunder."
            ),
            weeklyTrainingPlan = listOf(
                "Complete Chapter 5 (Tactics) in Chess Academy.",
                "Maintain a 5-day Daily Puzzle streak.",
                "Win 3 games against the Easy and Medium AI without using Undo."
            ),
            recommendedExercises = listOf(
                "Identify all undefended ('loose') pieces on both sides of the board during middlegames.",
                "Practice visualizing 2 moves ahead without moving pieces on the screen."
            ),
            milestoneIds = listOf(
                "m2_fork_pin" to "Spot Knight forks and absolute pins in live games",
                "m2_cct" to "Apply the Checks, Captures, Threats (CCT) checklist on every turn",
                "m2_review" to "Review 5 completed games using the local Engine Analysis screen"
            )
        ),
        TrainingLevel(
            levelNumber = 3,
            title = "LEVEL 3 — INTERMEDIATE",
            subtitle = "Positional understanding, pawn structures, openings, and essential endgames",
            corePillars = listOf(
                "Learn positional play: weak squares, Knight outposts, and good vs. bad Bishops.",
                "Understand core pawn structures (isolated d-pawn, doubled pawns, passed pawns).",
                "Build a coherent opening repertoire for White and against 1.e4 / 1.d4 for Black.",
                "Master foundational King + Pawn (Opposition) and Rook endgames."
            ),
            dailyPracticePlan = listOf(
                "15 mins: Solve Intermediate & Advanced tactical and endgame puzzles.",
                "20 mins: Play a serious 15+10 match against Hard AI or a Local/Online opponent.",
                "10 mins: Compare your opening choices against Chapter 6 Opening Principles."
            ),
            weeklyTrainingPlan = listOf(
                "Complete Chapters 6, 7, and 8 in Chess Academy.",
                "Practice 5 endgame positions starting from FEN setups.",
                "Eliminate early Queen sorties and unprovoked flank pawn pushes."
            ),
            recommendedExercises = listOf(
                "Ask during quiet positions: 'Which of my pieces is the least active, and how can I improve it?'",
                "Practice converting a 1-pawn advantage in King & Pawn endings using the Opposition."
            ),
            milestoneIds = listOf(
                "m3_openings" to "Understand the core ideas of the Italian, Ruy Lopez, Queen's Gambit & Sicilian",
                "m3_outposts" to "Identify weak squares and establish central Knight outposts",
                "m3_opposition" to "Demonstrate Direct Opposition and passed pawn conversion in endgames"
            )
        ),
        TrainingLevel(
            levelNumber = 4,
            title = "LEVEL 4 — ADVANCED",
            subtitle = "Strategic planning, prophylaxis, complex calculation, and time discipline",
            corePillars = listOf(
                "Analyze complex imbalances (Bishop pair vs. Knight outpost, queenside majority vs. kingside attack).",
                "Practice Prophylaxis: anticipate and neutralize the opponent's plan before executing your own.",
                "Study multi-phase Rook endgames (7th-rank invasion, active Rook behind passed pawns).",
                "Manage chess clock time efficiently—avoid spending 5 minutes on obvious recaptures."
            ),
            dailyPracticePlan = listOf(
                "20 mins: Calculate deep Mate-in-3 and Tactical Combination puzzles to completion before moving.",
                "25 mins: Play against Expert AI under tournament Rapid/Classical time controls.",
                "15 mins: Annotate key turning points where the evaluation graph shifted by >1.0 pawn."
            ),
            weeklyTrainingPlan = listOf(
                "Play 3 long games (15+10 or 30+0) with strict clock discipline.",
                "Review every Inaccuracy (`?!`) in the Engine Analysis report to refine positional precision."
            ),
            recommendedExercises = listOf(
                "Before choosing a candidate move, write down or mentally name your opponent's best constructive plan.",
                "Practice Rook + Pawn vs. Rook endgame defense and conversion."
            ),
            milestoneIds = listOf(
                "m4_prophylaxis" to "Prevent opponent tactical & positional ideas proactively (Prophylaxis)",
                "m4_rook_endings" to "Place Rooks behind passed pawns and control the 7th rank",
                "m4_clock" to "Complete timed Rapid matches with zero time-trouble blunders"
            )
        ),
        TrainingLevel(
            levelNumber = 5,
            title = "LEVEL 5 — EXPERT TRAINING",
            subtitle = "Complete game review discipline, candidate move trees, and tournament mastery",
            corePillars = listOf(
                "Analyze complete games from opening transition through endgame technique.",
                "Use structured Candidate Move trees (2–3 candidate moves evaluated cleanly without jumping back and forth).",
                "Maintain composure and objective evaluation under pressure in both winning and defending positions.",
                "Develop consistent post-game review habits to turn every defeat into permanent pattern recognition."
            ),
            dailyPracticePlan = listOf(
                "20 mins: High-difficulty combination & defensive puzzles without hints.",
                "30 mins: Full match against Master AI or competitive human opponent.",
                "15 mins: Complete the 6-Point Post-Game Review Checklist after each game."
            ),
            weeklyTrainingPlan = listOf(
                "Export PGNs of your toughest games and replay critical positions from both sides.",
                "Audit your statistics to identify whether tactical oversights or endgame transitions cause more losses."
            ),
            recommendedExercises = listOf(
                "When defending a worse position, look for active counterplay rather than passive waiting moves.",
                "When converting a winning position, trade Queens or active enemy pieces to extinguish counterplay."
            ),
            milestoneIds = listOf(
                "m5_candidates" to "Apply structured Candidate Move selection in complex positions",
                "m5_conversion" to "Convert winning advantages cleanly by neutralizing opponent counterplay",
                "m5_master_ai" to "Contest or defeat the Level 6 Master AI in a standard game"
            )
        )
    )

    val postGameReviewChecklist: List<Pair<String, String>> = listOf(
        "chk_opening" to "1. Opening Transition: Did I develop all minor pieces, control the center, and castle on time?",
        "chk_opponent_threat" to "2. Threat Awareness: Did I identify the purpose of my opponent's last move before playing mine?",
        "chk_hanging" to "3. Piece Safety: Did I leave any piece or pawn undefended (hanging) to a 1- or 2-move tactic?",
        "chk_worst_piece" to "4. Positional Activity: In quiet positions, did I improve my least active piece?",
        "chk_missed_tactics" to "5. Forcing Moves (CCT): Did I check all Checks, Captures, and Threats for both sides?",
        "chk_time_usage" to "6. Clock Management: Did I allocate thinking time to critical turning points rather than routine moves?"
    )
}
