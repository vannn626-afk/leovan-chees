const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfilePayload(userId, now = new Date()) {
  return {
    userId,
    playerName: "Grandmaster Alice",
    preferredTheme: "CLASSIC_WOOD",
    preferredPieceColor: "WHITE",
    totalGamesPlayed: 5,
    totalWins: 3,
    totalLosses: 1,
    totalDraws: 1,
    puzzlesSolvedCount: 10,
    currentPuzzleStreak: 4,
    bestPuzzleStreak: 6,
    createdAt: now,
    updatedAt: now,
  };
}

function validMatchPayload(userId, now = new Date()) {
  return {
    userId,
    gameMode: "VS_COMPUTER",
    whiteName: "Alice",
    blackName: "Computer (Medium)",
    playerColor: "WHITE",
    resultBanner: "Checkmate — White Wins!",
    outcomeForPlayer: "WIN",
    pgnText: "1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0",
    finalFen: "r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4",
    totalMoves: 4,
    createdAt: now,
  };
}

function validRoomPayload(hostUid, now = new Date()) {
  return {
    roomCode: "LVN777",
    hostUid,
    hostName: "Alice",
    guestUid: "",
    guestName: "",
    status: "waiting",
    fen: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    turn: "WHITE",
    plyCount: 0,
    lastMoveUci: "",
    moveHistoryUci: "",
    resultBanner: "",
    winnerUid: "",
    drawOfferBy: "",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: rejected from reading or writing users, match_history, and chess_rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("match_history").get()
  );
  await assertFails(unauthDb.collection("chess_rooms").where("status", "==", "waiting").get());
});

test("UserProfile: owner can create, read, and update own profile; cross-user and shadow fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ playerName: "Hacked" })
  );

  // Shadow field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      playerName: "Alice Updated",
      isAdmin: true,
      updatedAt: now,
    })
  );

  // Valid update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      playerName: "Alice Updated",
      totalWins: 4,
      updatedAt: now,
    })
  );
});

test("CloudMatchRecord: owner can create and list own match_history; updates and cross-user access rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  const matchRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("match_history")
    .doc("match_1");

  await assertSucceeds(matchRef.set(validMatchPayload(ALICE_UID, now)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("match_history").get()
  );

  // Immutable match record cannot be updated
  await assertFails(matchRef.update({ outcomeForPlayer: "LOSS" }));

  // Cross-user list rejected
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("match_history").get()
  );
});

test("ChessRoom: host creates waiting room, guest joins, participants play moves, terminal state locks room", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  const roomRefAlice = aliceDb.collection("chess_rooms").doc("room_1");
  const roomRefBob = bobDb.collection("chess_rooms").doc("room_1");
  const roomRefCharlie = charlieDb.collection("chess_rooms").doc("room_1");

  await assertSucceeds(roomRefAlice.set(validRoomPayload(ALICE_UID, now)));

  // Waiting rooms queryable with status == 'waiting'
  await assertSucceeds(
    bobDb.collection("chess_rooms").where("status", "==", "waiting").get()
  );

  // Host cannot join their own room as guest
  await assertFails(
    roomRefAlice.update({
      guestUid: ALICE_UID,
      guestName: "Alice Clone",
      status: "active",
      updatedAt: now,
    })
  );

  // Bob joins room
  await assertSucceeds(
    roomRefBob.update({
      guestUid: BOB_UID,
      guestName: "Bob",
      status: "active",
      updatedAt: now,
    })
  );

  // Outsider Charlie cannot read or update active room
  await assertFails(roomRefCharlie.get());
  await assertFails(
    roomRefCharlie.update({
      fen: "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
      turn: "BLACK",
      plyCount: 1,
      lastMoveUci: "e2e4",
      moveHistoryUci: "e2e4",
      updatedAt: now,
    })
  );

  // Alice makes move and finishes game
  await assertSucceeds(
    roomRefAlice.update({
      status: "finished",
      fen: "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
      turn: "BLACK",
      plyCount: 1,
      lastMoveUci: "e2e4",
      moveHistoryUci: "e2e4",
      resultBanner: "White Wins by Resignation",
      winnerUid: ALICE_UID,
      updatedAt: now,
    })
  );

  // Terminal state locking: no further updates allowed once status == 'finished'
  await assertFails(
    roomRefBob.update({
      resultBanner: "Tampered Result",
      updatedAt: now,
    })
  );
});
