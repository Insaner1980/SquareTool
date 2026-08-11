package com.finnvek.squaretool.domain.model

enum class MeasurementUnit {
    CENTIMETERS,
    INCHES,
}

data class BlanketDimensions(
    val width: Double,
    val height: Double,
    val unit: MeasurementUnit,
)

data class ProjectProgress(
    val completedCount: Int,
    val remainingCount: Int,
    val totalCount: Int,
    val percentage: Int,
)

data class DesignDistribution(
    val designCounts: Map<String, Int>,
    val assignedCount: Int,
    val blankCount: Int,
)

data class DesignColorProfile(
    val designId: String,
    val roundColorIds: List<String>,
    val roundWeights: List<Double>,
    val gramsPerSquareOverride: Double? = null,
)

data class ColorUsage(
    val percentages: Map<String, Double>,
    val contributingCellCount: Int,
)

data class YarnSettings(
    val globalGramsPerSquare: Double?,
    val skeinWeightGrams: Double?,
    val bufferPercent: Double = 0.0,
)

data class YarnEstimate(
    val baseGrams: Double,
    val totalGrams: Double,
    val equivalentSkeins: Double,
    val recommendedWholeSkeins: Int,
    val colorGrams: Map<String, Double>,
)

enum class MirrorDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
}

enum class GradientDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
    DIAGONAL,
}
