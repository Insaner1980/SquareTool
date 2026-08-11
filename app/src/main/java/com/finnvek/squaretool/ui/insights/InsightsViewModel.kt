package com.finnvek.squaretool.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finnvek.squaretool.data.repository.SquareToolRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InsightsUiState(
    val model: InsightsModel? = null,
    val isLoading: Boolean = true,
)

class InsightsViewModel(
    repository: SquareToolRepository,
    projectId: String,
) : ViewModel() {
    val uiState =
        combine(
            repository.observeProject(projectId),
            repository.observeProjectCells(projectId),
            repository.observeDesignsWithRounds(),
            repository.observeColors(),
            repository.observeProjectPalette(projectId),
        ) { project, cells, designs, colors, palette ->
            InsightsUiState(
                model = project?.let { buildInsightsModel(it, cells, designs, colors, palette) },
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    companion object {
        fun factory(
            repository: SquareToolRepository,
            projectId: String,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(InsightsViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return InsightsViewModel(repository, projectId) as T
                }
            }
    }
}
