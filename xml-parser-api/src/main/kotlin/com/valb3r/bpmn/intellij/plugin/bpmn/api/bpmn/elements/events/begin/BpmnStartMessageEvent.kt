package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.BpmnMessageEventDefinition
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnStartEventAlike

data class BpmnStartMessageEvent(
    override val id: BpmnElementId,
    val name: String? = null,
    val documentation: String? = null,
    val asyncBefore: Boolean? = null,
    val asyncAfter: Boolean? = null,
    val incoming: List<String>? = null,
    val outgoing: List<String>? = null,
    val messageEventDefinition: BpmnMessageEventDefinition? = null,
) : WithBpmnId, BpmnStartEventAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

