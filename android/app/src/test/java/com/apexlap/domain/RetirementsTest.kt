package com.apexlap.domain

import com.apexlap.domain.usecase.Retirement
import com.apexlap.domain.usecase.RetirementKind
import com.apexlap.domain.usecase.SeasonRetirementsRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetirementsTest {

    @Test
    fun `2026 record totals 73 retirements, most first`() {
        val record = SeasonRetirementsRecord.of(2026)!!
        assertEquals(73, record.total)
        assertEquals("Stroll", record.drivers.first().driver)
        assertEquals(12, record.drivers.first().count)
        assertEquals(record.drivers.sortedByDescending { it.count }.map { it.count }, record.drivers.map { it.count })
        assertNull(SeasonRetirementsRecord.of(2025))
    }

    @Test
    fun `classified and non-classified retirements are parsed`() {
        assertEquals(
            listOf(
                Retirement("Australie", RetirementKind.NOT_CLASSIFIED),
                Retirement("Chine"),
                Retirement("Azerbaïdjan", RetirementKind.CLASSIFIED),
            ),
            SeasonRetirementsRecord.parse("Australie (NC), Chine, Azerbaïdjan*"),
        )
        assertEquals("Azerbaïdjan*", Retirement("Azerbaïdjan", RetirementKind.CLASSIFIED).label)
    }
}
