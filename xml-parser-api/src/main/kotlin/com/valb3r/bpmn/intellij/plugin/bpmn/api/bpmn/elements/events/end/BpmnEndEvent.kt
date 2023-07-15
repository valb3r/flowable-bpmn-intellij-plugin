package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.BpmnErrorEventDefinition
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.BpmnEscalationEventDefinition
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.EndEventAlike

data class BpmnEndEvent(
    override val id: BpmnElementId,
    val name: String? = null,
    val documentation: String? = null,
    val asyncBefore: Boolean? = null,
    val asyncAfter: Boolean? = null,
    val incoming: List<String>? = null,
    val outgoing: List<String>? = null,
    val errorEventDefinition: BpmnErrorEventDefinition? = null,
    val escalationEventDefinition: BpmnEscalationEventDefinition? = null,
    val cancelEventDefinition: CancelEventDefinition? = null,
    val terminateEventDefinition: TerminateEventDefinition? = null
): WithBpmnId, EndEventAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }

    data class CancelEventDefinition(
            val cancelRef: String? = null
    )

    data class TerminateEventDefinition(
            val terminateAll: Boolean? = null
    )
}