package com.valb3r.bpmn.intellij.plugin.core.render

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseBpmnRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseDiagramRenderElement

fun dumpRenderTree(project: Project) {
    val renderState = lastRenderedState(project)!!
    println("===== Mapped XML tree: =====")
    val parentToElems = renderState.state.currentState.elementByBpmnId.entries
        .groupBy { it.value.parent.id }
        .mapValues { entry -> LinkedHashSet(entry.value.map { it.key.id }) }
    dumpTree(renderState.elementsById, parentToElems)
    println("=====   End XML tree   =====")

    println("=====   Render tree:  =====")
    dumpTree(renderState.elementsById, rootToElemsByParent(renderState, renderState.state.ctx.cachedDom!!.domRoot))
    println("===== End Render tree =====")
}

private fun rootToElemsByParent(state: RenderedState, root: BaseBpmnRenderElement): Map<String, Set<String>> {
    val elemMap = state.state.elemMap
    val front = linkedSetOf(root)
    val result = linkedMapOf<String, MutableSet<String>>()
    while (front.isNotEmpty()) {
        front.forEach { result.computeIfAbsent(it.parents.firstOrNull()?.bpmnElementId?.id ?: "") { mutableSetOf() }.add(it.bpmnElementId.id) }
        val newFront = front.flatMap { it.children }.map { elemMap[it.elementId] }.filterIsInstance<BaseBpmnRenderElement>()
        front.clear()
        front.addAll(newFront)
    }

    return result
}

private fun dumpTree(elementsById: Map<BpmnElementId, BaseDiagramRenderElement>, elemsByParent: Map<String, Set<String>>) {
    val childElems = LinkedHashSet(elemsByParent.values.flatten())
    val roots = LinkedHashSet(elemsByParent.keys.filter { !childElems.contains(it) })
    if (roots.isEmpty()) {
        println("Cyclic structure detected, none of ${elemsByParent.keys} is standalone root")
        elemsByParent.keys.forEach { possibleRoot ->
            elemsByParent.forEach { (k, v) ->
                if (v.contains(possibleRoot)) {
                    println("Possible root '$possibleRoot' is child of '$k'")
                }
            }
        }
    }
    println("{")
    dumpTree(elementsById, roots, elemsByParent)
    println("}")
}

private fun dumpTree(elementsById: Map<BpmnElementId, BaseDiagramRenderElement>, front: Set<String>, elemsByParent: Map<String, Set<String>>, prefix: Int = 4) {
    fun separatorIfNeeded(ind: Int) = if (ind != front.size - 1) "," else ""
    val prefixStr = " ".repeat(prefix)
    front.forEachIndexed { ind, elem ->
        val children = elemsByParent[elem]
        val diagramElem = elementsById[BpmnElementId(elem)]
        val clazz = if (null != diagramElem) diagramElem::class.simpleName else null
        if (null != children) {
            println("$prefixStr\"$elem [$clazz] (diagram id: ${diagramElem?.elementId?.id})\":")
            println("$prefixStr{")
            dumpTree(elementsById, children, elemsByParent, prefix + 4)
            println("$prefixStr}${separatorIfNeeded(ind)}")
        } else {
            println("$prefixStr\"$elem [$clazz] (diagram id: ${diagramElem?.elementId?.id})\"${separatorIfNeeded(ind)}")
        }

    }
}
