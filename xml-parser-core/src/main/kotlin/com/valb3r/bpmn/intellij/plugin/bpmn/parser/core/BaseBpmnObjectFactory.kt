package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ConditionExpression
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.*
import java.util.*
import kotlin.reflect.KClass

abstract class BaseBpmnObjectFactory : BpmnObjectFactory {
    private val mapper = ObjectMapper()

    override fun <T : WithBpmnId> newBpmnObject(clazz: KClass<T>): T {
        val result: WithBpmnId = when(clazz) {
            BpmnStartEvent::class -> BpmnStartEvent(generateBpmnId())
            BpmnStartConditionalEvent::class -> BpmnStartConditionalEvent(generateBpmnId())
            BpmnStartEscalationEvent::class -> BpmnStartEscalationEvent(generateBpmnId())
            BpmnStartErrorEvent::class -> BpmnStartErrorEvent(generateBpmnId())
            BpmnStartMessageEvent::class -> BpmnStartMessageEvent(generateBpmnId())
            BpmnStartSignalEvent::class -> BpmnStartSignalEvent(generateBpmnId())
            BpmnStartTimerEvent::class -> BpmnStartTimerEvent(generateBpmnId())
            BpmnBoundaryCancelEvent::class -> BpmnBoundaryCancelEvent(generateBpmnId())
            BpmnBoundaryCompensationEvent::class -> BpmnBoundaryCompensationEvent(generateBpmnId())
            BpmnBoundaryConditionalEvent::class -> BpmnBoundaryConditionalEvent(generateBpmnId())
            BpmnBoundaryErrorEvent::class -> BpmnBoundaryErrorEvent(generateBpmnId())
            BpmnBoundaryEscalationEvent::class -> BpmnBoundaryEscalationEvent(generateBpmnId())
            BpmnBoundaryMessageEvent::class -> BpmnBoundaryMessageEvent(generateBpmnId())
            BpmnBoundarySignalEvent::class -> BpmnBoundarySignalEvent(generateBpmnId())
            BpmnBoundaryTimerEvent::class -> BpmnBoundaryTimerEvent(generateBpmnId())
            BpmnUserTask::class -> BpmnUserTask(generateBpmnId())
            BpmnScriptTask::class -> BpmnScriptTask(generateBpmnId())
            BpmnServiceTask::class -> BpmnServiceTask(generateBpmnId())
            BpmnBusinessRuleTask::class -> BpmnBusinessRuleTask(generateBpmnId())
            BpmnReceiveTask::class -> BpmnReceiveTask(generateBpmnId())
            BpmnManualTask::class -> BpmnManualTask(generateBpmnId())
            BpmnCamelTask::class -> BpmnCamelTask(generateBpmnId())
            BpmnHttpTask::class -> BpmnHttpTask(generateBpmnId())
            BpmnMailTask::class -> BpmnMailTask(generateBpmnId())
            BpmnMuleTask::class -> BpmnMuleTask(generateBpmnId())
            BpmnDecisionTask::class -> BpmnDecisionTask(generateBpmnId())
            BpmnShellTask::class -> BpmnShellTask(generateBpmnId())
            BpmnSubProcess::class -> BpmnSubProcess(generateBpmnId(), triggeredByEvent = false, transactionalSubprocess = false)
            BpmnEventSubprocess::class -> BpmnEventSubprocess(generateBpmnId(), triggeredByEvent = true)
            BpmnTransactionalSubProcess::class -> BpmnTransactionalSubProcess(generateBpmnId(), transactionalSubprocess = true)
            BpmnCallActivity::class -> BpmnCallActivity(generateBpmnId(), "")
            BpmnAdHocSubProcess::class -> BpmnAdHocSubProcess(generateBpmnId(), completionCondition = CompletionCondition(null))
            BpmnExclusiveGateway::class -> BpmnExclusiveGateway(generateBpmnId())
            BpmnParallelGateway::class -> BpmnParallelGateway(generateBpmnId())
            BpmnInclusiveGateway::class -> BpmnInclusiveGateway(generateBpmnId())
            BpmnEventGateway::class -> BpmnEventGateway(generateBpmnId())
            BpmnEndEvent::class -> BpmnEndEvent(generateBpmnId())
            BpmnEndCancelEvent::class -> BpmnEndCancelEvent(generateBpmnId())
            BpmnEndErrorEvent::class -> BpmnEndErrorEvent(generateBpmnId())
            BpmnEndEscalationEvent::class -> BpmnEndEscalationEvent(generateBpmnId())
            BpmnEndTerminateEvent::class -> BpmnEndTerminateEvent(generateBpmnId())
            BpmnIntermediateTimerCatchingEvent::class -> BpmnIntermediateTimerCatchingEvent(generateBpmnId())
            BpmnIntermediateMessageCatchingEvent::class -> BpmnIntermediateMessageCatchingEvent(generateBpmnId())
            BpmnIntermediateSignalCatchingEvent::class -> BpmnIntermediateSignalCatchingEvent(generateBpmnId())
            BpmnIntermediateConditionalCatchingEvent::class -> BpmnIntermediateConditionalCatchingEvent(generateBpmnId())
            BpmnIntermediateNoneThrowingEvent::class -> BpmnIntermediateNoneThrowingEvent(generateBpmnId())
            BpmnIntermediateSignalThrowingEvent::class -> BpmnIntermediateSignalThrowingEvent(generateBpmnId())
            BpmnIntermediateEscalationThrowingEvent::class -> BpmnIntermediateEscalationThrowingEvent(generateBpmnId())
            else -> throw IllegalArgumentException("Can't create class: " + clazz.qualifiedName)
        }

        return result as T
    }

