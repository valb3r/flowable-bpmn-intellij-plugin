package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.lanes.BpmnFlowNodeRef
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import kotlin.reflect.KClass

interface BpmnObjectFactory {

    fun <T: WithBpmnId> newBpmnObject(clazz: KClass<T>): T
    fun newFlowRef(): BpmnFlowNodeRef
    fun <T: WithBpmnId> newOutgoingSequence(sourceRef: T): BpmnSequenceFlow
    fun <T> propertiesOf(obj: T):  PropertyTable
    fun <T: WithDiagramId> newDiagramObject(clazz: KClass<T>, forBpmnObject: WithBpmnId): T
    fun propertyTypes(): List<PropertyType>
}