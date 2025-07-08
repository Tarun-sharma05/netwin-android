package com.cehpoint.netwin.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cehpoint.netwin.presentation.screens.Alert
import com.cehpoint.netwin.presentation.screens.AlertType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor() : ViewModel() {
    private val _alertsState = MutableStateFlow(AlertsState())
    val alertsState: StateFlow<AlertsState> = _alertsState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAlerts()
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

//                // TODO: Replace with actual API call
//                // Simulated data for now
//                val alerts = listOf(
//                    Alert(
//                        id = "1",
//                        type = AlertType.TOURNAMENT,
//                        title = "New Tournament Available",
//                        message = "Join the PUBG Mobile Squad Showdown starting in 2 hours!",
//                        timestamp = System.currentTimeMillis() - 1000 * 60 * 30, // 30 minutes ago
//                        isRead = false
//                    ),
//                    Alert(
//                        id = "2",
//                        type = AlertType.WIN,
//                        title = "Tournament Victory!",
//                        message = "Congratulations! Your team won the Battle Royale Championship!",
//                        timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2, // 2 hours ago
//                        isRead = true
//                    ),
//                    Alert(
//                        id = "3",
//                        type = AlertType.PAYMENT,
//                        title = "Payment Successful",
//                        message = "Your tournament entry fee of ₹500 has been processed successfully.",
//                        timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5, // 5 hours ago
//                        isRead = true
//                    ),
//                    Alert(
//                        id = "4",
//                        type = AlertType.SYSTEM,
//                        title = "System Maintenance",
//                        message = "Scheduled maintenance on June 15th, 2024 from 2 AM to 4 AM IST.",
//                        timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24, // 1 day ago
//                        isRead = false
//                    )
//                )
//
//                _alertsState.value = AlertsState(alerts = alerts)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load alerts"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val currentAlerts = _alertsState.value.alerts
                val updatedAlerts = currentAlerts.map { it.copy(isRead = true) }
                _alertsState.value = _alertsState.value.copy(alerts = updatedAlerts)
                
                // TODO: Update read status in the backend
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to mark alerts as read"
            }
        }
    }

    fun markAlertAsRead(alertId: String) {
        viewModelScope.launch {
            try {
                val currentAlerts = _alertsState.value.alerts
                val updatedAlerts = currentAlerts.map { 
                    if (it.id == alertId) it.copy(isRead = true) else it 
                }
                _alertsState.value = _alertsState.value.copy(alerts = updatedAlerts)
                
                // TODO: Update read status in the backend
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to mark alert as read"
            }
        }
    }

    fun refreshAlerts() {
        loadAlerts()
    }
}

data class AlertsState(
    val alerts: List<Alert> = emptyList()
) 