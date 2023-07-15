package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.*

data class BpmnIntermediateCatchingEvent(
    override val id: BpmnElementId,
    val name: String? = null,
    val documentation: String? = null,
    val timerEventDefinition: BpmnTimerEventDefinition? = null,
    val signalEventDefinition: BpmnSignalEventDefinition? = null,
    val messageEventDefinition: BpmnMessageEventDefinition? = null,
    val conditionalEventDefinition: BpmnConditionalEventDefinition? = null,
    val linkEventDefinition: BpmnLinkEventDefinition? = null,
    val incoming: List<String>? = null,
    val outgoing: List<String>? = null,
    val executionListener: List<ExeсutionListener>? = null
) : WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}