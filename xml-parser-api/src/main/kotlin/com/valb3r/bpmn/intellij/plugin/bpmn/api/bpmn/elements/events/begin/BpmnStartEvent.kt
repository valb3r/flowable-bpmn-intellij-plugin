package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionFormProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnStartEventAlike

data class BpmnStartEvent(
    override val id: BpmnElementId,
    val name: String? = null,
    val documentation: String? = null,
    val asyncBefore: Boolean? = null,
    val asyncAfter: Boolean? = null,
    val timerEventDefinition: TimerEventDefinition? = null,
    val signalEventDefinition: SignalEventDefinition? = null,
    val messageEventDefinition: MessageEventDefinition? = null,
    val errorEventDefinition: ErrorEventDefinition? = null,
    val escalationEventDefinition: EscalationEventDefinition? = null,
    val conditionalEventDefinition: ConditionalEventDefinition? = null,
    val incoming: List<String>? = null,
    val outgoing: List<String>? = null,
    /* BPMN engine specific extensions (intermediate storage) */
    val extensionElements: List<ExtensionElement>? = null,
    /* Flattened extensionElements, for explicitness - these are the target of binding */
    val formPropertiesExtension: List<ExtensionFormProperty>? = null,
    val executionListener: List<ExeсutionListener>? = null
) : WithBpmnId, BpmnStartEventAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }


    data class TimerEventDefinition(
            val timeDate: String?,
            val timeDuration: String?,
            val timeCycle: String?,
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