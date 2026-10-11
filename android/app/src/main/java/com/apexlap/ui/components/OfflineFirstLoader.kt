package com.apexlap.ui.components

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class ScreenState<T>(
    /** Null until the local cache emitted for the first time. */
    val data: T? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

/**
 * Glue for the offline-first pattern in ViewModels: exposes the cached stream immediately and
 * reports the status of network refreshes alongside it.
 */
class OfflineFirstLoader<T>(
    private val scope: CoroutineScope,
    source: Flow<T>,
    /** Receives `force = true` for explicit user refreshes, so repositories may bypass their caches. */
    private val refreshAction: suspend (force: Boolean) -> Unit,
) {
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ScreenState<T>> = combine(
        source.map<T, T?> { it }.onStart { emit(null) },
        refreshing,
        error,
    ) { data, isRefreshing, message -> ScreenState(data, isRefreshing, message) }
        .stateIn(scope, SharingStarted.Eagerly, ScreenState(isRefreshing = true))

    fun refresh(force: Boolean = false) {
        if (refreshing.value) return
        scope.launch {
            refreshing.value = true
            error.value = null
            try {
                refreshAction(force)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = e.toUserMessage()
            } finally {
                refreshing.value = false
            }
        }
    }
}

fun Throwable.toUserMessage(): String = when (this) {
    is IOException -> "Hors ligne — affichage des données en cache"
    is HttpException -> "Serveur indisponible (HTTP ${code()})"
    else -> "Impossible de charger les données"
}
