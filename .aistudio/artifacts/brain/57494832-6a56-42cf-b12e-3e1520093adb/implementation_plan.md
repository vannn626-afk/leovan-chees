# LEOVAN CHEESY — Premium Classic Chess Game

A complete, commercial-grade Android chess application crafted by **LEOVAN STUDIO** with a full international-standard chess rules engine, 6-tier offline computer AI, local pass-and-play, real-time online 1v1 multiplayer, an 8-chapter interactive Chess Academy, verified tactical puzzles, post-game engine analysis, and three art-directed visual themes.

## User Review & Critical Decisions

> [!IMPORTANT]
> Review the recommended defaults below. Click **Proceed** to start building immediately, or share any adjustments in chat.

- **Online 1v1 & Offline Architecture (Recommended Default)**: All core modes—**Play vs Computer (6 AI levels)**, **Local 1v1**, **Beginner Mode**, **Chess Academy**, **Chess Puzzles**, **From Beginner to Master**, **How to Play**, **Game Analysis**, and **Profile/Statistics**—work **100% offline with zero login required**, persisted locally via Room Database. **Online 1v1 Multiplayer** uses **Firebase Firestore** with Google Sign-In required only when entering the Online lobby/rooms.
- **Board Interaction & Piece Movement (Recommended Default)**: Supports **both Tap-to-Select / Tap-to-Move and Drag-and-Drop** simultaneously with smooth spring animations, legal destination indicators, and magnetic square snapping.
- **Post-Game Engine Analysis (Recommended Default)**: Automatically computes material balance, captures, SAN move log, and evaluation graph upon game completion, with an interactive move-by-move engine review classifying **Best Moves, Inaccuracies, Mistakes, and Blunders**.

---

## 1. Overview & Core Concept

- **What It Does**: Delivers a complete, timeless chess platform combining tournament-accurate gameplay, adaptive offline AI, local and online 1v1 matches, guided beginner assistance, structured lessons, tactical puzzles, and engine-powered game review.
- **Target Audience / Persona**: From first-time learners mastering piece movements in Beginner Mode and the Chess Academy to seasoned club and tournament players practicing openings, solving tactical puzzles, and competing in timed matches.
- **Key Value**: Uncompromised FIDE rule accuracy, high-definition custom vector/Canvas Staunton chess pieces, zero-dependency offline AI and learning modules, and an art-directed classical aesthetic across three complete themes.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Main Menu Hub**: Opens to the **LEOVAN CHEESY** signature header ("THINK. MOVE. MASTER. — LEOVAN STUDIO"), an interactive live mini-chessboard preview, quick action cards (**Continue Saved Game**, **Quick Play**, **Daily Puzzle**, **Current Academy Progress**), and structured navigation to all 8 core sections.
2. **Play Menu & Match Setup**:
   - **Vs Computer (Offline AI)**: Select difficulty (**Beginner, Easy, Medium, Hard, Expert, Master**), playing color (**White, Black, Random**), time control (**Bullet, Blitz, Rapid, Classical, Custom, Unlimited**), AI thinking time, and toggle **Beginner Guided Mode**.
   - **Local 1v1**: Configure Player 1 & Player 2 names, chess clock time control, auto-rotate board after each turn, and consent-based undo.
   - **Online 1v1**: Authenticate with Google Sign-In when entering online mode, create a private room with a shareable 6-character code, join via room code, or enter random matchmaking with real-time board and clock synchronization and server-validated rules.
3. **Active Gameplay & Beginner Assistance**:
   - Precision 8×8 chessboard with rank/file coordinates, custom Staunton pieces, captured piece trays with material differential (`+2`, `+5`), dual digital chess clocks with increment support, and a scrollable SAN algebraic move strip.
   - When **Beginner Mode** is active: displays piece movement rules on selection, highlights all legal moves and capture targets, warns when the King is in check, highlights threatened pieces, explains why an attempted illegal move is forbidden, and offers tactical hints.
4. **Chess Academy & Improvement Roadmap**:
   - **8 Structured Chapters** (*Chess Basics, Piece Movement, Special Rules, Beginning Strategy, Tactics, Opening Principles, Middlegame, Endgame*) with interactive demonstration boards and hands-on exercises.
   - **From Beginner to Master (5 Levels)**: Structured training plans (daily/weekly routines, skill milestones, interactive exercises, and game review checklists).
   - **How to Play**: Visual, interactive reference guide covering all 12 core topics from board setup to special rules.
