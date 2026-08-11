package com.finnvek.squaretool.app

import com.finnvek.squaretool.ui.navigation.TopLevelDestination

object AppRoute {
    const val Projects = "projects"
    const val ProjectEditorPattern = "project-editor/{projectId}"
    const val InsightsPattern = "insights/{projectId}"
    const val ExportPattern = "export/{projectId}"
    const val AccessiblePlannerPattern = "accessible-planner/{projectId}"
    const val PlannerDesignPattern = "planner-design/{projectId}/{designId}"
    const val SquareEditorPattern = "square-editor/{designId}/{duplicate}"
    const val ColorEditorPattern = "color-editor/{colorId}/{duplicate}"
    const val PaletteEditorPattern = "palette-editor/{paletteId}/{duplicate}/{projectId}"
    const val Backup = "backup"
    const val About = "about"

    fun projectEditor(projectId: String? = null) = "project-editor/${projectId ?: "new"}"

    fun insights(projectId: String) = "insights/$projectId"

    fun export(projectId: String) = "export/$projectId"

    fun accessiblePlanner(projectId: String) = "accessible-planner/$projectId"

    fun plannerWithDesign(
        projectId: String,
        designId: String,
    ) = "planner-design/$projectId/$designId"

    fun squareEditor(
        designId: String? = null,
        duplicate: Boolean = false,
    ) = "square-editor/${designId ?: "new"}/$duplicate"

    fun colorEditor(
        colorId: String? = null,
        duplicate: Boolean = false,
    ) = "color-editor/${colorId ?: "new"}/$duplicate"

    fun paletteEditor(
        paletteId: String? = null,
        duplicate: Boolean = false,
        projectId: String? = null,
    ) = "palette-editor/${paletteId ?: "new"}/$duplicate/${projectId ?: "none"}"

    fun topLevelDestination(route: String?): TopLevelDestination? = TopLevelDestination.entries.firstOrNull { it.route == route }

    fun navigationDestination(route: String?): TopLevelDestination? =
        topLevelDestination(route) ?: if (
            route == InsightsPattern || route?.startsWith("insights/") == true ||
            route == PlannerDesignPattern || route?.startsWith("planner-design/") == true
        ) {
            TopLevelDestination.Planner
        } else {
            null
        }
}
