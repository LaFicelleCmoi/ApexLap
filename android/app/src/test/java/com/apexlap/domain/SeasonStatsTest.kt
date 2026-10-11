package com.apexlap.domain

import com.apexlap.domain.model.Constructor
import com.apexlap.domain.model.Driver
import com.apexlap.domain.model.RaceResult
import com.apexlap.domain.model.ScoredResult
import com.apexlap.domain.usecase.ComputeChampionshipProgressionUseCase
import com.apexlap.domain.usecase.ComputeHeadToHeadUseCase
import com.apexlap.domain.usecase.ComputeSeasonLeadersUseCase
import com.apexlap.domain.usecase.LeaderCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeasonStatsTest {

    private val mercedes = Constructor("mercedes", "Mercedes", "German")
    private val ferrari = Constructor("ferrari", "Ferrari", "Italian")
    private val ant = Driver("antonelli", "ANT", "12", "Kimi", "Antonelli", "Italian", null)
    private val lec = Driver("leclerc", "LEC", "16", "Charles", "Leclerc", "Monegasque", null)

    private fun result(
        round: Int,
        driver: Driver,
        position: Int,
        points: Double,
        grid: Int = position,
        sprint: Boolean = false,
        positionText: String = position.toString(),
        fastest: Boolean = false,
    ) = ScoredResult(
        round,
        sprint,
        RaceResult(position, positionText, points, grid, 50, "Finished", null, if (fastest) 1 else 5, null, driver,
            if (driver == ant) mercedes else ferrari),
    )

    private val season = listOf(
        result(1, ant, 1, 25.0, grid = 2, fastest = true),
        result(1, lec, 2, 18.0, grid = 1),
        result(2, ant, 1, 8.0, sprint = true),
        result(2, lec, 2, 7.0, sprint = true),
        result(2, ant, 15, 0.0, grid = 3, positionText = "R"),
        result(2, lec, 1, 25.0, grid = 5),
        result(3, ant, 3, 15.0, grid = 1),
        result(3, lec, 4, 12.0, grid = 2),
    )

    @Test
    fun `progression accumulates race and sprint points round by round, leader first`() {
        val progression = ComputeChampionshipProgressionUseCase()(season)
        assertEquals(listOf(1, 2, 3), progression.rounds)
        assertEquals(listOf("leclerc", "antonelli"), progression.drivers.map { it.driver.id })
        // Leclerc: 18, then sprint 7 + race 25, then 12.
        assertEquals(listOf(18.0, 50.0, 62.0), progression.drivers[0].cumulativePoints)
        // Antonelli: 25, then sprint 8 + retirement, then 15.
        assertEquals(listOf(25.0, 33.0, 48.0), progression.drivers[1].cumulativePoints)
    }

    @Test
    fun `leaders count Grand Prix only and skip zero scores`() {
        val leaders = ComputeSeasonLeadersUseCase()(season).associateBy { it.category }
        assertEquals(listOf(1, 1), leaders.getValue(LeaderCategory.WINS).entries.map { it.value })
        assertEquals("antonelli", leaders.getValue(LeaderCategory.FASTEST_LAPS).entries.single().driver.id)
        assertEquals(listOf("antonelli" to 1, "leclerc" to 1), leaders.getValue(LeaderCategory.POLES).entries.map { it.driver.id to it.value })
        assertEquals("antonelli", leaders.getValue(LeaderCategory.DNFS).entries.single().driver.id)
        // Leclerc: -1, +4, -2 = +1. Antonelli: +1, (retired, ignored), -2 = -1, so not a leader.
        assertEquals(listOf("leclerc" to 1), leaders.getValue(LeaderCategory.POSITIONS_GAINED).entries.map { it.driver.id to it.value })
    }

    @Test
    fun `head to head compares shared Grands Prix`() {
        val duel = ComputeHeadToHeadUseCase()(season, "antonelli", "leclerc")!!
        assertEquals(3, duel.sharedRaces)
        assertEquals(2, duel.first.racesAhead)
        assertEquals(1, duel.second.racesAhead)
        assertEquals(2, duel.first.startsAhead)
        assertEquals(1, duel.second.startsAhead)
        assertEquals(48.0, duel.first.points, 0.0)
        assertEquals(1, duel.first.dnfs)
        assertEquals(2.0, duel.first.averageFinish!!, 0.0)
        assertEquals(1, duel.second.bestFinish)
    }

    @Test
    fun `no duel against oneself or an unknown driver`() {
        assertNull(ComputeHeadToHeadUseCase()(season, "antonelli", "antonelli"))
        assertNull(ComputeHeadToHeadUseCase()(season, "antonelli", "nobody"))
    }
}
