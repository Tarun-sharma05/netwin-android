package com.cehpoint.netwin.data.repository

import android.net.Uri
import com.cehpoint.netwin.data.model.KycDocument
import com.cehpoint.netwin.data.model.KycStatus
import com.cehpoint.netwin.data.model.DocumentType
import com.cehpoint.netwin.domain.repository.KycImageType
import com.cehpoint.netwin.domain.repository.KycRepository
import com.cehpoint.netwin.data.remote.FirebaseManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class KycRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : KycRepository {

    private val kycCollection = firebaseManager.firestore.collection("kyc_documents")
    private val storage = firebaseManager.storage.reference

    override suspend fun updateKyc(kycDocument: KycDocument): Result<Unit> = try {
        kycCollection.document(kycDocument.id)
            .set(kycDocument)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateKycStatus(id: String, status: KycStatus, rejectionReason: String?): Result<Unit> = try {
        val updates = mutableMapOf<String, Any>(
            "status" to status
        )
        
        if (status == KycStatus.VERIFIED) {
            updates["verifiedAt"] = com.google.firebase.Timestamp.now()
        } else if (status == KycStatus.REJECTED) {
            updates["rejectionReason"] = rejectionReason ?: "No reason provided"
        }

        kycCollection.document(id)
            .update(updates)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteKycDocument(id: String): Result<Unit> {
        return try {
            // First get the document to get the file paths
            val document = firebaseManager.getDocument(
                "kyc_documents",
                id,
                KycDocument::class.java
            ).getOrNull()
            // Delete the files from storage if URLs exist
            document?.frontImageUrl?.takeIf { it.isNotBlank() }?.let { fileUrl ->
                val filePath = fileUrl.substringAfter("kyc/")
                firebaseManager.deleteFile("kyc/$filePath")
            }
            document?.backImageUrl?.takeIf { it.isNotBlank() }?.let { fileUrl ->
                val filePath = fileUrl.substringAfter("kyc/")
                firebaseManager.deleteFile("kyc/$filePath")
            }
            document?.selfieUrl?.takeIf { it.isNotBlank() }?.let { fileUrl ->
                val filePath = fileUrl.substringAfter("kyc/")
                firebaseManager.deleteFile("kyc/$filePath")
            }
            // Delete the document from Firestore
            firebaseManager.deleteDocument("kyc_documents", id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadKycImage(userId: String, imageType: KycImageType, imageUri: Uri): Result<String> {
        return try {
            val fileName = when (imageType) {
                KycImageType.FRONT -> "front_${System.currentTimeMillis()}"
                KycImageType.BACK -> "back_${System.currentTimeMillis()}"
                KycImageType.SELFIE -> "selfie_${System.currentTimeMillis()}"
            }
            val filePath = "kyc/$userId/$fileName"
            val uploadTask = storage.child(filePath).putFile(imageUri).await()
            val url = storage.child(filePath).downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitKycDocument(kycDocument: KycDocument): Result<Unit> = try {
        val docId = if (kycDocument.id.isNotEmpty()) kycDocument.id else UUID.randomUUID().toString()
        kycCollection.document(docId).set(kycDocument.copy(id = docId)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getKycDocument(userId: String): Result<KycDocument?> = try {
        val querySnapshot = kycCollection.whereEqualTo("userId", userId).limit(1).get().await()
        val kyc = querySnapshot.documents.firstOrNull()?.toObject(KycDocument::class.java)
        Result.success(kyc)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeKycDocument(userId: String): Flow<KycDocument?> = callbackFlow {
        val subscription = kycCollection
            .whereEqualTo("userId", userId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val kyc = snapshot?.documents?.firstOrNull()?.toObject(KycDocument::class.java)
                trySend(kyc)
            }
        awaitClose { subscription.remove() }
    }
} 