    override fun <T : WithDiagramId> newDiagramObject(clazz: KClass<T>, forBpmnObject: WithBpmnId): T {
        val result: WithDiagramId = when (clazz) {
            EdgeElement::class -> EdgeElement(
                DiagramElementId("edge-" + UUID.randomUUID().toString()),
                forBpmnObject.id,
                null
            )
            ShapeElement::class -> ShapeElement(
                DiagramElementId("shape-" + UUID.randomUUID().toString()),
                forBpmnObject.id,
                bounds(forBpmnObject)
            )
            else -> throw IllegalArgumentException("Can't create class: " + clazz.qualifiedName)
        }

        return result as T
    }

    override fun <T : WithBpmnId> newOutgoingSequence(sourceRef: T): BpmnSequenceFlow {
        return when (sourceRef) {
            is BpmnExclusiveGateway, is BpmnParallelGateway, is BpmnInclusiveGateway -> BpmnSequenceFlow(
                generateBpmnId(),
                null,
                null,
                sourceRef.id.id,
                "",
                ConditionExpression("tFormalExpression", "")
            )
            else -> BpmnSequenceFlow(generateBpmnId(), null, null, sourceRef.id.id, "", null)
        }
    }

    override fun <T : WithBpmnId> propertiesOf(obj: T): PropertyTable {
        val table = when (obj) {
            is BpmnStartEvent, is BpmnStartTimerEvent, is BpmnStartSignalEvent, is BpmnStartMessageEvent,
            is BpmnStartErrorEvent, is BpmnStartEscalationEvent, is BpmnStartConditionalEvent, is BpmnEndEvent,
            is BpmnEndErrorEvent, is BpmnEndCancelEvent, is BpmnEndEscalationEvent,
            is BpmnEndTerminateEvent, is BpmnBoundaryCancelEvent, is BpmnBoundaryCompensationEvent,
            is BpmnBoundaryConditionalEvent, is BpmnBoundaryEscalationEvent, is BpmnBoundaryMessageEvent, is BpmnBoundaryErrorEvent,
            is BpmnBoundarySignalEvent, is BpmnBoundaryTimerEvent,
            is BpmnTask, is BpmnUserTask, is BpmnScriptTask, is BpmnServiceTask, is BpmnBusinessRuleTask,
            is BpmnSendTask, is BpmnReceiveTask, is BpmnCamelTask, is BpmnHttpTask, is BpmnMuleTask, is BpmnDecisionTask, is BpmnShellTask, is BpmnMailTask,
            is BpmnManualTask,
            is BpmnSubProcess, is BpmnEventSubprocess, is BpmnTransactionalSubProcess, is BpmnAdHocSubProcess, is BpmnCollapsedSubprocess, is BpmnTransactionCollapsedSubprocess,
            is BpmnExclusiveGateway, is BpmnParallelGateway, is BpmnInclusiveGateway, is BpmnEventGateway, is BpmnComplexGateway,
            is BpmnIntermediateTimerCatchingEvent, is BpmnIntermediateMessageCatchingEvent, is BpmnIntermediateSignalCatchingEvent, is BpmnIntermediateConditionalCatchingEvent,
            is BpmnIntermediateNoneThrowingEvent, is BpmnIntermediateSignalThrowingEvent, is BpmnIntermediateEscalationThrowingEvent, is BpmnIntermediateLinkCatchingEvent,
            is BpmnProcess,
            is BpmnCollaboration, is BpmnParticipant, is BpmnMessageFlow
            -> processDtoToPropertyMap(obj)

            is BpmnCallActivity -> fillForCallActivity(obj)
            is BpmnSequenceFlow -> fillForSequenceFlow(obj)
            else -> throw IllegalArgumentException("Can't parse properties of: ${obj.javaClass}")
        }
        
        return PropertyTable(table)
    }

