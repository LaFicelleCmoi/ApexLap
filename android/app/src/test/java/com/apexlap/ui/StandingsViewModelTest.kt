package com.apexlap.ui

import app.cash.turbine.test
import com.apexlap.data.repository.OfflineFirstF1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.testing.FakeF1Dao
import com.apexlap.testing.FakeJolpicaApi
import com.apexlap.ui.standings.StandingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StandingsViewModelTest {

    private val api = FakeJolpicaApi()
    private val clock = Clock { Instant.parse("2026-10-11T18:00:00Z") }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = StandingsViewModel(OfflineFirstF1Repository(api, FakeF1Dao(), clock), clock)

    @Test
    fun `loads standings for the current season`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(2026, state.season)
            assertEquals(3, state.drivers?.size)
            assertFalse(state.isRefreshing)
        }
    }

    @Test
    fun `search filters drivers by name, code or team`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            vm.onQueryChange("ham")
            assertEquals(listOf("hamilton"), expectMostRecentItem().drivers?.map { it.driver.id })
            vm.onQueryChange("mercedes")
            assertEquals(listOf("antonelli", "russell"), expectMostRecentItem().drivers?.map { it.driver.id })
        }
    }

    @Test
    fun `offline refresh surfaces an error message`() = runTest {
        api.offline = true
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(com.apexlap.i18n.I18n.strings.offlineCached, expectMostRecentItem().error)
        }
    }
}
