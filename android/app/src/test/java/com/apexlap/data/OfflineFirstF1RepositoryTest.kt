package com.apexlap.data

import app.cash.turbine.test
import com.apexlap.data.repository.OfflineFirstF1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.testing.FakeF1Dao
import com.apexlap.testing.FakeJolpicaApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Duration
import java.time.Instant

class OfflineFirstF1RepositoryTest {

    private val api = FakeJolpicaApi()
    private val dao = FakeF1Dao()
    private var now = Instant.parse("2026-10-11T18:00:00Z")
    private val clock = Clock { now }
    private val repository = OfflineFirstF1Repository(api, dao, clock)

    @Test
    fun `calendar is served from the cache and updated by refresh`() = runTest {
        repository.observeCalendar(2026).test {
            assertEquals(emptyList<Any>(), awaitItem())
            repository.refreshCalendar(2026)
            assertEquals(listOf(1, 2, 3), awaitItem().map { it.round })
        }
    }

    @Test
    fun `failed refresh keeps the previous cache`() = runTest {
        repository.refreshStandings(2026)
        api.offline = true

        val error = runCatching { repository.refreshStandings(2026) }.exceptionOrNull()

        assertTrue(error is IOException)
        repository.observeDriverStandings(2026).test {
            assertEquals(listOf("antonelli", "russell", "hamilton"), awaitItem().map { it.driver.id })
        }
    }

    @Test
    fun `weekend refresh stores race, sprint and qualifying without accumulating rows`() = runTest {
        repository.refreshRaceWeekend(2026, 17, includeSprint = true)
        repository.refreshRaceWeekend(2026, 17, includeSprint = true)

        assertEquals(3, repository.observeRaceResults(2026, 17).first().size)
        assertEquals(3, repository.observeSprintResults(2026, 17).first().size)
        val qualifying = repository.observeQualifying(2026, 17).first()
        assertEquals(listOf("max_verstappen", "leclerc", "hamilton"), qualifying.map { it.driver.id })
        assertEquals("1:31.373", qualifying.first().bestTime)
    }

    @Test
    fun `season results walk every page and drop rows repeated across pages`() = runTest {
        api.seasonTotal = 150

        repository.refreshSeasonResults(2026)

        assertEquals(listOf("results@0", "results@100", "sprint@0"), api.pageRequests)
        val season = repository.observeSeasonResults(2026).first()
        assertEquals(3, season.count { !it.isSprint })
        assertEquals(3, season.count { it.isSprint })
    }

    @Test
    fun `season refresh is skipped while fresh unless forced`() = runTest {
        repository.refreshSeasonResults(2026)
        repository.refreshSeasonResults(2026)
        assertEquals(2, api.pageRequests.size)

        repository.refreshSeasonResults(2026, force = true)
        assertEquals(4, api.pageRequests.size)
    }

    @Test
    fun `profiles are cached for twelve hours`() = runTest {
        repository.refreshDriverProfile("antonelli")
        val requests = api.countRequests.size

        now = now.plus(Duration.ofHours(11))
        repository.refreshDriverProfile("antonelli")
        assertEquals(requests, api.countRequests.size)

        now = now.plus(Duration.ofHours(2))
        repository.refreshDriverProfile("antonelli")
        assertEquals(requests * 2, api.countRequests.size)
    }

    @Test
    fun `career stats are derived from result counts`() = runTest {
        api.counts = mapOf(
            "seasons" to 12, "results" to 250, "results/1" to 73,
            "results/2" to 40, "results/3" to 15, "grid/1/results" to 48,
        )

        repository.refreshDriverProfile("antonelli")

        repository.observeDriverProfile("antonelli").test {
            val stats = awaitItem()!!.stats
            assertEquals(12, stats.seasons)
            assertEquals(250, stats.races)
            assertEquals(73, stats.wins)
            assertEquals(128, stats.podiums)
            assertEquals(48, stats.poles)
        }
        assertTrue(api.countRequests.all { it.startsWith("drivers/antonelli/") })
    }
}
