package com.example

import com.example.data.content.ChessAcademyCatalog
import com.example.data.content.ChessPuzzleCatalog
import com.example.data.online.AuthoritativeServerReferee
import com.example.engine.AiDifficulty
import com.example.engine.ChessAiEngine
import com.example.engine.ChessEngine
import com.example.engine.FenPgnCodec
import com.example.engine.GameTermination
import com.example.engine.MoveFlag
import com.example.engine.PieceColor
import com.example.engine.PieceType
import com.example.engine.Square
import com.example.engine.StockfishUciAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun perft_initialPosition_matchesStandardFideCounts() {
        val state = ChessEngine.createInitialState()
        assertEquals(20L, ChessEngine.perft(state, 1))
        assertEquals(400L, ChessEngine.perft(state, 2))
        assertEquals(8902L, ChessEngine.perft(state, 3))
    }

    @Test
    fun perft_kiwipetePosition_validatesCastlingEnPassantAndPromotions() {
        val kiwipete = FenPgnCodec.parseFen(
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
        )
        assertEquals(48L, ChessEngine.perft(kiwipete, 1))
        assertEquals(2039L, ChessEngine.perft(kiwipete, 2))
    }

    @Test
    fun specialRules_castlingEnPassantPromotionAndCheckmate_workAccurately() {
        // 1. Fool's Mate Checkmate in 2 moves
        var state = ChessEngine.createInitialState()
        for (san in listOf("f3", "e5", "g4", "Qh4")) {
            val mv = FenPgnCodec.parseSanMove(state, san)
            assertNotNull("Expected legal move $san", mv)
            state = ChessEngine.makeMove(state, mv!!)
        }
        assertTrue(state.isInCheck)
        assertEquals(GameTermination.CHECKMATE, state.termination)
        assertEquals(PieceColor.BLACK, state.winner)
        assertEquals("Qh4#", state.moveHistory.last().san)

        // 2. En Passant capture
        var epState = FenPgnCodec.parseFen("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3")
        val epMove = FenPgnCodec.parseUciMove(epState, "e5f6")
        assertNotNull(epMove)
        assertEquals(MoveFlag.EN_PASSANT, epMove!!.flag)
        epState = ChessEngine.makeMove(epState, epMove)
        assertNullPieceAt(epState, Square.fromAlgebraic("f5")!!)
        assertEquals(PieceType.PAWN, epState.pieceAt(Square.fromAlgebraic("f6")!!)?.type)

        // 3. Stalemate detection
        val preStalemate = FenPgnCodec.parseFen("7k/5K2/8/8/8/8/8/6Q1 w - - 0 1")
        val qg6 = FenPgnCodec.parseUciMove(preStalemate, "g1g6")!!
        val stalemateState = ChessEngine.makeMove(preStalemate, qg6)
        assertFalse(stalemateState.isInCheck)
        assertEquals(GameTermination.STALEMATE, stalemateState.termination)

        // 4. Insufficient material detection (K+B vs K)
        val insuffState = FenPgnCodec.parseFen("4k3/8/8/8/8/8/8/4KB2 w - - 0 1")
        assertEquals(GameTermination.INSUFFICIENT_MATERIAL, insuffState.termination)
    }

    @Test
    fun allBundledPuzzlesAndAcademyLessons_haveStrictlyLegalSolutions() {
        for (puzzle in ChessPuzzleCatalog.allPuzzles) {
            var state = FenPgnCodec.parseFen(puzzle.initialFen)
            assertTrue("Puzzle ${puzzle.id} must have at least 1 solution move", puzzle.solutionMovesUci.isNotEmpty())
            for (uci in puzzle.solutionMovesUci) {
                val mv = FenPgnCodec.parseUciMove(state, uci)
                assertNotNull("Puzzle ${puzzle.id} move $uci must be strictly legal in FEN ${FenPgnCodec.toFen(state)}", mv)
                state = ChessEngine.makeMove(state, mv!!)
            }
        }

        for (chapter in ChessAcademyCatalog.chapters) {
            for (lesson in chapter.lessons) {
                val state = FenPgnCodec.parseFen(lesson.initialFen)
                val exMove = FenPgnCodec.parseUciMove(state, lesson.exerciseSolutionUci)
                assertNotNull("Lesson ${lesson.id} exercise solution ${lesson.exerciseSolutionUci} must be legal", exMove)
            }
        }
    }

    @Test
    fun aiAllSixDifficulties_produceStrictlyLegalMovesAndAnalysisWorks() {
        val state = ChessEngine.createInitialState()
        for (diff in AiDifficulty.entries) {
            val aiMove = ChessAiEngine.findBestMove(state, diff, timeLimitMs = 120L)
            assertNotNull("AI ${diff.name} must return a move", aiMove)
            assertTrue(ChessEngine.generateLegalMoves(state).any { it.from == aiMove!!.from && it.to == aiMove.to })
        }

        // Verify post-game analysis report generation
        var game = ChessEngine.createInitialState()
        for (uci in listOf("e2e4", "e7e5", "g1f3", "b8c6")) {
            val mv = FenPgnCodec.parseUciMove(game, uci)!!
            game = ChessEngine.makeMove(game, mv)
        }
        val report = StockfishUciAnalyzer.analyzeGame(game.moveHistory, perMoveTimeMs = 40L)
        assertEquals(4, report.analyzedPlies.size)
        assertEquals(5, report.evaluationCurveWhiteCp.size)
    }

    @Test
    fun authoritativeServerReferee_enforcesTurnOrderPlySequenceAndMoveLegality() {
        val (room, whiteToken) = AuthoritativeServerReferee.createRoom("Alice", 600_000L, 0L)
        val joinRes = AuthoritativeServerReferee.joinRoom(room.roomCode, "Bob")
        assertTrue(joinRes.isSuccess)
        val blackToken = joinRes.getOrThrow().second

        // Reject Black moving first
        val outOfTurn = AuthoritativeServerReferee.validateAndCommitMove(room.roomCode, blackToken, 0, "e7e5")
        assertTrue(outOfTurn.isFailure)

        // Reject illegal White move (e2e5)
        val illegalWhite = AuthoritativeServerReferee.validateAndCommitMove(room.roomCode, whiteToken, 0, "e2e5")
        assertTrue(illegalWhite.isFailure)

        // Accept legal White move (e2e4)
        val legalWhite = AuthoritativeServerReferee.validateAndCommitMove(room.roomCode, whiteToken, 0, "e2e4")
        assertTrue(legalWhite.isSuccess)

        // Reject duplicate ply 0 submission (race condition protection)
        val duplicatePly = AuthoritativeServerReferee.validateAndCommitMove(room.roomCode, blackToken, 0, "e7e5")
        assertTrue(duplicatePly.isFailure)

        // Accept legal Black reply at ply 1
        val legalBlack = AuthoritativeServerReferee.validateAndCommitMove(room.roomCode, blackToken, 1, "e7e5")
        assertTrue(legalBlack.isSuccess)
    }

    private fun assertNullPieceAt(state: com.example.engine.BoardState, sq: Square) {
        assertEquals(null, state.pieceAt(sq))
    }
}
