package com.valb3r.bpmn.intellij.plugin.core.debugger

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import java.util.concurrent.atomic.AtomicReference


private val bpmnDebugger = AtomicReference<BpmnDebugger>()

fun prepareDebugger(debugger: BpmnDebugger) {
    bpmnDebugger.set(debugger)
}

fun detachDebugger() {
    bpmnDebugger.set(null)
}

fun currentDebugger(): BpmnDebugger? {
    return bpmnDebugger.get()
}

interface BpmnDebugger {
    fun executionSequence(processId: String): ExecutedElements?
}

data class ExecutedElements(val history: List<BpmnElementId>)