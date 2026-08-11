package com.finnvek.squaretool.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finnvek.squaretool.data.local.ProjectEntity
import com.finnvek.squaretool.data.repository.SquareToolRepository
import com.finnvek.squaretool.ui.projects.ProjectCardModel
import com.finnvek.squaretool.ui.projects.buildProjectCardModels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class HomeNotice { SAVE_FAILED }

data class HomeUiState(
    val query: String = "",
    val current: ProjectCardModel? = null,
    val recent: List<ProjectCardModel> = emptyList(),
    val searchResults: List<ProjectCardModel> = emptyList(),
    val pendingDelete: ProjectEntity? = null,
    val notice: HomeNotice? = null,
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val repository: SquareToolRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val pendingDelete = MutableStateFlow<ProjectEntity?>(null)
    private val notice = MutableStateFlow<HomeNotice?>(null)
    private val cards = repository.observeProjectCardData().map(::buildProjectCardModels)

    val uiState =
        combine(cards, query, pendingDelete, notice) { loaded, currentQuery, deleteProject, currentNotice ->
            val selection = selectHomeProjects(loaded)
            HomeUiState(
                query = currentQuery,
                current = selection.current,
                recent = selection.recent,
                searchResults = searchHomeProjects(loaded, currentQuery),
                pendingDelete = deleteProject,
                notice = currentNotice,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun requestDelete(project: ProjectEntity) {
        pendingDelete.value = project
    }

    fun cancelDelete() {
        pendingDelete.value = null
    }

    fun clearNotice() {
        notice.value = null
    }

    fun openProject(
        projectId: String,
        onOpen: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { repository.markProjectOpened(projectId) }
                .onSuccess { onOpen(projectId) }
                .onFailure { notice.value = HomeNotice.SAVE_FAILED }
        }
    }

    fun toggleFavorite(projectId: String) {
        viewModelScope.launch {
            runCatching {
                val project = repository.getProject(projectId) ?: return@runCatching
                repository.updateProject(project.copy(favorite = !project.favorite, updatedAt = System.currentTimeMillis()))
            }.onFailure { notice.value = HomeNotice.SAVE_FAILED }
        }
    }

    fun duplicate(
        projectId: String,
        onDuplicated: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                val project = repository.getProject(projectId) ?: error("Project not found")
                repository.duplicateProject(
                    sourceProjectId = projectId,
                    newProjectId = UUID.randomUUID().toString(),
                    newName = project.name,
                )
            }.onSuccess { onDuplicated(it.id) }
                .onFailure { notice.value = HomeNotice.SAVE_FAILED }
        }
    }

    fun confirmDelete() {
        val project = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch {
            runCatching { repository.deleteProject(project.id) }
                .onFailure { notice.value = HomeNotice.SAVE_FAILED }
        }
    }

    fun createSample(onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.createSampleProject() }
                .onSuccess { onCreated(it.id) }
                .onFailure { notice.value = HomeNotice.SAVE_FAILED }
        }
    }

    companion object {
        fun factory(repository: SquareToolRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(HomeViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return HomeViewModel(repository) as T
                }
            }
    }
}
