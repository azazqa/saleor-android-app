package com.bdf.saleor.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdf.saleor.core.data.AccountRepository
import com.bdf.saleor.core.model.Money
import com.bdf.saleor.core.model.PointsHistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PointsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val balance: Money? = null,
    val entries: List<PointsHistoryEntry> = emptyList(),
    val endCursor: String? = null,
    val hasNextPage: Boolean = false,
    val totalCount: Int = 0,
)

@HiltViewModel
class PointsViewModel @Inject constructor(
    private val repository: AccountRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PointsUiState())
    val uiState: StateFlow<PointsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getPoints(after = null) }
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            balance = page.balance,
                            entries = page.entries,
                            endCursor = page.endCursor,
                            hasNextPage = page.hasNextPage,
                            totalCount = page.totalCount,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Unknown error")
                    }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasNextPage) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            runCatching { repository.getPoints(after = state.endCursor) }
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            entries = it.entries + page.entries,
                            endCursor = page.endCursor,
                            hasNextPage = page.hasNextPage,
                            totalCount = page.totalCount,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }
}
