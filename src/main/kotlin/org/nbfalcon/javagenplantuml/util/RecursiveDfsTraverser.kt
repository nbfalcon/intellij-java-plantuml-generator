package org.nbfalcon.javagenplantuml.util

private class VisitMe<T>(val roots: Array<out T>, var curIndex: Int = 0) {
    val isEmpty: Boolean get() = curIndex >= roots.size

    fun next(): T = roots[curIndex++]
}

fun <T> traverseDfs(roots: Array<out T>, cb: (T) -> Array<out T>?) {
    val stack = mutableListOf(VisitMe(roots))
    while (stack.isNotEmpty()) {
        val top = stack[stack.size - 1]
        if (top.isEmpty) stack.removeLast()
        else {
            val newRoots = cb(top.next())?.takeIf { it.isNotEmpty() } ?: continue
            stack.add(VisitMe(newRoots))
        }
    }
}