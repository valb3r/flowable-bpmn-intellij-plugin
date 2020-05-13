package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId

interface WithBpmnId {
    val id: BpmnElementId
}