package com.nutriscanner.app.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Anonymous auth is enough here: scan history is per-device, there's no
 * account system, and we just need a stable uid to scope Firestore reads
 * and writes to the person who took the scan. Signing in anonymously also
 * means the first launch has zero friction, which matters for a demo app.
 */
class AuthManager(private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    suspend fun ensureSignedIn(): String {
        firebaseAuth.currentUser?.let { return it.uid }
        val result = firebaseAuth.signInAnonymously().await()
        return result.user?.uid
            ?: error("Anonymous sign-in succeeded but returned no user; check Firebase Auth console config.")
    }
}
