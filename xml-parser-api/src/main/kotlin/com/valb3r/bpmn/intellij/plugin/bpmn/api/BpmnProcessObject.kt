package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*

data class BpmnProcessObject(val process: BpmnProcess, val diagram: List<DiagramElement>) {

    private val mapper = ObjectMapper()

    fun toView() : BpmnProcessObjectView {
        val elementById = mutableMapOf<String, WithId>()
        val propertiesById = mutableMapOf<String, MutableMap<PropertyType, Property>>()

        elementById[process.startEvent.id] = process.startEvent
        elementById[process.endEvent.id] = process.endEvent

        process.callActivity?.forEach { fillForCallActivity(it, elementById, propertiesById) }
        process.serviceTask?.forEach { fillForServiceTask(it, elementById, propertiesById) }
        process.sequenceFlow?.forEach { fillForSequenceFlow(it, elementById, propertiesById) }
        process.exclusiveGateway?.forEach { fillForExclusiveGateway(it, elementById, propertiesById) }

        return BpmnProcessObjectView(
                process,
                elementById,
                propertiesById,
                diagram
        )
    }

    private fun fillForCallActivity(
            activity: BpmnCallActivity,
            elementById: MutableMap<String, WithId>,
            propertiesByElemType: MutableMap<String, MutableMap<PropertyType, Property>>) {
        elementById[activity.id] = activity
        val properties = processDtoToPropertyMap(activity)
        // TODO: handle extension elements
        propertiesByElemType[activity.id] = properties
    }

    private fun fillForServiceTask(
            activity: BpmnServiceTask,
            elementById: MutableMap<String, WithId>,
            propertiesByElemType: MutableMap<String, MutableMap<PropertyType, Property>>) {
        elementById[activity.id] = activity
        propertiesByElemType[activity.id] = processDtoToPropertyMap(activity)
    }

    private fun fillForSequenceFlow(
            activity: BpmnSequenceFlow,
            elementById: MutableMap<String, WithId>,
            propertiesByElemType: MutableMap<String, MutableMap<PropertyType, Property>>) {
        elementById[activity.id] = activity
        if (null != activity.conditionExpression && activity.conditionExpression.type != "tFormalExpression") {
            throw IllegalArgumentException("Unknown type: ${activity.conditionExpression.type}")
        }

        propertiesByElemType[activity.id] = processDtoToPropertyMap(activity)
    }

    private fun fillForExclusiveGateway(
            activity: BpmnExclusiveGateway,
            elementById: MutableMap<String, WithId>,
            propertiesByElemType: MutableMap<String, MutableMap<PropertyType, Property>>) {
        elementById[activity.id] = activity
        propertiesByElemType[activity.id] = processDtoToPropertyMap(activity)
    }

    private fun processDtoToPropertyMap(dto: Any): MutableMap<PropertyType, Property> {
        val result: MutableMap<PropertyType, Property> = mutableMapOf()
        val propertyTree = mapper.valueToTree<JsonNode>(dto)

        for (type in PropertyType.values()) {
            if (type.id.contains(".")) {
                tryParseNestedValue(type, propertyTree, result)
                continue
            }

            propertyTree[type.id]?.apply {
                parseValue(result, type)
            }
        }

        return result
    }

    private fun tryParseNestedValue(type: PropertyType, propertyTree: JsonNode, result: MutableMap<PropertyType, Property>) {
        val split = type.id.split(".", limit = 2)
        val targetId = split[0]
        propertyTree[targetId]?.apply {
            if (split[1].contains(".")) {
                tryParseNestedValue(type, this, result)
            }

            parseValue(result, type)
        }
    }

    private fun JsonNode.parseValue(result: MutableMap<PropertyType, Property>, type: PropertyType) {
        result[type] = when (type.valueType) {
            STRING, CLASS, EXPRESSION -> Property(this.asText())
            BOOLEAN -> Property(this.asBoolean())
        }
    }
}

data class BpmnProcessObjectView(
        val process: BpmnProcess,
        val elementById: Map<String, WithId>,
        val elemPropertiesByElementId: Map<String, Map<PropertyType, Property>>,
        val diagram: List<DiagramElement>
)