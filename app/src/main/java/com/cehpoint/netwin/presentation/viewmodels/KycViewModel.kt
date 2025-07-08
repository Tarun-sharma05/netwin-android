package com.cehpoint.netwin.presentation.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.data.model.KycDocument
import com.cehpoint.netwin.domain.repository.KycImageType
import com.cehpoint.netwin.domain.repository.KycRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KycViewModel @Inject constructor(
    private val kycRepository: KycRepository
) : ViewModel() {

    data class KycUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val kycDocument: KycDocument? = null,
        val frontImageUrl: String = "",
        val backImageUrl: String = "",
        val selfieUrl: String = "",
        val uploadProgress: Float = 0f,
        val status: String? = null
    )

    private val _uiState = MutableStateFlow(KycUiState())
    val uiState: StateFlow<KycUiState> = _uiState.asStateFlow()

    fun observeKyc(userId: String) {
        viewModelScope.launch {
            kycRepository.observeKycDocument(userId).collectLatest { kycDoc ->
                _uiState.value = _uiState.value.copy(
                    kycDocument = kycDoc,
                    status = kycDoc?.status?.name ?: ""
                )
                Log.d("KycViewModel", "KYC Document updated: ${kycDoc?.status?.name}")
            }
        }
    }

    fun uploadImage(userId: String, imageType: KycImageType, imageUri: Uri) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = kycRepository.uploadKycImage(userId, imageType, imageUri)
            result.onSuccess { url ->
                when (imageType) {
                    KycImageType.FRONT -> _uiState.value = _uiState.value.copy(frontImageUrl = url)
                    KycImageType.BACK -> _uiState.value = _uiState.value.copy(backImageUrl = url)
                    KycImageType.SELFIE -> _uiState.value = _uiState.value.copy(selfieUrl = url)
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun submitKyc(kycDocument: KycDocument) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = kycRepository.submitKycDocument(kycDocument)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetImages() {
        _uiState.value = _uiState.value.copy(
            frontImageUrl = "",
            backImageUrl = "",
            selfieUrl = ""
        )
    }
} 