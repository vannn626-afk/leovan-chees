package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = ChessCloudRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveOrUpdateUserProfile(
                playerName = "Grandmaster Alice",
                preferredTheme = "CLASSIC_WOOD",
                preferredPieceColor = "WHITE",
                totalGamesPlayed = 10L,
                totalWins = 7L,
                totalLosses = 2L,
                totalDraws = 1L,
                puzzlesSolvedCount = 15L,
                currentPuzzleStreak = 5L,
                bestPuzzleStreak = 8L
            )
        }
        assertTrue("Profile save should succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(aliceUid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals("Grandmaster Alice", profile?.playerName)
        assertEquals(7L, profile?.totalWins)
    }

    @Test
    fun observeUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = ChessCloudRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveOrUpdateUserProfile(
                playerName = "Alice",
                preferredTheme = "CLASSIC_WOOD",
                preferredPieceColor = "WHITE",
                totalGamesPlayed = 1L,
                totalWins = 1L,
                totalLosses = 0L,
                totalDraws = 0L,
                puzzlesSolvedCount = 0L,
                currentPuzzleStreak = 0L,
                bestPuzzleStreak = 0L
            ).getOrThrow()
        }

        // Switch to Bob and try to read Alice's profile
        signInTestUser(BOB_EMAIL)
        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserProfile(aliceUid).first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull("Cross-user profile read must fail", caughtException)
        val firestoreException = checkNotNull(caughtException!!.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun observeMatchHistory_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = ChessCloudRepository(firestore)
        val matchIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMatchRecord(
                gameMode = "VS_COMPUTER",
                whiteName = "Alice",
                blackName = "Computer",
                playerColor = "WHITE",
                resultBanner = "Checkmate — White Wins!",
                outcomeForPlayer = "WIN",
                pgnText = "1. e4 e5 1-0",
                finalFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                totalMoves = 1L
            )
        }
        assertTrue(matchIdResult.isSuccess)

        // Sign out and attempt to observe match history
        auth.signOut()
        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeMatchHistory(aliceUid).first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull("Unauthenticated match_history list must fail", caughtException)
        val firestoreException = checkNotNull(caughtException!!.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun chessRoom_createJoinAndAuthoritativeMoves_succeedsForParticipants() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = ChessCloudRepository(firestore)
        val uniqueRoomCode = "RM" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

        val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createChessRoom(uniqueRoomCode, "Alice").getOrThrow()
        }
        assertEquals(uniqueRoomCode, roomId)

        // Bob joins the waiting room
        signInTestUser(BOB_EMAIL)
        val joinedRoom = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.joinRoomByCode(uniqueRoomCode, "Bob").getOrThrow()
        }
        assertEquals("active", joinedRoom.status)

        // Alice plays e2e4
        signInTestUser(ALICE_EMAIL)
        val afterWhiteMove = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitAuthoritativeRoomMove(uniqueRoomCode, "e2e4").getOrThrow()
        }
        assertEquals(1L, afterWhiteMove.plyCount)
        assertEquals("BLACK", afterWhiteMove.turn)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@leovanchess.test"
        const val BOB_EMAIL = "bob@leovanchess.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
