package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import kotlin.reflect.KClass

interface BpmnObjectFactory {

    fun <T: WithBpmnId> newBpmnObject(clazz: KClass<T>): T
    fun <T: WithBpmnId> newOutgoingSequence(obj: T): BpmnSequenceFlow
    fun <T: WithBpmnId> propertiesOf(obj: T):  Map<PropertyType, Property>
    fun <T: WithDiagramId> newDiagramObject(clazz: KClass<T>, forBpmnObject: WithBpmnId): T
}