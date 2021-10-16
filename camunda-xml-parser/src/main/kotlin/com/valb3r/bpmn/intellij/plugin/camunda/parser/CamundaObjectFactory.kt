package com.valb3r.bpmn.intellij.plugin.camunda.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.BaseBpmnObjectFactory

class CamundaObjectFactory: BaseBpmnObjectFactory() {

    override fun propertyTypes(): List<PropertyType> {
        return CamundaPropertyTypeDetails.values().map { it.details.propertyType }
    }
}