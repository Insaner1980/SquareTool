package com.finnvek.squaretool.export

import com.finnvek.squaretool.domain.model.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectExportSnapshotTest {
    @Test
    fun storedMeasurementUnitsAreParsedCaseInsensitively() {
        assertEquals(MeasurementUnit.CENTIMETERS, parseExportMeasurementUnit("centimeters"))
        assertEquals(MeasurementUnit.INCHES, parseExportMeasurementUnit("INCHES"))
    }

    @Test
    fun materialsSummaryUsesStoredRoundColorIdsAndTemplateWeights() {
        val design =
            exportDesign("a", "Olive Bloom").copy(
                roundColorIds = listOf("olive", "cream", "orange"),
            )
        val snapshot =
            exportSnapshot(
                designs = listOf(design),
                cells =
                    listOf(
                        ExportCell(0, 0, "a", false, false),
                        ExportCell(0, 1, "a", false, false),
                    ),
            )

        val summary = snapshot.materialsSummary()

        assertEquals(10.0, summary.colorUsagePercentages.getValue("olive"), 0.0001)
        assertEquals(14.0, summary.colorUsagePercentages.getValue("cream"), 0.0001)
        assertEquals(76.0, summary.colorUsagePercentages.getValue("orange"), 0.0001)
    }

    @Test
    fun legendEntries_countAssignedCellsAndIgnoreMissingReferences() {
        val snapshot =
            exportSnapshot(
                designs = listOf(exportDesign("a", "Olive Bloom"), exportDesign("b", "Sunburst")),
                cells =
                    listOf(
                        ExportCell(0, 0, "b", false, false),
                        ExportCell(0, 1, "a", false, false),
                        ExportCell(1, 0, "a", false, true),
                        ExportCell(1, 1, "missing", false, false),
                    ),
            )

        assertEquals(
            listOf(
                ExportLegendEntry("A", snapshot.designs[0], 2),
                ExportLegendEntry("B", snapshot.designs[1], 1),
            ),
            snapshot.legendEntries(),
        )
    }

    @Test
    fun legendCode_supportsMoreThanTwentySixDesigns() {
        assertEquals("A", legendCode(0))
        assertEquals("Z", legendCode(25))
        assertEquals("AA", legendCode(26))
        assertEquals("AB", legendCode(27))
    }

    @Test
    fun visualForCell_returnsNullForBlankOrMissingDesign() {
        val snapshot =
            exportSnapshot(
                designs = listOf(exportDesign("a", "Olive Bloom")),
                cells = emptyList(),
            )

        assertNull(snapshot.visualForCell(ExportCell(0, 0, null, false, false)))
        assertNull(snapshot.visualForCell(ExportCell(0, 0, "missing", false, false)))
    }

    private fun exportSnapshot(
        designs: List<ExportDesign>,
        cells: List<ExportCell>,
    ) = ProjectExportSnapshot(
        project =
            ExportProject(
                name = "Test",
                notes = "",
                rows = 2,
                columns = 2,
                squareWidth = null,
                squareHeight = null,
                joiningGap = null,
                measurementUnit = "CENTIMETERS",
                trackingEnabled = true,
                globalGramsPerSquare = null,
                skeinWeightGrams = null,
                bufferPercent = 10.0,
            ),
        designs = designs,
        colors = emptyList(),
        cells = cells,
    )

    private fun exportDesign(
        id: String,
        name: String,
    ) = ExportDesign(id, name, "classic_granny", listOf(0xFF6B8A2E.toInt(), 0xFFF3E6C9.toInt(), 0xFFD75A1F.toInt()), null)
}
