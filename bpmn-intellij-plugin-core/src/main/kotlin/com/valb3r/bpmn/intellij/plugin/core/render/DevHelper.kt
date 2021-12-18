package com.valb3r.bpmn.intellij.plugin.core.render

import com.intellij.openapi.project.Project

fun dumpRenderTree(project: Project) {
    val renderState = lastRenderedState(project)!!
    println("===== Mapped XML tree: =====")
    dumpTree(renderState.state.currentState.elementByBpmnId.entries.groupBy { it.key.id }.mapValues { it.value.first().value.id.id })
    println("=====   End XML tree   =====")

    println("=====   Render tree:  =====")
    println("===== End Render tree =====")
}

private fun dumpTree(elems: Map<String, String>) {

}
