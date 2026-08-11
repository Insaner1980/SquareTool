package com.finnvek.squaretool.domain.algorithm

import com.finnvek.squaretool.domain.model.GridSnapshot
import java.util.ArrayDeque

data class PlannerHistoryEntry(
    val label: String,
    val before: GridSnapshot,
    val after: GridSnapshot,
)

class PlannerHistory(
    initial: GridSnapshot,
    capacity: Int = DEFAULT_CAPACITY,
) {
    private val capacity = capacity.coerceAtLeast(MINIMUM_CAPACITY)
    private val undoEntries = ArrayDeque<PlannerHistoryEntry>()
    private val redoEntries = ArrayDeque<PlannerHistoryEntry>()

    var current: GridSnapshot = initial
        private set

    val canUndo: Boolean get() = undoEntries.isNotEmpty()
    val canRedo: Boolean get() = redoEntries.isNotEmpty()
    val undoDepth: Int get() = undoEntries.size
    val redoDepth: Int get() = redoEntries.size
    val lastOperationLabel: String? get() = undoEntries.peekLast()?.label

    fun record(
        label: String,
        next: GridSnapshot,
    ): GridSnapshot {
        if (next == current) return current
        undoEntries.addLast(PlannerHistoryEntry(label, current, next))
        while (undoEntries.size > capacity) undoEntries.removeFirst()
        redoEntries.clear()
        current = next
        return current
    }

    fun undo(): GridSnapshot {
        val entry = undoEntries.pollLast() ?: return current
        redoEntries.addLast(entry)
        current = entry.before
        return current
    }

    fun redo(): GridSnapshot {
        val entry = redoEntries.pollLast() ?: return current
        undoEntries.addLast(entry)
        current = entry.after
        return current
    }

    companion object {
        const val MINIMUM_CAPACITY = 50
        const val DEFAULT_CAPACITY = MINIMUM_CAPACITY
    }
}
