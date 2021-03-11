package com.valb3r.bpmn.intellij.plugin.core.debugger

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import java.util.*


private val bpmnDebugger = Collections.synchronizedMap(WeakHashMap<Project,  BpmnDebugger>())

fun prepareDebugger(project: Project, debugger: BpmnDebugger) {
    bpmnDebugger[project] = debugger
}

fun detachDebugger(project: Project) {
    bpmnDebugger.remove(project)
}

fun currentDebugger(project: Project): BpmnDebugger? {
    return bpmnDebugger[project]
}

interface BpmnDebugger {
    fun executionSequence(project: Project, processId: String): ExecutedElements?
}

data class ExecutedElements(val history: List<BpmnElementId>)