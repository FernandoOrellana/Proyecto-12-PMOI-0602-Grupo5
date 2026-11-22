package com.uth.proyectofinal.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.Page
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class MessageViewModel : ViewModel() {
    private val channel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = channel.receiveAsFlow()

    protected fun say(text: String) {
        channel.trySend(text)
    }
}

data class PagedState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val page: Int = 0,
    val hasMore: Boolean = true,
    val refreshing: Boolean = false,
)

abstract class PagedViewModel<T> : MessageViewModel() {
    private val _state = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()
    private var job: Job? = null

    protected abstract suspend fun fetch(page: Int): ApiResult<Page<List<T>>>

    fun refresh() = recargar(mostrarIndicador = false)

    fun pullRefresh() = recargar(mostrarIndicador = true)

    private fun recargar(mostrarIndicador: Boolean) {
        job?.cancel()
        if (_state.value.items.isEmpty()) {
            _state.value = PagedState()
            loadMore()
            return
        }
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, refreshing = mostrarIndicador, error = null) }
            when (val result = fetch(1)) {
                is ApiResult.Success -> {
                    val meta = result.data.meta
                    _state.value = PagedState(
                        items = result.data.data,
                        page = 1,
                        hasMore = meta != null && meta.page < meta.totalPages,
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, refreshing = false, error = result.message) }
            }
        }
    }

    fun reload() {
        job?.cancel()
        _state.value = PagedState()
        loadMore()
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || !current.hasMore) return
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = fetch(current.page + 1)) {
                is ApiResult.Success -> _state.update {
                    val meta = result.data.meta
                    it.copy(
                        items = it.items + result.data.data,
                        loading = false,
                        page = current.page + 1,
                        hasMore = meta != null && meta.page < meta.totalPages,
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
