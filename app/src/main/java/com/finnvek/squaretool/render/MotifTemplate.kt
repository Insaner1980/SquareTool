package com.finnvek.squaretool.render

enum class MotifTemplateCategory {
    CLASSIC,
    FLORAL,
    GEOMETRIC,
    SIMPLE,
}

data class MotifTemplate(
    val id: String,
    val displayName: String,
    val category: MotifTemplateCategory,
    val minRounds: Int,
    val maxRounds: Int,
    private val baseAreaWeights: List<Float>,
    internal val geometryStyle: MotifGeometryStyle,
) {
    init {
        require(minRounds in 3..6)
        require(maxRounds in minRounds..6)
        require(baseAreaWeights.size == maxRounds)
        require(baseAreaWeights.all { it > 0f })
    }

    fun supportsRoundCount(roundCount: Int): Boolean = roundCount in minRounds..maxRounds

    fun requireSupportedRoundCount(roundCount: Int) {
        require(supportsRoundCount(roundCount)) {
            "Template '$id' supports $minRounds..$maxRounds rounds, not $roundCount"
        }
    }

    /**
     * Returns inner-to-outer color coverage. When fewer rounds are active, the
     * unused outer area belongs to the outermost active color.
     */
    fun areaWeights(roundCount: Int): List<Float> {
        requireSupportedRoundCount(roundCount)
        val active = baseAreaWeights.take(roundCount).toMutableList()
        active[active.lastIndex] = 1f - active.dropLast(1).sum()
        return active
    }
}

enum class MotifGeometryStyle {
    CLASSIC_GRANNY,
    SUNBURST,
    DAISY,
    FLOWER_MEDALLION,
    SOLID_CENTER,
    STAR_BLOOM,
    DIAMOND_LAYERS,
    PINWHEEL,
    CORNER_ACCENT,
    SIMPLE_ROUNDS,
}

object MotifTemplateRegistry {
    val templates: List<MotifTemplate> =
        listOf(
            MotifTemplate(
                id = "classic_granny",
                displayName = "Classic Granny",
                category = MotifTemplateCategory.CLASSIC,
                minRounds = 3,
                maxRounds = 6,
                baseAreaWeights = listOf(0.10f, 0.14f, 0.18f, 0.20f, 0.18f, 0.20f),
                geometryStyle = MotifGeometryStyle.CLASSIC_GRANNY,
            ),
            MotifTemplate(
                id = "sunburst",
                displayName = "Sunburst",
                category = MotifTemplateCategory.FLORAL,
                minRounds = 4,
                maxRounds = 6,
                baseAreaWeights = listOf(0.12f, 0.17f, 0.20f, 0.19f, 0.16f, 0.16f),
                geometryStyle = MotifGeometryStyle.SUNBURST,
            ),
            MotifTemplate(
                id = "daisy",
                displayName = "Daisy",
                category = MotifTemplateCategory.FLORAL,
                minRounds = 4,
                maxRounds = 5,
                baseAreaWeights = listOf(0.10f, 0.18f, 0.25f, 0.24f, 0.23f),
                geometryStyle = MotifGeometryStyle.DAISY,
            ),
            MotifTemplate(
                id = "flower_medallion",
                displayName = "Flower Medallion",
                category = MotifTemplateCategory.FLORAL,
                minRounds = 4,
                maxRounds = 6,
                baseAreaWeights = listOf(0.08f, 0.14f, 0.18f, 0.20f, 0.19f, 0.21f),
                geometryStyle = MotifGeometryStyle.FLOWER_MEDALLION,
            ),
            MotifTemplate(
                id = "solid_center",
                displayName = "Solid Center",
                category = MotifTemplateCategory.SIMPLE,
                minRounds = 3,
                maxRounds = 5,
                baseAreaWeights = listOf(0.32f, 0.22f, 0.18f, 0.15f, 0.13f),
                geometryStyle = MotifGeometryStyle.SOLID_CENTER,
            ),
            MotifTemplate(
                id = "star_bloom",
                displayName = "Star Bloom",
                category = MotifTemplateCategory.FLORAL,
                minRounds = 4,
                maxRounds = 6,
                baseAreaWeights = listOf(0.12f, 0.16f, 0.19f, 0.20f, 0.17f, 0.16f),
                geometryStyle = MotifGeometryStyle.STAR_BLOOM,
            ),
            MotifTemplate(
                id = "diamond_layers",
                displayName = "Diamond Layers",
                category = MotifTemplateCategory.GEOMETRIC,
                minRounds = 3,
                maxRounds = 6,
                baseAreaWeights = listOf(0.08f, 0.13f, 0.16f, 0.18f, 0.21f, 0.24f),
                geometryStyle = MotifGeometryStyle.DIAMOND_LAYERS,
            ),
            MotifTemplate(
                id = "pinwheel",
                displayName = "Pinwheel",
                category = MotifTemplateCategory.GEOMETRIC,
                minRounds = 3,
                maxRounds = 5,
                baseAreaWeights = listOf(0.13f, 0.19f, 0.23f, 0.23f, 0.22f),
                geometryStyle = MotifGeometryStyle.PINWHEEL,
            ),
            MotifTemplate(
                id = "corner_accent",
                displayName = "Corner Accent",
                category = MotifTemplateCategory.GEOMETRIC,
                minRounds = 3,
                maxRounds = 6,
                baseAreaWeights = listOf(0.09f, 0.14f, 0.17f, 0.20f, 0.19f, 0.21f),
                geometryStyle = MotifGeometryStyle.CORNER_ACCENT,
            ),
            MotifTemplate(
                id = "simple_rounds",
                displayName = "Simple Rounds",
                category = MotifTemplateCategory.SIMPLE,
                minRounds = 3,
                maxRounds = 6,
                baseAreaWeights = listOf(0.07f, 0.13f, 0.17f, 0.21f, 0.20f, 0.22f),
                geometryStyle = MotifGeometryStyle.SIMPLE_ROUNDS,
            ),
        )

    private val templatesById = templates.associateBy(MotifTemplate::id)

    fun find(id: String): MotifTemplate? = templatesById[id]

    fun require(id: String): MotifTemplate = requireNotNull(find(id)) { "Unknown motif template '$id'" }
}
