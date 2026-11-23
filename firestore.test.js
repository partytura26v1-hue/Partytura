const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_musician_1";
const BOB_UID = "bob_drummer_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSongPayload(uid, created) {
  return {
    userId: uid,
    title: "Autobiografia",
    artist: "Perfect",
    musicalKey: "C",
    bpm: 112,
    lyrics: "[C]Miałem dziesięć lat [Am]gdy usłyszał o nim świat",
    primaryInstrument: "Gitara",
    bandName: "Nasz Zespół",
    transposeSemitones: 0,
    favorite: false,
    instruments: ["Gitara"],
    createdAt: created,
    updatedAt: created,
  };
}

function validPartPayload(uid, songId, created) {
  return {
    userId: uid,
    songId: songId,
    instrument: "Gitara",
    notationType: "STAFF",
    clef: "TREBLE",
    tuning: "Standard E A D G B E",
    capo: 0,
    staffNotes: "C4 D4 E4 G4 | A4 G4 E4 -",
    tablature: "e|---0---3---|\nB|---1---0---|",
    chordsProgression: "| C | Am | F | G |",
    performanceNotes: "Intro delikatnie palcami",
    createdAt: created,
    updatedAt: created,
  };
}

function validSetlistPayload(uid, created) {
  return {
    userId: uid,
    name: "Próba Piątkowa",
    venueOrNotes: "Sala prób nr 2, 19:00",
    songIds: ["song_1"],
    createdAt: created,
    updatedAt: created,
  };
}

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

test("Unauthenticated user cannot read or write songs, parts, or setlists", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("songs").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("songs").doc("song_1").set(
      validSongPayload(ALICE_UID, now)
    )
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("setlists").get()
  );
});

test("Owner can create, list, update, and delete songs matching Kotlin schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const songsCol = aliceDb.collection("users").doc(ALICE_UID).collection("songs");

  await assertSucceeds(songsCol.doc("song_1").set(validSongPayload(ALICE_UID, created)));
  await assertSucceeds(songsCol.get());

  await assertSucceeds(
    songsCol.doc("song_1").update({
      transposeSemitones: 2,
      favorite: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject extra @DocumentId 'id' field in payload
  await assertFails(
    songsCol.doc("song_2").set({
      id: "song_2",
      ...validSongPayload(ALICE_UID, created),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    songsCol.doc("song_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject out-of-range BPM or future timestamp
  await assertFails(
    songsCol.doc("song_1").update({
      bpm: 999,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    songsCol.doc("song_1").update({
      title: "Future Edit",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(songsCol.doc("song_1").delete());
});

test("Owner can create song + instrument part in batch and update part safely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const songRef = aliceDb.collection("users").doc(ALICE_UID).collection("songs").doc("song_batch");
  const partRef = songRef.collection("parts").doc("part_guitar");

  // Orphaned part without parent song must fail
  await assertFails(partRef.set(validPartPayload(ALICE_UID, "song_batch", created)));

  // Batch create song + part succeeds (existsAfter)
  const batch = aliceDb.batch();
  batch.set(songRef, validSongPayload(ALICE_UID, created));
  batch.set(partRef, validPartPayload(ALICE_UID, "song_batch", created));
  await assertSucceeds(batch.commit());

  await assertSucceeds(songRef.collection("parts").get());

  await assertSucceeds(
    partRef.update({
      staffNotes: "D4 E4 F#4 A4 | B4 A4 F#4 -",
      capo: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid notationType or ghost field must fail
  await assertFails(
    partRef.update({
      notationType: "INVALID",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    partRef.update({
      isVerified: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's songs, parts, and setlists", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).collection("songs").doc("alice_song").set(
      validSongPayload(ALICE_UID, created)
    );
    await db.collection("users").doc(ALICE_UID).collection("songs").doc("alice_song")
      .collection("parts").doc("alice_part").set(
        validPartPayload(ALICE_UID, "alice_song", created)
      );
    await db.collection("users").doc(ALICE_UID).collection("setlists").doc("alice_set").set(
      validSetlistPayload(ALICE_UID, created)
    );
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("songs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("songs").doc("alice_song").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("songs").doc("alice_song").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("songs").doc("alice_song")
      .collection("parts").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("setlists").doc("alice_set").get()
  );
});

test("Owner can create and update setlists", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const setlistsCol = aliceDb.collection("users").doc(ALICE_UID).collection("setlists");

  await assertSucceeds(setlistsCol.doc("set_1").set(validSetlistPayload(ALICE_UID, created)));
  await assertSucceeds(setlistsCol.get());
  await assertSucceeds(
    setlistsCol.doc("set_1").update({
      songIds: ["song_1", "song_2"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
