package com.valb3r.bpmn.intellij.plugin.flowable.schemas

import com.valb3r.bpmn.intellij.plugin.core.schemas.BpmnFileSchemasProvider


class FlowableBpmnFileSchemasProvider: BpmnFileSchemasProvider() {

    override val BPMN20Schemas: Map<String, String>
        get() = super.BPMN20Schemas + ("http://flowable.org/bpmn" to "/xsds/flowable-6.0.xsd")
}