package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*

// TODO - move to some implementation module
data class BpmnProcessObject(val process: BpmnProcess, val diagram: List<DiagramElement>) {

    private val mapper = ObjectMapper()

    fun toView() : BpmnProcessObjectView {
        val elementByDiagramId = mutableMapOf<DiagramElementId, BpmnElementId>()
        val elementByStaticId = mutableMapOf<BpmnElementId, WithId>()
        val propertiesById = mutableMapOf<BpmnElementId, MutableMap<PropertyType, Property>>()

        fillForStartEvent(process.startEvent, elementByStaticId, propertiesById)
        fillForEndEvent(process.endEvent, elementByStaticId, propertiesById)
        process.callActivity?.forEach { fillForCallActivity(it, elementByStaticId, propertiesById) }
        process.serviceTask?.forEach { fillForServiceTask(it, elementByStaticId, propertiesById) }
        process.sequenceFlow?.forEach { fillForSequenceFlow(it, elementByStaticId, propertiesById) }
        process.exclusiveGateway?.forEach { fillForExclusiveGateway(it, elementByStaticId, propertiesById) }

        diagram.firstOrNull()
                ?.bpmnPlane
                ?.bpmnEdge
                ?.filter { null != it.bpmnElement }
                ?.forEach { elementByDiagramId[it.id] = it.bpmnElement!! }

        diagram.firstOrNull()
                ?.bpmnPlane
                ?.bpmnShape
                ?.forEach { elementByDiagramId[it.id] = it.bpmnElement }

        return BpmnProcessObjectView(
                process,
                elementByDiagramId,
                elementByStaticId,
                propertiesById,
                diagram
        )
    }

    private fun fillForStartEvent(
            activity: BpmnStartEvent,
            elementById: MutableMap<BpmnElementId, WithId>,
            propertiesByElemType: MutableMap<BpmnElementId, MutableMap<PropertyType, Property>>) {
        elementById[activity.id] = activity
        propertiesByElemType[activity.id] = processDtoToPropertyMap(activity)
    }

    private fun fillForEndEvent(
            activity: BpmnEndEvent,
            elementById: MutableMap<BpmnElementId, WithId>,
            propertiesByElemType: MutableMap<BpmnElementId, MutableMap<PropertyType, Property>>) {
        elementById[activity.id] = activity
        propertiesByElemType[activity.id] = processDtoToPropertyMap(activity)
    }

    private fun fillForCallActivity(
            activity: BpmnCallActivity,
            elementById: MutableMap<BpmnElementId, WithId>,
            propertiesByElemType: MutableMap<BpmnElementId, MutableMap<PropertyType, Property>>) {
        elementById[activity.id] = activity
        val properties = processDtoToPropertyMap(activity)
        // TODO: handle extension elements
        propertiesByElemType[activity.id] = properties
    }

    private fun fillForServiceTask(
            activity: BpmnServiceTask,
            elementById: MutableMap<BpmnElementId, WithId>,
            propertiesByElemType: MutableMap<BpmnElementId, MutableMap<PropertyType, Property>>) {
        elementById[activity.id] = activity
        propertiesByElemType[activity.id] = processDtoToPropertyMap(activity)
    }

    private fun fillForSequenceFlow(
            activity: BpmnSequenceFlow,
            elementById: MutableMap<BpmnElementId, WithId>,
            propertiesByElemType: MutableMap<BpmnElementId, MutableMap<PropertyType, Property>>) {
        elementById[activity.id] = activity
        if (null != activity.conditionExpression && activity.conditionExpression.type != "tFormalExpression") {
            throw IllegalArgumentException("Unknown type: ${activity.conditionExpression.type}")
        }

        propertiesByElemType[activity.id] = processDtoToPropertyMap(activity)
    }

    private fun fillForExclusiveGateway(
            activity: BpmnExclusiveGateway,
            elementById: MutableMap<BpmnElementId, WithId>,
            propertiesByElemType: MutableMap<BpmnElementId, MutableMap<PropertyType, Property>>) {
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

            this[split[1]]?.parseValue(result, type)
        }
    }

    private fun JsonNode.parseValue(result: MutableMap<PropertyType, Property>, type: PropertyType) {
        result[type] = when (type.valueType) {
            STRING, CLASS, EXPRESSION -> if (this.isNull) Property(null) else Property(this.asText())
            BOOLEAN -> Property(this.asBoolean())
        }
    }
}

data class BpmnProcessObjectView(
        val process: BpmnProcess,
        val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
        val elementByStaticId: Map<BpmnElementId, WithId>,
        val elemPropertiesByElementId: Map<BpmnElementId, Map<PropertyType, Property>>,
        val diagram: List<DiagramElement>
)