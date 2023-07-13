package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.IntermediateCatchingEventAlike

data class BpmnIntermediateTimerCatchingEvent(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val incoming: List<String>? = null,
        val outgoing: List<String>? = null,
        val timeDate: String? = null,
        val timeDuration: String? = null,
        val timeCycle: String? = null,
) : WithBpmnId, IntermediateCatchingEventAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}