    abstract override fun propertyTypes(): List<PropertyType>

    protected open fun processDtoToPropertyMap(dto: Any): MutableMap<PropertyType, MutableList<Property>> {
        val result: MutableMap<PropertyType, MutableList<Property>> = mutableMapOf()
        val propertyTree = mapper.valueToTree<JsonNode>(dto)

        for (type in propertyTypes()) {
            parseValue(type.path, type, propertyTree, result, 0)
        }

        return result
    }

    protected open fun fillForSequenceFlow(activity: BpmnSequenceFlow): MutableMap<PropertyType, MutableList<Property>> {
        verifyConditionalExpressionInSequenceFlow(activity)
        return processDtoToPropertyMap(activity)
    }

    protected open fun verifyConditionalExpressionInSequenceFlow(activity: BpmnSequenceFlow) {
        if (
            null != activity.conditionExpression
            && null != activity.conditionExpression!!.type
            && activity.conditionExpression!!.type != "tFormalExpression"
            && activity.conditionExpression!!.type != "bpmn:tFormalExpression" // FIXME It is actually a hack
        ) {
            throw IllegalArgumentException("Unknown type: ${activity.conditionExpression!!.type}")
        }
    }

    private fun fillForCallActivity(activity: BpmnCallActivity): MutableMap<PropertyType, MutableList<Property>> {
        val properties = processDtoToPropertyMap(activity)
        // TODO: handle extension elements
        return properties
    }

