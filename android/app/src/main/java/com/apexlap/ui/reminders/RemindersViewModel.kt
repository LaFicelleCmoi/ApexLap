package com.apexlap.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.PlanRemindersUseCase
import com.apexlap.domain.usecase.PlannedReminder
import com.apexlap.domain.usecase.ReminderScope
import com.apexlap.domain.usecase.ReminderSettings
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.reminders.ReminderScheduler
import com.apexlap.reminders.ReminderSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemindersUiState(
    val settings: ReminderSettings = ReminderSettings(),
    val upcoming: List<PlannedReminder> = emptyList(),
    val exactAlarmsAllowed: Boolean = true,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val store: ReminderSettingsStore,
    private val scheduler: ReminderScheduler,
    repository: F1Repository,
    private val clock: Clock,
    private val plan: PlanRemindersUseCase,
) : ViewModel() {

    private val exactAllowed = MutableStateFlow(scheduler.canScheduleExact)

    val uiState: StateFlow<RemindersUiState> = combine(
        store.settings,
        repository.observeCalendar(clock.currentSeason()),
        exactAllowed,
    ) { settings, calendar, exact ->
        RemindersUiState(settings, plan(calendar, clock.now(), settings), exact)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun setLeadMinutes(minutes: Int) = update { it.copy(leadMinutes = minutes) }

    fun setScope(scope: ReminderScope) = update { it.copy(scope = scope) }

    /** Called when the screen resumes: the user may have granted exact alarms in system settings. */
    fun onResume() {
        exactAllowed.value = scheduler.canScheduleExact
        viewModelScope.launch { scheduler.reschedule() }
    }

    private fun update(transform: (ReminderSettings) -> ReminderSettings) {
        viewModelScope.launch {
            store.update(transform)
            scheduler.reschedule()
        }
    }
}
