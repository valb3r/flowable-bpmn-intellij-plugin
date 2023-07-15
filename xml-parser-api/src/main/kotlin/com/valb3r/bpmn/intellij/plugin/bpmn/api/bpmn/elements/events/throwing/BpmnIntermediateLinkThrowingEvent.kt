package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.BpmnLinkEventDefinition
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.IntermediateCatchingEventAlike
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.IntermediateThrowingEventAlike

data class BpmnIntermediateLinkThrowingEvent(
    override val id: BpmnElementId,
    val name: String? = null,
    val documentation: String? = null,
    val incoming: List<String>? = null,
    val outgoing: List<String>? = null,
    val linkEventDefinition: BpmnLinkEventDefinition? = null,
    val executionListener: List<ExeсutionListener>? = null
) : WithBpmnId, IntermediateThrowingEventAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}