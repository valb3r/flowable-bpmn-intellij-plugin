package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.BaseBpmnObjectFactory

open class Activiti7ObjectFactory: ActivitiObjectFactory() {

    override fun propertyTypes(): List<PropertyType> {
        return Activiti7PropertyTypeDetails.values().map { it.details.propertyType }
    }

    override fun verifyConditionalExpressionInSequenceFlow(activity: BpmnSequenceFlow) {
        if (
            null != activity.conditionExpression
            && activity.conditionExpression!!.type != "tFormalExpression"
            && activity.conditionExpression!!.type != "bpmn2:tFormalExpression" // FIXME It is actually a hack
        ) {
            throw IllegalArgumentException("Unknown type: ${activity.conditionExpression!!.type}")
        }
    }
}