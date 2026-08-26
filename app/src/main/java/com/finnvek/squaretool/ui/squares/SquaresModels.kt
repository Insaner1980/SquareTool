package com.finnvek.squaretool.ui.squares

import com.finnvek.squaretool.data.local.ColorEntity
import com.finnvek.squaretool.data.local.SquareDesignEntity
import com.finnvek.squaretool.render.MotifTemplateCategory
import com.finnvek.squaretool.render.MotifTemplateRegistry

enum class SquareFilter {
    ALL,
    FAVORITES,
    FLORAL,
    GEOMETRIC,
    SIMPLE,
    CUSTOM,
}

data class SquareDesignListItem(
    val design: SquareDesignEntity,
    val roundColors: List<ColorEntity>,
) {
    val id: String get() = design.id
    val canEdit: Boolean get() = !design.builtIn
    val canDuplicate: Boolean get() = true
}

fun filterSquareDesigns(
    designs: List<SquareDesignListItem>,
    query: String,
    filter: SquareFilter,
): List<SquareDesignListItem> {
    val normalizedQuery = query.trim()
    return designs.filter { item ->
        val matchesQuery =
            normalizedQuery.isEmpty() ||
                listOf(
                    item.design.name,
                    item.design.note,
                    item.design.category,
                ).any { it.contains(normalizedQuery, ignoreCase = true) }
        val matchesFilter =
            when (filter) {
                SquareFilter.ALL -> true
                SquareFilter.FAVORITES -> item.design.favorite
                SquareFilter.FLORAL -> item.matchesCategory(MotifTemplateCategory.FLORAL, "floral")
                SquareFilter.GEOMETRIC -> item.matchesCategory(MotifTemplateCategory.GEOMETRIC, "geometric")
                SquareFilter.SIMPLE -> item.matchesCategory(MotifTemplateCategory.SIMPLE, "simple")
                SquareFilter.CUSTOM -> !item.design.builtIn
            }
        matchesQuery && matchesFilter
    }
}

private fun SquareDesignListItem.matchesCategory(
    templateCategory: MotifTemplateCategory,
    label: String,
): Boolean =
    design.category.equals(label, ignoreCase = true) ||
        MotifTemplateRegistry.find(design.motifTemplateId)?.category == templateCategory

enum class SquareDraftError {
    NAME,
    TEMPLATE,
    ROUND_COUNT,
    ROUND_COLOR,
}

data class SquareEditorDraft(
    val id: String,
    val name: String,
    val templateId: String,
    val roundColorIds: List<String>,
    val notes: String,
    val favorite: Boolean,
    val sourceBuiltIn: Boolean,
) {
    fun validationErrors(): Set<SquareDraftError> =
        buildSet {
            if (name.isBlank()) add(SquareDraftError.NAME)
            val template = MotifTemplateRegistry.find(templateId)
            if (template == null) {
                add(SquareDraftError.TEMPLATE)
            } else if (!template.supportsRoundCount(roundColorIds.size)) {
                add(SquareDraftError.ROUND_COUNT)
            }
            if (roundColorIds.any(String::isBlank)) add(SquareDraftError.ROUND_COLOR)
        }

    fun planTemplateChange(newTemplateId: String): SquareTemplateChange {
        val template = MotifTemplateRegistry.require(newTemplateId)
        val needsTruncation = roundColorIds.size > template.maxRounds
        val retained = roundColorIds.take(template.maxRounds).toMutableList()
        val fallbackColor = retained.lastOrNull() ?: roundColorIds.lastOrNull().orEmpty()
        while (retained.size < template.minRounds) retained += fallbackColor
        return SquareTemplateChange(
            updatedDraft = copy(templateId = newTemplateId, roundColorIds = retained),
            requiresRoundTruncationConfirmation = needsTruncation,
        )
    }
}

data class SquareTemplateChange(
    val updatedDraft: SquareEditorDraft,
    val requiresRoundTruncationConfirmation: Boolean,
)