    private fun parseValue(
        path: String,
        type: PropertyType,
        propertyTree: JsonNode,
        result: MutableMap<PropertyType, MutableList<Property>>,
        arrayIndexDepth: Int,
        indexInArray: List<String>? = null
    ) {
        val split = path.split(".", limit = 2)
        val targetId = if (null != indexInArray) split[0].substring(1) else split[0]

        val node = propertyTree[targetId] ?: return

        if (node.isArray) {
            if (node.isEmpty) {
                doParse(NullNode.instance, result, type, indexInArray = listOf())
                return
            }
            node.forEach {
                val indexValue = when (val indexKey = type.indexInGroupArrayName!!.split(".")[arrayIndexDepth]) {
                    "@" -> it.asText()
                    else -> it[indexKey].asText()
                }

                val index = if (null != indexValue) ((indexInArray ?: listOf()) + indexValue) else indexInArray
                if (split.size < 2) {
                    doParse(it, result, type, indexInArray = index)
                } else {
                    parseValue(split[1], type, it, result, arrayIndexDepth = arrayIndexDepth + 1, indexInArray = index)
                }
            }
            return
        }

        if (split.size < 2) {
            doParse(node, result, type, indexInArray = indexInArray)
            return
        }

        if (split[1].contains(".")) {
            parseValue(split[1], type, node, result, indexInArray = indexInArray, arrayIndexDepth = arrayIndexDepth)
            return
        }

        val value = node[split[1]]
        doParse(value, result, type, indexInArray = indexInArray)
    }

    private fun doParse(
        node: JsonNode?,
        result: MutableMap<PropertyType, MutableList<Property>>,
        type: PropertyType,
        indexInArray: List<String>? = null
    ) {
        val makeProperty = {it: Any? ->
            if (null != indexInArray) {
                val minPathIdLen = type.indexInGroupArrayName!!.split(".").size
                val computedIndex = indexInArray + (0 until minPathIdLen - indexInArray.size).map { "" }.toList()
                Property(it, computedIndex)
            } else Property(it)
        }

        if (null == node || node.isNull) {
            result.computeIfAbsent(type) { mutableListOf() }.add(makeProperty(type.defaultValueIfNull))
            return
        }

        val propVal = when (type.valueType) {
            PropertyValueType.STRING, PropertyValueType.CLASS, PropertyValueType.EXPRESSION, PropertyValueType.ATTACHED_SEQUENCE_SELECT
            -> if (node.isNull) makeProperty(null) else makeProperty(node.asText())
            PropertyValueType.BOOLEAN -> makeProperty(node.asBoolean())
        }
        result.computeIfAbsent(type) { mutableListOf() }.add(propVal)
    }

    private fun bounds(forBpmnObject: WithBpmnId): BoundsElement {
        return when (forBpmnObject) {
            is BpmnStartEvent, is BpmnStartEscalationEvent, is BpmnStartConditionalEvent, is BpmnStartErrorEvent,
            is BpmnStartMessageEvent, is BpmnStartSignalEvent, is BpmnStartTimerEvent, is BpmnEndEvent,
            is BpmnEndTerminateEvent, is BpmnEndEscalationEvent, is BpmnBoundaryCancelEvent, is BpmnBoundaryCompensationEvent,
            is BpmnBoundaryConditionalEvent, is BpmnBoundaryEscalationEvent, is BpmnBoundaryMessageEvent, is BpmnBoundaryErrorEvent,
            is BpmnBoundarySignalEvent, is BpmnBoundaryTimerEvent, is BpmnEndErrorEvent,
            is BpmnEndCancelEvent, is BpmnIntermediateTimerCatchingEvent, is BpmnIntermediateMessageCatchingEvent,
            is BpmnIntermediateSignalCatchingEvent, is BpmnIntermediateConditionalCatchingEvent, is BpmnIntermediateNoneThrowingEvent,
            is BpmnIntermediateSignalThrowingEvent, is BpmnIntermediateEscalationThrowingEvent, is BpmnIntermediateLinkCatchingEvent -> BoundsElement(
                0.0f,
                0.0f,
                30.0f,
                30.0f
            )
            is BpmnExclusiveGateway, is BpmnParallelGateway, is BpmnInclusiveGateway, is BpmnEventGateway, is BpmnComplexGateway -> BoundsElement(
                0.0f,
                0.0f,
                40.0f,
                40.0f
            )
            else -> BoundsElement(0.0f, 0.0f, 100.0f, 80.0f)
        }
    }

    protected open fun generateBpmnId(): BpmnElementId {
        return BpmnElementId("sid-" + UUID.randomUUID().toString())
    }
}
