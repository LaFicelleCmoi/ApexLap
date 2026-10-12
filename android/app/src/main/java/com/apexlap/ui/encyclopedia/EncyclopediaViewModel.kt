package com.apexlap.ui.encyclopedia

import com.apexlap.i18n.Strings
import com.apexlap.i18n.I18n
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexlap.data.encyclopedia.EncyclopediaRepository
import com.apexlap.domain.encyclopedia.Champion
import com.apexlap.domain.encyclopedia.ChampionsRegistry
import com.apexlap.domain.encyclopedia.Glossary
import com.apexlap.domain.encyclopedia.GlossaryTerm
import com.apexlap.domain.encyclopedia.matchesSearch
import com.apexlap.domain.model.Circuit
import com.apexlap.domain.model.Constructor
import com.apexlap.domain.model.Driver
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.ui.components.OfflineFirstLoader
import com.apexlap.ui.components.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EncyclopediaTab(val label: (Strings) -> String) {
    SEASONS(Strings::tabSeasons),
    DRIVERS(Strings::tabDrivers),
    CONSTRUCTORS(Strings::tabTeams),
    CIRCUITS(Strings::tabCircuits),
    GLOSSARY(Strings::tabGlossary),
}

data class EncyclopediaUiState(
    val query: String = "",
    val championsOnly: Boolean = false,
    val currentSeason: Int,
    /** Leader of the season in progress (no champion yet). */
    val currentLeader: DriverStanding? = null,
    val registry: ChampionsRegistry? = null,
    val drivers: ScreenState<List<Driver>> = ScreenState(),
    val constructors: ScreenState<List<Constructor>> = ScreenState(),
    val circuits: ScreenState<List<Circuit>> = ScreenState(),
    val glossary: List<GlossaryTerm> = Glossary.terms,
) {
    val champions: List<Champion> get() = registry?.champions.orEmpty()
}

@HiltViewModel
class EncyclopediaViewModel @Inject constructor(
    private val encyclopedia: EncyclopediaRepository,
    f1Repository: F1Repository,
    clock: Clock,
) : ViewModel() {

    private val season = clock.currentSeason()
    private val query = MutableStateFlow("")
    private val championsOnly = MutableStateFlow(false)
    private val registry = MutableStateFlow<ChampionsRegistry?>(null)

    private val drivers = OfflineFirstLoader(viewModelScope, encyclopedia.observeDrivers()) { encyclopedia.refreshDrivers(it) }
    private val constructors = OfflineFirstLoader(viewModelScope, encyclopedia.observeConstructors()) { encyclopedia.refreshConstructors(it) }
    private val circuits = OfflineFirstLoader(viewModelScope, encyclopedia.observeCircuits()) { encyclopedia.refreshCircuits(it) }
    private val loaded = mutableSetOf<EncyclopediaTab>()

    private val lists = combine(drivers.state, constructors.state, circuits.state) { d, c, k -> Triple(d, c, k) }
    private val filters = combine(query, championsOnly, registry) { q, champs, reg -> Triple(q, champs, reg) }

    val uiState: StateFlow<EncyclopediaUiState> = combine(
        lists,
        filters,
        f1Repository.observeDriverStandings(season),
        I18n.language,
    ) { (d, c, k), (q, champs, reg), standings, language ->
        EncyclopediaUiState(
            query = q,
            championsOnly = champs,
            currentSeason = season,
            currentLeader = standings.firstOrNull(),
            registry = reg,
            drivers = d.copy(data = d.data?.filter { driver ->
                (!champs || reg?.driverTitles(driver.id)?.count?.let { it > 0 } == true) &&
                    (driver.fullName.matchesSearch(q) || driver.nationality.matchesSearch(q))
            }?.let { list -> if (champs && reg != null) list.sortedByDescending { reg.driverTitles(it.id).count } else list }),
            constructors = c.copy(data = c.data?.filter { team ->
                (!champs || reg?.constructorTitles(team.id)?.count?.let { it > 0 } == true) &&
                    (team.name.matchesSearch(q) || team.nationality.matchesSearch(q))
            }?.let { list -> if (champs && reg != null) list.sortedByDescending { reg.constructorTitles(it.id).count } else list }),
            circuits = k.copy(data = k.data?.filter {
                it.name.matchesSearch(q) || it.locality.matchesSearch(q) || it.country.matchesSearch(q)
            }),
            glossary = Glossary.terms.filter { it.matches(q, language) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EncyclopediaUiState(currentSeason = season))

    init {
        viewModelScope.launch { registry.value = encyclopedia.champions() }
    }

    /** Lists are only downloaded when their tab is first shown. */
    fun onTabShown(tab: EncyclopediaTab) {
        if (!loaded.add(tab)) return
        when (tab) {
            EncyclopediaTab.DRIVERS -> drivers.refresh()
            EncyclopediaTab.CONSTRUCTORS -> constructors.refresh()
            EncyclopediaTab.CIRCUITS -> circuits.refresh()
            else -> Unit
        }
    }

    fun refresh(tab: EncyclopediaTab) {
        when (tab) {
            EncyclopediaTab.DRIVERS -> drivers.refresh(force = true)
            EncyclopediaTab.CONSTRUCTORS -> constructors.refresh(force = true)
            EncyclopediaTab.CIRCUITS -> circuits.refresh(force = true)
            else -> Unit
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onChampionsOnlyChange(value: Boolean) {
        championsOnly.value = value
    }
}
