package com.finnvek.squaretool.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataFactoryTest {
    @Test
    fun autumnGardenUsesSpecifiedGridProgressAndPaletteCounts() {
        val sample = SampleDataFactory.create(now = 100)

        assertEquals(12, sample.project.rowCount)
        assertEquals(8, sample.project.columnCount)
        assertEquals(96, sample.cells.size)
        assertEquals(69, sample.cells.count { it.completed })
        assertEquals(10, sample.cells.count { it.locked })
        assertEquals(7, sample.projectColors.size)
        assertEquals(6, sample.designs.size)
        assertTrue(sample.designs.all { (_, rounds) -> rounds.size in 3..6 })
    }

    @Test
    fun sameTimestampProducesSameDeterministicSampleLayout() {
        val first = SampleDataFactory.create(now = 100)
        val second = SampleDataFactory.create(now = 100)

        assertEquals(first, second)
    }
}
