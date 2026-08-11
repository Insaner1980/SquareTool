package com.finnvek.squaretool.ui.home

import com.finnvek.squaretool.ui.projects.ProjectCardModel

data class HomeProjectSelection(
    val current: ProjectCardModel?,
    val recent: List<ProjectCardModel>,
)

fun selectHomeProjects(cards: List<ProjectCardModel>): HomeProjectSelection {
    val current =
        cards.maxWithOrNull(
            compareBy<ProjectCardModel> { it.project.lastOpenedAt }
                .thenBy { it.project.updatedAt },
        )
    val recent =
        cards
            .asSequence()
            .filterNot { it.project.id == current?.project?.id }
            .sortedByDescending { it.project.lastOpenedAt }
            .take(2)
            .toList()
    return HomeProjectSelection(current, recent)
}

fun searchHomeProjects(
    cards: List<ProjectCardModel>,
    query: String,
): List<ProjectCardModel> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return emptyList()
    return cards
        .filter {
            it.project.name.contains(normalized, ignoreCase = true) ||
                it.project.notes.contains(normalized, ignoreCase = true)
        }.sortedByDescending { it.project.lastOpenedAt }
}
