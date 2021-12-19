package com.valb3r.bpmn.intellij.plugin.core.render

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseBpmnRenderElement

fun dumpRenderTree(project: Project) {
    val renderState = lastRenderedState(project)!!
    println("===== Mapped XML tree: =====")
    val parentToElems = renderState.state.currentState.elementByBpmnId.entries
        .groupBy { it.value.parent.id }
        .mapValues { entry -> entry.value.map { it.key.id }.toSet() }
    dumpTree(parentToElems)
    println("=====   End XML tree   =====")

    println("=====   Render tree:  =====")
    dumpTree(rootToElemsByParent(renderState, renderState.state.ctx.cachedDom!!.domRoot))
    println("===== End Render tree =====")
}

private fun rootToElemsByParent(state: RenderedState, root: BaseBpmnRenderElement): Map<String, Set<String>> {
    val elemMap = state.state.elemMap
    val front = mutableSetOf(root)
    val result = mutableMapOf<String, MutableSet<String>>()
    while (front.isNotEmpty()) {
        front.forEach { result.computeIfAbsent(it.parents.firstOrNull()?.bpmnElementId?.id ?: "") { mutableSetOf() }.add(it.bpmnElementId.id) }
        val newFront = front.flatMap { it.children }.map { elemMap[it.elementId] }.filterIsInstance<BaseBpmnRenderElement>()
        front.clear()
        front.addAll(newFront)
    }

    return result
}

private fun dumpTree(elemsByParent: Map<String, Set<String>>) {
    val childElems = elemsByParent.values.flatten().toSet()
    val roots = elemsByParent.keys.filter { !childElems.contains(it) }.toSet()
    println("{")
    dumpTree(roots, elemsByParent)
    println("}")
}

private fun dumpTree(front: Set<String>, elemsByParent: Map<String, Set<String>>, prefix: Int = 4) {
    fun separatorIfNeeded(ind: Int) = if (ind != front.size - 1) "," else ""
    val prefixStr = " ".repeat(prefix)
    front.forEachIndexed { ind, elem ->
        val children = elemsByParent[elem]
        if (null != children) {
            println("$prefixStr\"$elem\":")
            println("$prefixStr{")
            dumpTree(children, elemsByParent, prefix + 4)
            println("$prefixStr}${separatorIfNeeded(ind)}")
        } else {
            println("$prefixStr\"$elem\"${separatorIfNeeded(ind)}")
        }

    }
}
