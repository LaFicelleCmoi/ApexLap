package com.apexlap.ui.constructor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.apexlap.data.encyclopedia.EncyclopediaRepository
import com.apexlap.domain.encyclopedia.Titles
import com.apexlap.domain.encyclopedia.constructorBio
import com.apexlap.domain.model.ConstructorProfile
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.ui.components.OfflineFirstLoader
import com.apexlap.ui.navigation.ConstructorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ConstructorUiState(
    val season: Int,
    val profile: ConstructorProfile? = null,
    val standing: ConstructorStanding? = null,
    val drivers: List<DriverStanding> = emptyList(),
    val titles: Titles = Titles.NONE,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val bio: String? = null,
)

@HiltViewModel
class ConstructorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: F1Repository,
    encyclopedia: EncyclopediaRepository,
    clock: Clock,
) : ViewModel() {

    private val constructorId = savedStateHandle.toRoute<ConstructorRoute>().constructorId
    private val season = clock.currentSeason()

    private val profile = OfflineFirstLoader(
        scope = viewModelScope,
        source = repository.observeConstructorProfile(constructorId),
        refreshAction = { force -> repository.refreshConstructorProfile(constructorId, force) },
    )

    val uiState: StateFlow<ConstructorUiState> = combine(
        profile.state,
        repository.observeConstructorStandings(season),
        repository.observeDriverStandings(season),
        flow { emit(encyclopedia.champions().constructorTitles(constructorId)) },
    ) { profile, teams, drivers, titles ->
        val standing = teams.firstOrNull { it.constructor.id == constructorId }
        ConstructorUiState(
            season = season,
            profile = profile.data,
            standing = standing,
            bio = profile.data?.let { constructorBio(it, titles, standing, season) },
            drivers = drivers.filter { it.constructor?.id == constructorId },
            titles = titles,
            isRefreshing = profile.isRefreshing,
            error = profile.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConstructorUiState(season, isRefreshing = true))

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) = profile.refresh(force)
}
