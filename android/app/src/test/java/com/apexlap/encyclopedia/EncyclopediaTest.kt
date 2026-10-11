package com.apexlap.encyclopedia

import com.apexlap.data.encyclopedia.ChampionsAsset
import com.apexlap.data.encyclopedia.EncyclopediaRepository
import com.apexlap.domain.encyclopedia.ChampionsRegistry
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
}
