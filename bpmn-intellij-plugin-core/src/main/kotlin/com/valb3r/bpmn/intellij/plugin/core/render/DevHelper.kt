package com.valb3r.bpmn.intellij.plugin.core.render

import com.intellij.openapi.project.Project

fun dumpRenderTree(project: Project) {
    val renderState = lastRenderedState(project)!!
    println("===== Mapped XML tree: =====")
    val parentToElems = renderState.state.currentState.elementByBpmnId.entries
        .groupBy { it.value.parent.id }
        .mapValues { entry -> entry.value.map { it.key.id }.toSet() }
    dumpTree(parentToElems)
    println("=====   End XML tree   =====")

    println("=====   Render tree:  =====")
    val treeElems = renderState.elementsById.entries
        .groupBy { it.value.parents.map { it.bpmnElementId.id }.firstOrNull() ?: "" }
        .mapValues { entry -> entry.value.map { it.key.id }.toSet() }
    dumpTree(treeElems)
    println("===== End Render tree =====")
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
