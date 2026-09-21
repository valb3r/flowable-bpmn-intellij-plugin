package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId

data class BpmnAssociation(
    override val id: BpmnElementId,
    val sourceRef: String? = null,
    val targetRef: String? = null,
    val associationDirection: String? = null,
) : WithBpmnId {
    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId = copy(id = newId)
}
