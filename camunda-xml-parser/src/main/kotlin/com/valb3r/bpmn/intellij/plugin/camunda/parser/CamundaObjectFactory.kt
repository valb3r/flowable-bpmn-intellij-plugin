package com.valb3r.bpmn.intellij.plugin.camunda.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateLinkCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnComplexGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnSendTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.BaseBpmnObjectFactory
import kotlin.reflect.KClass

class CamundaObjectFactory: BaseBpmnObjectFactory() {

    override fun <T : WithBpmnId> newBpmnObject(clazz: KClass<T>): T {
        return when(clazz) {
            BpmnTask::class -> BpmnTask(generateBpmnId())
            BpmnSendTask::class -> BpmnSendTask(generateBpmnId())
            BpmnComplexGateway::class -> BpmnComplexGateway(generateBpmnId())
            BpmnIntermediateLinkCatchingEvent::class -> BpmnIntermediateLinkCatchingEvent(generateBpmnId())
            else -> super.newBpmnObject(clazz)
        } as T
    }

    override fun propertyTypes(): List<PropertyType> {
        return CamundaPropertyTypeDetails.values().map { it.details.propertyType }
    }
}