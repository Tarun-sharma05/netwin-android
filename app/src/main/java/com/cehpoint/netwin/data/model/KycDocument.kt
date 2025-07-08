package com.cehpoint.netwin.data.model

import com.google.firebase.Timestamp

data class KycDocument(
    val id: String = "",
    val userId: String = "",
    val documentType: DocumentType = DocumentType.AADHAR,
    val documentNumber: String = "",
    val frontImageUrl: String = "",
    val backImageUrl: String = "",
    val selfieUrl: String = "",
    val status: KycStatus = KycStatus.PENDING,
    val rejectionReason: String? = null,
    val verifiedAt: Timestamp? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Timestamp? = null,
    val reviewedBy: String? = null
)

enum class DocumentType {
    AADHAR,
    PAN,
    PASSPORT,
    DRIVING_LICENSE
}

enum class KycStatus {
    PENDING,
    VERIFIED,
    REJECTED
} 