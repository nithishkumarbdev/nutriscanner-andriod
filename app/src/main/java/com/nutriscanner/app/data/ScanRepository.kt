package com.nutriscanner.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Scan history lives at users/{uid}/scans/{scanId}. Scoping every document
 * under the signed-in uid keeps the Firestore security rules simple (a user
 * can only read/write their own subtree) and means we never need a
 * cross-user query.
 *
 * Firestore's local cache gives us offline persistence for free: writes
 * queue locally and sync when connectivity returns, and snapshot listeners
 * keep firing from cache while offline. That's existing SDK behavior we're
 * relying on, not something built here.
 */
class ScanRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {

    private fun scansCollection(uid: String) =
        firestore.collection("users").document(uid).collection("scans")

    suspend fun saveScan(uid: String, record: ScanRecord): String {
        val docRef = scansCollection(uid).document()
        val toSave = record.copy(id = docRef.id)
        docRef.set(toSave).await()
        return docRef.id
    }

    /** Uploads the label photo and returns its download URL. Callers pass this into [saveScan]'s record. */
    suspend fun uploadLabelPhoto(uid: String, imageBytes: ByteArray): String {
        val ref = storage.reference.child("users/$uid/scans/${UUID.randomUUID()}.jpg")
        ref.putBytes(imageBytes).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Live-updating scan history, newest first. Emits a new list on every
     * Firestore snapshot, including the initial cached snapshot when
     * offline, so the history screen updates immediately either way.
     */
    fun observeScans(uid: String): Flow<List<ScanRecord>> = callbackFlow {
        val registration: ListenerRegistration = scansCollection(uid)
            .orderBy("scannedAtEpochMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { it.toObject(ScanRecord::class.java) }
                    ?: emptyList()
                trySend(records)
            }
        awaitClose { registration.remove() }
    }
}