5. **Chess Puzzles & Daily Challenge**:
   - Filter by 11 tactical categories (*Mate in 1/2/3, Best Move, Win Material, Combinations, Fork, Pin, Skewer, Defensive, Endgame*) and 4 difficulties (*Beginner to Expert*), plus a **Daily Puzzle** with streak tracking.
6. **Post-Game Review & Local Engine Analysis**:
   - Displays winner/loser, termination reason, duration, captures, evaluation graph across all plies, and move-by-move classifications (*Best Move, Inaccuracy, Mistake, Blunder, Missed Opportunity*) with interactive board replay and PGN/FEN export.

### Visual Identity & Three Complete Themes
- **Aesthetic Direction**: *Classic. Elegant. Clean. Intelligent. Timeless.* No generic AI gradients or futuristic clutter—crafted with refined borders, subtle surface textures, and tournament-grade contrast.
- **Typography & Hierarchy**:
  - *Display / Headers*: **Playfair Display** (`R.font.playfair_display`) for authoritative classical titles, chapter headings, and brand identity.
  - *UI & Body*: **Plus Jakarta Sans** (`R.font.plus_jakarta_sans`) for crisp readability across lessons, menus, and analytics.
  - *Clocks & Algebraic Notation*: **JetBrains Mono** (`R.font.jetbrains_mono`) for tabular chess clocks, FEN/PGN strings, and SAN move tables.
- **Three Switchable Complete Themes** (transforming board squares, Staunton piece shading, backgrounds, menus, buttons, and move highlights):
  1. **THEME A — CLASSIC WOOD (Default)**:
     - *Atmosphere*: Warm walnut background (`#1E140F`), natural oak surfaces (`#2D1F18`), cream light squares (`#EDD6B0`), rich walnut dark squares (`#8B5A2B`), warm gold accents (`#D4AF37`), and ivory/ebony Staunton pieces with warm wood-toned shading.
  2. **THEME B — ROYAL BLUE**:
     - *Atmosphere*: Deep navy background (`#0B162C`), royal slate surfaces (`#152642`), icy white-blue light squares (`#E6EEF8`), tournament royal blue dark squares (`#3B6998`), crisp cyan-gold accents (`#60A5FA`), and tournament-contrast Staunton pieces.
  3. **THEME C — MONOCHROME**:
     - *Atmosphere*: Pure obsidian black (`#0A0A0A`), charcoal surfaces (`#1A1A1A`), stark alabaster light squares (`#F0F0F0`), slate-charcoal dark squares (`#4A4A4A`), silver-gray accents (`#D4D4D8`), and high-contrast minimalist Staunton pieces.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Custom High-Definition Canvas/Vector Staunton Piece Renderer**
  - *Chosen Approach*: Render all 12 Staunton chess pieces (White & Black King, Queen, Rook, Bishop, Knight, Pawn) using multi-layered vector paths with inner bevels, crown/collar highlights, base pedestals, and soft drop shadows tailored to the active theme.
  - *Why*: Guarantees razor-sharp silhouettes at any screen density or tablet layout without ever relying on emoji characters or blurry bitmaps.
- **Decision 2: Complete FIDE Legal Engine + 6-Tier Local Minimax/Alpha-Beta AI**
  - *Chosen Approach*: Pure Kotlin chess engine implementing all 23 required FIDE rules (including en passant, castling rights & attack path checks, promotion, 50-move/75-move rules, threefold Zobrist repetition, and insufficient material), paired with an offline AI using iterative deepening, alpha-beta pruning, transposition tables, MVV-LVA move ordering, piece-square tables, opening book principles, and endgame King-activity evaluation.
  - *Why*: Ensures 100% offline reliability, zero API latency, and genuinely distinct playing strength across Beginner (depth 1 + intentional human-like softness) through Master (deep iterative search with tactical quiescence and opening/endgame heuristics).
- **Decision 3: Dual Persistence Strategy (Room Offline-First + Firestore Online Rooms)**
  - *Chosen Approach*: Store player profile, statistics, saved games, PGN history, puzzle streaks, Academy progress, and theme/audio settings locally in **Room Database** so offline play requires no login. Use **Firebase Firestore** with transactional move validation and security rules for **Online 1v1 Multiplayer** rooms.
  - *Why*: Honors the requirement that offline gameplay works without login or internet while delivering real-time, cheat-resistant online 1v1 matches.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram
