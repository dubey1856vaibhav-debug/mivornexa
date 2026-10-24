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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read workspaces", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workspaces").get());
});

test("Authenticated user: can create their own valid workspace", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workspaces").doc("ws_1").set({
      userId: ALICE_UID,
      name: "Alice Workshop",
      businessType: "RETAIL_ECOMMERCE",
      currencySymbol: "$",
      currencyCode: "USD",
      createdAt: null,
      updatedAt: null
    })
  );
});

test("Authenticated user: cannot read or write another user's workspace", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("workspaces").doc("ws_alice").set({
      userId: ALICE_UID,
      name: "Alice Private Workspace",
      businessType: "RETAIL_ECOMMERCE"
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workspaces").doc("ws_alice").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workspaces").doc("ws_alice").set({
    userId: BOB_UID,
    name: "Hacked Workspace",
    businessType: "RETAIL_ECOMMERCE"
  }));
});

test("Authenticated user: cannot create customer with invalid or spoofed userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set({
      userId: BOB_UID, // spoofed
      workspaceId: "ws_1",
      name: "John Doe"
    })
  );
});

test("Authenticated user: can create valid customer under their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set({
      userId: ALICE_UID,
      workspaceId: "ws_1",
      name: "John Doe",
      phone: "+15551234567",
      email: "john@example.com",
      createdAt: null,
      updatedAt: null
    })
  );
});
