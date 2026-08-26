package com.finnvek.squaretool.ui

fun <T> moveListItem(
    values: List<T>,
    fromIndex: Int,
    toIndex: Int,
): List<T> {
    if (fromIndex !in values.indices || toIndex !in values.indices || fromIndex == toIndex) return values
    return values.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
