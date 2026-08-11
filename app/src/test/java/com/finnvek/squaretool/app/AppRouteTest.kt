package com.finnvek.squaretool.app

import com.finnvek.squaretool.ui.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppRouteTest {
    @Test
    fun topLevelRoutesMapToTheirDestination() {
        TopLevelDestination.entries.forEach { destination ->
            assertEquals(destination, AppRoute.topLevelDestination(destination.route))
        }
    }

    @Test
    fun secondaryRoutesDoNotPretendToBeTopLevel() {
        assertNull(AppRoute.topLevelDestination(AppRoute.projectEditor("project-1")))
        assertNull(AppRoute.topLevelDestination(AppRoute.insights("project-1")))
        assertNull(AppRoute.topLevelDestination(AppRoute.export("project-1")))
        assertNull(
            AppRoute.topLevelDestination(AppRoute.plannerWithDesign("project-1", "design-1")),
        )
    }

    @Test
    fun insightsKeepsPlannerSelectedInPrimaryNavigation() {
        assertEquals(
            TopLevelDestination.Planner,
            AppRoute.navigationDestination(AppRoute.InsightsPattern),
        )
        assertEquals(
            TopLevelDestination.Planner,
            AppRoute.navigationDestination(AppRoute.insights("project-1")),
        )
        assertEquals(
            TopLevelDestination.Planner,
            AppRoute.navigationDestination(AppRoute.plannerWithDesign("project-1", "design-1")),
        )
    }

    @Test
    fun entityEditorRoutesCarryIdentityAndDuplicateFlag() {
        assertEquals("square-editor/design-1/true", AppRoute.squareEditor("design-1", true))
        assertEquals("color-editor/color-1/false", AppRoute.colorEditor("color-1", false))
        assertEquals(
            "palette-editor/palette-1/true/project-1",
            AppRoute.paletteEditor("palette-1", true, "project-1"),
        )
    }

    @Test
    fun newEntityRoutesUseStableSentinelValues() {
        assertEquals("project-editor/new", AppRoute.projectEditor())
        assertEquals("square-editor/new/false", AppRoute.squareEditor())
        assertEquals("color-editor/new/false", AppRoute.colorEditor())
        assertEquals("palette-editor/new/false/none", AppRoute.paletteEditor())
    }
}
