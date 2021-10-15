package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionFormProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnStartEvent(
    override val id: BpmnElementId,
    val name: String?,
    val documentation: String?,
    val timerEventDefinition: TimerEventDefinition?,
    val signalEventDefinition: SignalEventDefinition?,
    val messageEventDefinition: MessageEventDefinition?,
    val errorEventDefinition: ErrorEventDefinition?,
    val escalationEventDefinition: EscalationEventDefinition?,
    val conditionalEventDefinition: ConditionalEventDefinition?,
    /* BPMN engine specific extensions (intermediate storage) */
    val extensionElements: List<ExtensionElement>? = null,
    /* Flattened extensionElements, for explicitness - these are the target of binding */
    val formPropertiesExtension: List<ExtensionFormProperty>? = null
) : WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
    
    data class TimerEventDefinition(
            val timeDate: String? = null
    )

    data class SignalEventDefinition(
            val signalRef: String? = null
    )

    data class MessageEventDefinition(
            val messageRef: String? = null
    )

    data class ErrorEventDefinition(
            val errorRef: String? = null
    )

    data class EscalationEventDefinition(
            val escalationRef: String? = null
    )

    data class ConditionalEventDefinition(
            val condition: String? = null
    )
}