package com.valb3r.bpmn.intellij.plugin.core.debugger

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.core.id
import java.util.concurrent.ConcurrentHashMap


private val bpmnDebugger = ConcurrentHashMap<String, BpmnDebugger>()

fun prepareDebugger(project: Project, debugger: BpmnDebugger) {
    bpmnDebugger[project.id()] = debugger
}

fun detachDebugger(project: Project) {
    bpmnDebugger.remove(project.id())
}

fun currentDebugger(project: Project): BpmnDebugger? {
    return bpmnDebugger[project.id()]
}

interface BpmnDebugger {
    fun executionSequence(project: Project, processId: String): ExecutedElements?
}

data class ExecutedElements(val history: List<BpmnElementId>)