```
┌────────────────────────────────────────────────────────────────────────────┐
│                        LEOVAN CHEESY — UI LAYER (Compose)                  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌───────────────────┐  │
│  │  Main Menu & │ │  Chessboard  │ │ Chess Academy│ │  Puzzles, Review, │  │
│  │  Theme Host  │ │  & Clocks UI │ │ & HowToPlay  │ │  Mastery & Stats  │  │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └─────────┬─────────┘  │
└─────────┼────────────────┼────────────────┼───────────────────┼────────────┘
          │                │                │                   │
          ▼                ▼                ▼                   ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     VIEWMODEL & STATE LAYER (StateFlow)                    │
│  ┌────────────────────┐ ┌────────────────────┐ ┌────────────────────────┐  │
│  │   GameViewModel    │ │  Academy & Puzzle  │ │  OnlineMatchViewModel  │  │
│  │ (AI, Local, Clock, │ │     ViewModels     │ │ (Rooms, Matchmaking,   │  │
│  │  Beginner, Review) │ │ (Lessons, Streaks) │ │  Realtime Sync, Auth)  │  │
│  └─────────┬──────────┘ └─────────┬──────────┘ └───────────┬────────────┘  │
└────────────┼──────────────────────┼────────────────────────┼───────────────┘
             │                      │                        │
             ▼                      ▼                        ▼
┌─────────────────────────┐ ┌──────────────────┐ ┌───────────────────────────┐
│   CHESS ENGINE CORE     │ │ LOCAL REPOSITORY │ │  ONLINE MULTIPLAYER REPO  │
│ • Board & FEN/PGN Codec │ │   (Room SQLite)  │ │   (Firebase Firestore)    │
│ • Full Legal Move Gen   │ │ • PlayerProfile  │ │ • /online_rooms/{roomId}  │
│ • Zobrist & Draw Rules  │ │ • SavedGameEntity│ │ • /online_users/{uid}     │
│ • 6-Tier Alpha-Beta AI  │ │ • LessonProgress │ │ • Server Rule Validation  │
│ • Post-Game Evaluator   │ │ • PuzzleProgress │ │ • Turn & Clock Sync       │
└─────────────────────────┘ └──────────────────┘ └───────────────────────────┘
```

### Data Model & State
- **Core Engine Models**: `BoardState` (64-square array, active color, castling bitmask, en passant target square, halfmove clock, fullmove number, Zobrist hash history), `ChessMove` (from, to, piece, capturedPiece, promotion, isCastling, isEnPassant, sanNotation, evaluationCp), and `GameStatus` (*InProgress, Check, Checkmate, Stalemate, DrawFiftyMove, DrawSeventyFiveMove, DrawRepetition, DrawInsufficientMaterial, DrawDeadPosition, DrawByAgreement, Resigned, TimeOut*).
- **Local Room Entities**:
  - `PlayerProfileEntity`: Player name, avatar ID, games played/won/lost/drawn, puzzle rating & streak, total play time, active theme (`CLASSIC_WOOD`, `ROYAL_BLUE`, `MONOCHROME`), board & sound settings.
  - `GameHistoryEntity`: Complete PGN, final FEN, mode, opponent/AI level, time control, winner, move count, duration, and serialized move evaluations.
  - `LessonProgressEntity` & `PuzzleProgressEntity`: Completion state, stars/attempts, and daily puzzle streak history.
- **Firestore Online Collections** (when Firebase is enabled):
  - `online_rooms/{roomId}`: `roomCode`, `whiteUid`, `whiteName`, `blackUid`, `blackName`, `status` (`WAITING`, `ACTIVE`, `COMPLETED`), `currentFen`, `pgn`, `turnColor`, `lastMoveFrom`, `lastMoveTo`, `whiteTimeRemainingMs`, `blackTimeRemainingMs`, `incrementMs`, `drawOfferBy`, `winner`, `terminationReason`, `memberUids`, `updatedAt`.
  - `online_users/{uid}`: `displayName`, `gamesPlayed`, `gamesWon`, `onlineStatus`, `updatedAt`.

### Interactive Component & State Mapping
- **Interactive Board & Move Execution**: Selecting or dragging a piece queries `ChessEngine.getLegalMovesForSquare(square)`. If **Beginner Mode** is enabled, attempting an illegal destination triggers an explanatory toast/banner detailing *why* the move is illegal (e.g., *"Illegal move: Your King would remain in check"* or *"Bishops only move diagonally"*).
- **Chess Clock & Lifecycle**: A coroutine ticker updates active player millisecond clocks at 100ms intervals, adds increment upon legal move commit, pauses automatically when the pause modal or app background state triggers, and declares timeout immediately at `00:00.0`.
- **Sound Synthesizer**: Generates subtle, realistic acoustic chess audio (wood tap on move, crisp snap on capture, resonant chime on check/victory) via Android `AudioTrack` synthesis with independent Sound Effects and ambient Music toggles in Settings.
