package com.cehpoint.netwin.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<State, Event> : ViewModel() {
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    protected open fun setState(newState: State) {
        viewModelScope.launch {
            _state.emit(newState)
        }
    }

    protected open fun setLoading(loading: Boolean) {
        viewModelScope.launch {
            _isLoading.emit(loading)
        }
    }

    protected open fun setError(error: String?) {
        viewModelScope.launch {
            _error.emit(error)
        }
    }

    abstract fun handleEvent(event: Event)
} 