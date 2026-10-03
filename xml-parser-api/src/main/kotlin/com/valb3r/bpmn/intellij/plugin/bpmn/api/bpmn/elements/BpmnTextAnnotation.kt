package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId

data class BpmnTextAnnotation(
    override val id: BpmnElementId,
    val text: TextAnnotationText? = null,
    val textFormat: String? = null,
) : WithBpmnId {
    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId = copy(id = newId)
}

data class TextAnnotationText(val text: String? = null)
