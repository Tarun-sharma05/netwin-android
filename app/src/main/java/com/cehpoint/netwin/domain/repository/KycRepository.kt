package com.cehpoint.netwin.domain.repository

import android.net.Uri
import com.cehpoint.netwin.data.model.KycDocument
import com.cehpoint.netwin.data.model.KycStatus
import kotlinx.coroutines.flow.Flow

interface KycRepository {
    // Uploads an image to Firebase Storage and returns the download URL
    suspend fun uploadKycImage(userId: String, imageType: KycImageType, imageUri: Uri): Result<String>

    // Submits or updates the KYC document for a user
    suspend fun submitKycDocument(kycDocument: KycDocument): Result<Unit>

    // Fetches the KYC document/status for a user
    suspend fun getKycDocument(userId: String): Result<KycDocument?>

    // Listen for real-time KYC status updates
    fun observeKycDocument(userId: String): Flow<KycDocument?>

    suspend fun updateKyc(kycDocument: KycDocument): Result<Unit>
    suspend fun updateKycStatus(id: String, status: KycStatus, rejectionReason: String? = null): Result<Unit>
    suspend fun deleteKycDocument(id: String): Result<Unit>
}

// Enum to distinguish image types for upload
enum class KycImageType {
    FRONT, BACK, SELFIE
} 