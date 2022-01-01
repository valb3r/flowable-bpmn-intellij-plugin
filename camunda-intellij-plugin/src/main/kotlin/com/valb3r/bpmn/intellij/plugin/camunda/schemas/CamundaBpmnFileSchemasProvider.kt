package com.valb3r.bpmn.intellij.plugin.camunda.schemas

import com.valb3r.bpmn.intellij.plugin.core.schemas.BpmnFileSchemasProvider


class CamundaBpmnFileSchemasProvider: BpmnFileSchemasProvider() {

    override val BPMN20Schemas: Map<String, String>
        get() = super.BPMN20Schemas + ("http://camunda.org/schema/1.0/bpmn" to "/xsds/camunda-1.0.xsd")
}