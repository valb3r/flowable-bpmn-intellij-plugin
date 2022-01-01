package com.valb3r.bpmn.intellij.activiti.plugin.schemas

import com.valb3r.bpmn.intellij.plugin.core.schemas.BpmnFileSchemasProvider


class ActivitiBpmnFileSchemasProvider: BpmnFileSchemasProvider() {

    override val BPMN20Schemas: Map<String, String>
        get() = super.BPMN20Schemas + ("http://activiti.org/bpmn" to "/xsds/activiti-6.0.xsd")
}