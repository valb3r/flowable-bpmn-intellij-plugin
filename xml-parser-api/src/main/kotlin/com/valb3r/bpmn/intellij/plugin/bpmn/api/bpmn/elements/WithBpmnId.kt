package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId

@JsonTypeInfo(
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
)
interface WithBpmnId {
    val id: BpmnElementId

    fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId
}