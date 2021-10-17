package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnEventGateway(
        override val id: BpmnElementId,
        val name: String? = null,
        val defaultElement: String? = null,
        val documentation: String? = null
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

