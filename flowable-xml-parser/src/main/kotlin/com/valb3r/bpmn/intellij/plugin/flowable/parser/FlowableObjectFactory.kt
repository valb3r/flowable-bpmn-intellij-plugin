package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.BaseBpmnObjectFactory

class FlowableObjectFactory: BaseBpmnObjectFactory() {

    override fun propertyTypes(): List<PropertyType> {
        return FlowablePropertyTypeDetails.values().map { it.details.propertyType }
    }
}