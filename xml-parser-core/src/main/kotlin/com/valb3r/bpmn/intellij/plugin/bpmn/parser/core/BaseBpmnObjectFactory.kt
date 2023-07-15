package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ConditionExpression
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.exceptions.IgnorableParserException
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.*
import java.util.*
import kotlin.reflect.KClass

abstract class BaseBpmnObjectFactory : BpmnObjectFactory {
    private val mapper = ObjectMapper()

    override fun <T : WithBpmnId> newBpmnObject(clazz: KClass<T>): T {
        val result: WithBpmnId = when(clazz) {
            BpmnStartEvent::class -> BpmnStartEvent(generateBpmnId())
            BpmnStartConditionalEvent::class -> BpmnStartConditionalEvent(generateBpmnId(), conditionalEventDefinition = BpmnConditionalEventDefinition(null))
            BpmnStartEscalationEvent::class -> BpmnStartEscalationEvent(generateBpmnId(), escalationEventDefinition = BpmnEscalationEventDefinition(null))
            BpmnStartErrorEvent::class -> BpmnStartErrorEvent(generateBpmnId(), errorEventDefinition = BpmnErrorEventDefinition(null))
            BpmnStartMessageEvent::class -> BpmnStartMessageEvent(generateBpmnId(), messageEventDefinition = BpmnMessageEventDefinition(null))
            BpmnStartSignalEvent::class -> BpmnStartSignalEvent(generateBpmnId())
            BpmnStartTimerEvent::class -> BpmnStartTimerEvent(generateBpmnId(), timerEventDefinition = BpmnTimerEventDefinition(null, null, null))
            BpmnBoundaryCancelEvent::class -> BpmnBoundaryCancelEvent(generateBpmnId())
            BpmnBoundaryCompensationEvent::class -> BpmnBoundaryCompensationEvent(generateBpmnId())
            BpmnBoundaryConditionalEvent::class -> BpmnBoundaryConditionalEvent(generateBpmnId(), conditionalEventDefinition = BpmnConditionalEventDefinition(null))
            BpmnBoundaryErrorEvent::class -> BpmnBoundaryErrorEvent(generateBpmnId(), errorEventDefinition = BpmnErrorEventDefinition(null))
            BpmnBoundaryEscalationEvent::class -> BpmnBoundaryEscalationEvent(generateBpmnId())
            BpmnBoundaryMessageEvent::class -> BpmnBoundaryMessageEvent(generateBpmnId(), messageEventDefinition = BpmnMessageEventDefinition(null))
            BpmnBoundarySignalEvent::class -> BpmnBoundarySignalEvent(generateBpmnId())
            BpmnBoundaryTimerEvent::class -> BpmnBoundaryTimerEvent(generateBpmnId(), timerEventDefinition = BpmnTimerEventDefinition(null, null, null))
            BpmnUserTask::class -> BpmnUserTask(generateBpmnId())
            BpmnScriptTask::class -> BpmnScriptTask(generateBpmnId())
            BpmnServiceTask::class -> BpmnServiceTask(generateBpmnId())
            BpmnExternalTask::class -> BpmnExternalTask(generateBpmnId())
            BpmnSendEventTask::class -> BpmnSendEventTask(generateBpmnId())
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
            BpmnEndErrorEvent::class -> BpmnEndErrorEvent(generateBpmnId(), errorEventDefinition = BpmnErrorEventDefinition(null))
            BpmnEndEscalationEvent::class -> BpmnEndEscalationEvent(generateBpmnId())
            BpmnEndTerminateEvent::class -> BpmnEndTerminateEvent(generateBpmnId())
            BpmnIntermediateTimerCatchingEvent::class -> BpmnIntermediateTimerCatchingEvent(generateBpmnId(), timerEventDefinition = BpmnTimerEventDefinition(null, null, null))
            BpmnIntermediateMessageCatchingEvent::class -> BpmnIntermediateMessageCatchingEvent(generateBpmnId(), messageEventDefinition = BpmnMessageEventDefinition(null))
            BpmnIntermediateSignalCatchingEvent::class -> BpmnIntermediateSignalCatchingEvent(generateBpmnId())
            BpmnIntermediateConditionalCatchingEvent::class -> BpmnIntermediateConditionalCatchingEvent(generateBpmnId(), conditionalEventDefinition = BpmnConditionalEventDefinition(null))
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
            is BpmnStartEventAlike, is EndEventAlike, is BpmnBoundaryEventAlike, is BpmnTaskAlike, is BpmnGatewayAlike,
            is IntermediateCatchingEventAlike, is IntermediateThrowingEventAlike, is BpmnProcess
            -> processDtoToPropertyMap(obj)

            is BpmnStructuralElementAlike -> fillForCallActivity(obj)
            is BpmnSequenceFlow -> fillForSequenceFlow(obj)
            else -> throw IgnorableParserException("Can't parse properties of element with ID ${obj.id.id} (${obj.javaClass.simpleName})")
        }
        
        return PropertyTable(table)
    }

    abstract override fun propertyTypes(): List<PropertyType>

    protected open fun processDtoToPropertyMap(dto: Any): MutableMap<PropertyType, MutableList<Property>> {
        val result: MutableMap<PropertyType, MutableList<Property>> = mutableMapOf()
        val propertyTree = mapper.valueToTree<JsonNode>(dto)
        for (type in propertyTypes()) {
            if (type.isUsedOnlyBy.isEmpty() || type.isUsedOnlyBy.contains(dto::class)) {
                parseValue(type.path, type, propertyTree, result, 0)
            }
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

    private fun fillForCallActivity(activity: BpmnStructuralElementAlike): MutableMap<PropertyType, MutableList<Property>> {
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
            PropertyValueType.STRING, PropertyValueType.CLASS, PropertyValueType.EXPRESSION, PropertyValueType.ATTACHED_SEQUENCE_SELECT, PropertyValueType.LIST_SELECT
            -> if (node.isNull) makeProperty(null) else makeProperty(node.asText())
            PropertyValueType.BOOLEAN -> makeProperty(node.asBoolean())
        }
        result.computeIfAbsent(type) { mutableListOf() }.add(propVal)
    }

    private fun bounds(forBpmnObject: WithBpmnId): BoundsElement {
        return when (forBpmnObject) {
            is BpmnStartEventAlike, is EndEventAlike, is BpmnBoundaryEventAlike, is IntermediateThrowingEventAlike,
            is IntermediateCatchingEventAlike -> BoundsElement(
                0.0f,
                0.0f,
                30.0f,
                30.0f
            )
            is BpmnGatewayAlike -> BoundsElement(
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
