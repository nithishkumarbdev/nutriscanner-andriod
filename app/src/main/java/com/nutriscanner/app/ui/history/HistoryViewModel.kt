package com.nutriscanner.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscanner.app.auth.AuthManager
import com.nutriscanner.app.data.ScanRecord
import com.nutriscanner.app.data.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryState(
    val scans: List<ScanRecord> = emptyList(),
    val isLoading: Boolean = true,
)

class HistoryViewModel(
    private val repository: ScanRepository = ScanRepository(),
    private val authManager: AuthManager = AuthManager(),
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = authManager.ensureSignedIn()
            repository.observeScans(uid).collect { scans ->
                _state.value = HistoryState(scans = scans, isLoading = false)
            }
        }
    }
}
