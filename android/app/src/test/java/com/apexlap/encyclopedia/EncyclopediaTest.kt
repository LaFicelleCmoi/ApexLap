package com.apexlap.encyclopedia

import com.apexlap.data.encyclopedia.ChampionsAsset
import com.apexlap.data.encyclopedia.EncyclopediaRepository
import com.apexlap.domain.encyclopedia.ChampionsRegistry
import com.apexlap.domain.encyclopedia.CircuitGuides
import com.apexlap.domain.encyclopedia.GrandPrixFacts
import com.apexlap.domain.encyclopedia.SeasonHighlights
import com.apexlap.domain.encyclopedia.SeasonWinner
import com.apexlap.domain.encyclopedia.Titles
import com.apexlap.domain.encyclopedia.driverBio
import com.apexlap.domain.encyclopedia.formatSeasons
import com.apexlap.domain.encyclopedia.parseLapTime
import com.apexlap.data.remote.dto.RaceTableData
import com.apexlap.data.remote.toDomain
import com.apexlap.domain.model.CareerStats
import com.apexlap.domain.model.DriverProfile
import com.apexlap.testing.decodeFixture
import java.time.LocalDate
import com.apexlap.domain.encyclopedia.Glossary
import com.apexlap.domain.encyclopedia.matchesSearch
import com.apexlap.testing.FakeF1Dao
import com.apexlap.testing.FakeJolpicaApi
import com.apexlap.testing.testJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EncyclopediaTest {

    /** The real bundled asset, read from the source tree. */
    private val registry = ChampionsRegistry(ChampionsAsset.parse(testJson, File("src/main/assets/champions.json").readText()))

    @Test
    fun `bundled champions cover 1950 to 2025, most recent first`() {
        assertEquals(76, registry.champions.size)
        assertEquals(2025, registry.champions.first().season)
        assertEquals(1950, registry.champions.last().season)
        assertEquals("farina", registry.season(1950)?.driver?.id)
        assertNull(registry.season(1950)?.constructor)
        assertEquals("vanwall", registry.season(1958)?.constructor?.id)
    }

    @Test
    fun `titles are counted per driver and constructor`() {
        assertEquals(7, registry.driverTitles("hamilton").count)
        assertEquals(7, registry.driverTitles("michael_schumacher").count)
        assertEquals(listOf(1988, 1990, 1991), registry.driverTitles("senna").seasons.sorted())
        assertEquals(16, registry.constructorTitles("ferrari").count)
        assertEquals(0, registry.driverTitles("nobody").count)
    }

    @Test
    fun `search ignores case and accents`() {
        assertTrue("Sergio Pérez".matchesSearch("perez"))
        assertTrue("Kimi Räikkönen".matchesSearch("RAIKKONEN"))
        assertTrue("Anything".matchesSearch("  "))
        assertFalse("Hamilton".matchesSearch("senna"))
    }

    @Test
    fun `glossary terms are unique and defined`() {
        assertEquals(Glossary.terms.size, Glossary.terms.map { it.term }.toSet().size)
        assertTrue(Glossary.terms.all { it.definition.length > 20 })
    }

    @Test
    fun `all-time drivers are paged once and not re-downloaded`() = runTest {
        val api = FakeJolpicaApi()
        val dao = FakeF1Dao()
        val repository = EncyclopediaRepository(android.app.Application(), api, dao, testJson)

        repository.refreshDrivers(force = false)
        assertEquals(listOf(0, 100, 200), api.driverPages)
        assertEquals(250, repository.observeDrivers().first().size)

        repository.refreshDrivers(force = false)
        assertEquals(3, api.driverPages.size)
        repository.refreshDrivers(force = true)
        assertEquals(6, api.driverPages.size)
    }

    @Test
    fun `circuit history ranks the winners`() = runTest {
        val repository = EncyclopediaRepository(android.app.Application(), FakeJolpicaApi(), FakeF1Dao(), testJson)
        val history = repository.circuitHistory("marina_bay")
        assertEquals("marina_bay", history.circuit.id)
        assertEquals(1, history.grandsPrix)
        assertEquals(2026, history.firstSeason)
        assertEquals("max_verstappen", history.topDrivers().single().first.id)
    }

    private val singapore = decodeFixture<com.apexlap.data.remote.dto.JolpicaResponse<RaceTableData>>("results_singapore_2026.json")
        .data.raceTable.races.first()
    private val results = singapore.results.map { it.toDomain() }

    @Test
    fun `lap times are parsed to milliseconds`() {
        assertEquals(81_046L, parseLapTime("1:21.046"))
        assertEquals(58_123L, parseLapTime("58.123"))
        assertNull(parseLapTime("DNF"))
    }

    @Test
    fun `consecutive title seasons collapse into ranges`() {
        assertEquals("2008, 2014–2015, 2017–2020", formatSeasons(listOf(2020, 2008, 2014, 2015, 2017, 2018, 2019)))
    }

    @Test
    fun `driver bio mentions age, titles and career`() {
        val driver = results.first().driver.copy(dateOfBirth = LocalDate.of(1997, 9, 30))
        val bio = driverBio(
            DriverProfile(driver, CareerStats(seasons = 12, races = 230, wins = 70, podiums = 120, poles = 45)),
            Titles(4, listOf(2021, 2022, 2023, 2024)),
            standing = null,
            season = 2026,
            today = LocalDate.of(2026, 10, 12),
        )
        assertTrue(bio, bio.contains("(29 ans)"))
        assertTrue(bio, bio.contains("4 fois champion du monde (2021–2024)"))
        assertTrue(bio, bio.contains("230 Grands Prix en 12 saisons : 70 victoires"))
    }

    @Test
    fun `grand prix facts give winner, pole and fastest lap`() {
        val facts = GrandPrixFacts.of(results, emptyList())
        assertEquals(results.first { it.position == 1 }.driver, facts.winner?.driver)
        assertEquals(results.firstOrNull { it.grid == 1 }?.driver, facts.pole)
        assertEquals(results.firstOrNull { it.hasFastestLap }, facts.fastestLap)
        assertTrue(GrandPrixFacts.of(emptyList(), emptyList()).isEmpty)
    }

    @Test
    fun `season highlights count winners and the title margin`() {
        val race = singapore.toDomain()
        val first = results[0]
        val second = results[1]
        val winners = listOf(
            SeasonWinner(race.copy(round = 1), first.copy(grid = 1)),
            SeasonWinner(race.copy(round = 2), first.copy(grid = 4)),
            SeasonWinner(race.copy(round = 3), second.copy(grid = 9)),
        )
        val standings = listOf(
            com.apexlap.domain.model.DriverStanding(1, 300.0, 2, first.driver, first.constructor),
            com.apexlap.domain.model.DriverStanding(2, 288.0, 1, second.driver, second.constructor),
        )
        val highlights = SeasonHighlights.of(winners, standings)
        assertEquals(2, highlights.differentWinners)
        assertEquals(first.driver to 2, highlights.topWinner)
        assertEquals(1, highlights.winsFromPole)
        assertEquals(9, highlights.deepestWin?.winner?.grid)
        assertEquals(12.0, highlights.titleMargin!!, 0.0)
    }

    @Test
    fun `circuit guides exist for the calendar circuits`() {
        assertTrue(CircuitGuides.of("spa")!!.corners.any { it.name.contains("Eau Rouge") })
        assertNull(CircuitGuides.of("unknown"))
    }

    @Test
    fun `grand prix context counts editions before the season`() = runTest {
        val repository = EncyclopediaRepository(android.app.Application(), FakeJolpicaApi(), FakeF1Dao(), testJson)
        val history = repository.circuitHistory("marina_bay")
        assertEquals(1, history.contextFor(2026).edition)
        assertNull(history.contextFor(2026).previousWinner)
        assertEquals(2, history.contextFor(2027).edition)
        assertEquals(2026, history.contextFor(2027).previousWinner?.season)
    }
}
