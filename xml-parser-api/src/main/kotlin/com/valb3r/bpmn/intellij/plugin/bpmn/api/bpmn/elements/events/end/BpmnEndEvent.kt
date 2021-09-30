package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnEndEvent(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val errorEventDefinition: ErrorEventDefinition?,
        val escalationEventDefinition: EscalationEventDefinition?,
        val cancelEventDefinition: CancelEventDefinition?,
        val terminateEventDefinition: TerminateEventDefinition?
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }

    data class ErrorEventDefinition(
            val errorRef: String? = null
    )

    data class EscalationEventDefinition(
            val escalationRef: String? = null
    )

    data class CancelEventDefinition(
            val cancelRef: String? = null
    )

    data class TerminateEventDefinition(
            val terminateAll: Boolean? = null
    )
}