package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreRulesTest : FirestoreEmulatorTestBase() {

  @Test
  fun unauthenticatedUser_cannotReadWorkspaces() = runTest {
    auth.signOut()
    val testUid = "unauth_" + UUID.randomUUID().toString()
    try {
      withTimeout(5000L) {
        firestore.collection("users").document(testUid).collection("workspaces").get().await()
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
    } catch (e: Throwable) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected Firestore exception", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  @Test
  fun authenticatedUser_canCreateAndReadOwnWorkspace() = runTest {
    val dynamicEmail = "alice_" + UUID.randomUUID().toString().take(8) + "@test.local"
    val aliceUid = signInTestUser(dynamicEmail)
    val wsId = "ws_" + UUID.randomUUID().toString().take(8)
    val wsDoc = firestore.collection("users").document(aliceUid).collection("workspaces").document(wsId)

    val payload = mapOf(
      "userId" to aliceUid,
      "name" to "Alice Auto Shop",
      "businessType" to "AUTOMOTIVE_GARAGE",
      "currencySymbol" to "$",
      "currencyCode" to "USD"
    )

    withTimeout(8000L) {
      wsDoc.set(payload).await()
      val snapshot = wsDoc.get().await()
      assertTrue(snapshot.exists())
      assertEquals("Alice Auto Shop", snapshot.getString("name"))
    }
  }

  @Test
  fun crossUser_cannotAccessAliceData() = runTest {
    val bobEmail = "bob_" + UUID.randomUUID().toString().take(8) + "@test.local"
    val bobUid = signInTestUser(bobEmail)
    val foreignUserUid = "foreign_user_uid_" + UUID.randomUUID().toString().take(8)
    val foreignDoc = firestore.collection("users").document(foreignUserUid).collection("workspaces").document("ws_foreign")

    try {
      withTimeout(8000L) {
        foreignDoc.get().await()
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED for foreign user document access")
    } catch (e: Throwable) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected PERMISSION_DENIED for cross-user access", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }
}
