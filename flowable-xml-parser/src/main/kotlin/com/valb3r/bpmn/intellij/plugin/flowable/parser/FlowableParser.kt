package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateConditionalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateMessageCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateSignalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateTimerCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnEventGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnInclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnParallelGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnAdHocSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnEventSubprocess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionalSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnFile
import org.dom4j.*
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.awt.geom.Point2D
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets


const val CDATA_FIELD = "CDATA"

enum class PropertyTypeDetails(
        val propertyType: PropertyType,
        val xmlPath: String,
        val type: XmlType
) {
    ID(PropertyType.ID, "id", XmlType.ATTRIBUTE),
    NAME(PropertyType.NAME,"name", XmlType.ATTRIBUTE),
    DOCUMENTATION(PropertyType.DOCUMENTATION, "documentation.text", XmlType.CDATA),
    IS_FOR_COMPENSATION(PropertyType.IS_FOR_COMPENSATION, "isForCompensation", XmlType.ATTRIBUTE),
    ASYNC(PropertyType.ASYNC, "flowable:async", XmlType.ATTRIBUTE),
    ASSIGNEE(PropertyType.ASSIGNEE, "flowable:assignee", XmlType.ATTRIBUTE),
    CALLED_ELEM(PropertyType.CALLED_ELEM, "calledElement", XmlType.ATTRIBUTE),
    CALLED_ELEM_TYPE(PropertyType.CALLED_ELEM_TYPE, "flowable:calledElementType", XmlType.ATTRIBUTE),
    INHERIT_VARS(PropertyType.INHERIT_VARS, "flowable:inheritVariables", XmlType.ATTRIBUTE),
    FALLBACK_TO_DEF_TENANT(PropertyType.FALLBACK_TO_DEF_TENANT, "flowable:fallbackToDefaultTenant", XmlType.ATTRIBUTE),
    EXCLUSIVE(PropertyType.EXCLUSIVE,"flowable:exclusive", XmlType.ATTRIBUTE),
    EXPRESSION(PropertyType.EXPRESSION, "flowable:expression", XmlType.ATTRIBUTE),
    DELEGATE_EXPRESSION(PropertyType.DELEGATE_EXPRESSION, "flowable:delegateExpression", XmlType.ATTRIBUTE),
    CLASS(PropertyType.CLASS, "flowable:class", XmlType.ATTRIBUTE),
    SKIP_EXPRESSION(PropertyType.SKIP_EXPRESSION, "flowable:skipExpression", XmlType.ATTRIBUTE),
    IS_TRIGGERABLE(PropertyType.IS_TRIGGERABLE, "flowable:triggerable", XmlType.ATTRIBUTE),
    DUE_DATE(PropertyType.DUE_DATE, "flowable:dueDate", XmlType.ATTRIBUTE),
    CATEGORY(PropertyType.CATEGORY, "flowable:category", XmlType.ATTRIBUTE),
    FORM_KEY(PropertyType.FORM_KEY, "flowable:formKey", XmlType.ATTRIBUTE),
    FORM_FIELD_VALIDATION(PropertyType.FORM_FIELD_VALIDATION, "flowable:formFieldValidation", XmlType.ATTRIBUTE),
    PRIORITY(PropertyType.PRIORITY, "flowable:priority", XmlType.ATTRIBUTE),
    SCRIPT(PropertyType.SCRIPT, "script.text", XmlType.CDATA),
    SCRIPT_FORMAT(PropertyType.SCRIPT_FORMAT, "scriptFormat", XmlType.ATTRIBUTE),
    AUTO_STORE_VARIABLES(PropertyType.AUTO_STORE_VARIABLES, "flowable:autoStoreVariables", XmlType.ATTRIBUTE),
    RULE_VARIABLES_INPUT(PropertyType.RULE_VARIABLES_INPUT, "flowable:ruleVariablesInput", XmlType.ATTRIBUTE),
    RULES(PropertyType.RULES, "flowable:rules", XmlType.ATTRIBUTE),
    RESULT_VARIABLE(PropertyType.RESULT_VARIABLE, "flowable:resultVariable", XmlType.ATTRIBUTE),
    RESULT_VARIABLE_NAME(PropertyType.RESULT_VARIABLE_NAME, "flowable:resultVariableName", XmlType.ATTRIBUTE),
    EXCLUDE(PropertyType.EXCLUDE, "flowable:exclude", XmlType.ATTRIBUTE),
    SOURCE_REF(PropertyType.SOURCE_REF,"sourceRef", XmlType.ATTRIBUTE),
    TARGET_REF(PropertyType.TARGET_REF, "targetRef", XmlType.ATTRIBUTE),
    ATTACHED_TO_REF(PropertyType.ATTACHED_TO_REF, "attachedToRef", XmlType.ATTRIBUTE),
    CONDITION_EXPR_VALUE(PropertyType.CONDITION_EXPR_VALUE, "conditionExpression.text", XmlType.CDATA),
    CONDITION_EXPR_TYPE(PropertyType.CONDITION_EXPR_TYPE, "conditionExpression.xsi:type", XmlType.ATTRIBUTE),
    COMPLETION_CONDITION(PropertyType.COMPLETION_CONDITION, "completionCondition.text", XmlType.CDATA),
    DEFAULT_FLOW(PropertyType.DEFAULT_FLOW, "default", XmlType.ATTRIBUTE),
    IS_TRANSACTIONAL_SUBPROCESS(PropertyType.IS_TRANSACTIONAL_SUBPROCESS, "transactionalSubprocess", XmlType.ELEMENT),
    IS_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE(PropertyType.IS_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE, "flowable:useLocalScopeForResultVariable", XmlType.ATTRIBUTE),
    CAMEL_CONTEXT(PropertyType.CAMEL_CONTEXT, "extensionElements.flowable:field?name=camelContext.flowable:string.text", XmlType.CDATA),
    DECISION_TABLE_REFERENCE_KEY(PropertyType.DECISION_TABLE_REFERENCE_KEY, "extensionElements.flowable:field?name=decisionTableReferenceKey.flowable:string.text", XmlType.CDATA),
    DECISION_TASK_THROW_ERROR_ON_NO_HITS(PropertyType.DECISION_TASK_THROW_ERROR_ON_NO_HITS, "extensionElements.flowable:field?name=decisionTaskThrowErrorOnNoHits.flowable:string.text", XmlType.CDATA),
    FALLBACK_TO_DEF_TENANT_CDATA(PropertyType.FALLBACK_TO_DEF_TENANT_CDATA, "extensionElements.flowable:field?name=fallbackToDefaultTenant.flowable:string.text", XmlType.CDATA),
    REQUEST_METHOD(PropertyType.REQUEST_METHOD, "extensionElements.flowable:field?name=requestMethod.flowable:string.text", XmlType.CDATA),
    REQUEST_URL(PropertyType.REQUEST_URL, "extensionElements.flowable:field?name=requestUrl.flowable:string.text", XmlType.CDATA),
    REQUEST_HEADERS(PropertyType.REQUEST_HEADERS, "extensionElements.flowable:field?name=requestHeaders.flowable:string.text", XmlType.CDATA),
    REQUEST_BODY(PropertyType.REQUEST_BODY, "extensionElements.flowable:field?name=requestBody.flowable:string.text", XmlType.CDATA),
    REQUEST_BODY_ENCODING(PropertyType.REQUEST_BODY_ENCODING, "extensionElements.flowable:field?name=requestBodyEncoding.flowable:string.text", XmlType.CDATA),
    REQUEST_TIMEOUT(PropertyType.REQUEST_TIMEOUT, "extensionElements.flowable:field?name=requestTimeout.flowable:string.text", XmlType.CDATA),
    DISALLOW_REDIRECTS(PropertyType.DISALLOW_REDIRECTS, "extensionElements.flowable:field?name=disallowRedirects.flowable:string.text", XmlType.CDATA),
    FAIL_STATUS_CODES(PropertyType.FAIL_STATUS_CODES, "extensionElements.flowable:field?name=failStatusCodes.flowable:string.text", XmlType.CDATA),
    HANDLE_STATUS_CODES(PropertyType.HANDLE_STATUS_CODES, "extensionElements.flowable:field?name=handleStatusCodes.flowable:string.text", XmlType.CDATA),
    RESPONSE_VARIABLE_NAME(PropertyType.RESPONSE_VARIABLE_NAME, "extensionElements.flowable:field?name=responseVariableName.flowable:string.text", XmlType.CDATA),
    IGNORE_EXCEPTION(PropertyType.IGNORE_EXCEPTION, "extensionElements.flowable:field?name=ignoreException.flowable:string.text", XmlType.CDATA),
    SAVE_REQUEST_VARIABLES(PropertyType.SAVE_REQUEST_VARIABLES, "extensionElements.flowable:field?name=saveRequestVariables.flowable:string.text", XmlType.CDATA),
    SAVE_RESPONSE_PARAMETERS(PropertyType.SAVE_RESPONSE_PARAMETERS, "extensionElements.flowable:field?name=saveResponseParameters.flowable:string.text", XmlType.CDATA),
    RESULT_VARIABLE_PREFIX(PropertyType.RESULT_VARIABLE_PREFIX, "extensionElements.flowable:field?name=resultVariablePrefix.flowable:string.text", XmlType.CDATA),
    SAVE_RESPONSE_PARAMETERS_TRANSIENT(PropertyType.SAVE_RESPONSE_PARAMETERS_TRANSIENT, "extensionElements.flowable:field?name=saveResponseParametersTransient.flowable:string.text", XmlType.CDATA),
    SAVE_RESPONSE_VARIABLE_AS_JSON(PropertyType.SAVE_RESPONSE_VARIABLE_AS_JSON, "extensionElements.flowable:field?name=saveResponseVariableAsJson.flowable:string.text", XmlType.CDATA)
}

enum class XmlType {

    CDATA,
    ATTRIBUTE,
    ELEMENT
}

class FlowableParser : BpmnParser {

    private val mapper: XmlMapper = mapper()

    override fun parse(input: String): BpmnProcessObject {
        val dto = mapper.readValue<BpmnFile>(input)
        return toProcessObject(dto)
    }

    /**
     * Impossible to use FasterXML - Multiple objects of same type issue:
     * https://github.com/FasterXML/jackson-dataformat-xml/issues/205
     */
    override fun update(input: String, events: List<Event>): String {
        val reader = SAXReader()
        val doc = reader.read(ByteArrayInputStream(input.toByteArray(StandardCharsets.UTF_8)))

        val os = ByteArrayOutputStream()
        parseAndWrite(doc, os, events)

        return os.toString()
    }

    private fun parseAndWrite(doc: Document, os: OutputStream, events: List<Event>) {
        doUpdate(doc, events)

        val format = OutputFormat.createPrettyPrint()
        format.isPadText = false
        format.isNewLineAfterDeclaration = false
        val writer = XMLWriter(os, format)
        writer.write(doc)
    }

    private fun doUpdate(doc: Document, events: List<Event>) {
        for (event in events) {
            when (event) {
                is LocationUpdateWithId -> applyLocationUpdate(doc, event)
                is BpmnShapeResizedAndMoved -> applyShapeRectUpdate(doc, event)
                is NewWaypoints -> applyNewWaypoints(doc, event)
                is DiagramElementRemoved -> applyDiagramElementRemoved(doc, event)
                is BpmnElementRemoved -> applyBpmnElementRemoved(doc, event)
                is BpmnShapeObjectAdded -> applyBpmnShapeObjectAdded(doc, event)
                is BpmnEdgeObjectAdded -> applyBpmnEdgeObjectAdded(doc, event)
                is PropertyUpdateWithId -> applyPropertyUpdateWithId(doc, event)
                is BpmnParentChanged -> applyParentChange(doc, event)
            }
        }
    }

    fun trimWhitespace(node: Node, recurse: Boolean = true) {
        val children = node.selectNodes("*")
        val toRemove = mutableListOf<Node>()
        for (i in 0 until children.size) {
            val child = children[i]
            if (child.nodeType == Node.TEXT_NODE && child.text.trim().isBlank()) {
                toRemove.add(child)
            } else if (recurse) {
                trimWhitespace(child)
            }
        }
        toRemove.forEach { it.parent.remove(it) }
    }

    private fun applyLocationUpdate(doc: Document, update: LocationUpdateWithId) {
        val node = if (null != update.internalPos) {
            // Internal waypoint update
            doc.selectSingleNode(
                    "//*[local-name()='BPMNEdge'][@id='${update.parentElementId!!.id}']/*[@x][@y][${update.internalPos!! + 1}]"
            ) as Element
        } else {
            doc.selectSingleNode(
                    "//*[local-name()='BPMNShape'][@id='${update.diagramElementId.id}']/*[@x][@y]"
            ) as Element
        }

        val nx = node.attribute("x")
        val ny = node.attribute("y")

        nx.value = (nx.value.toFloat() + update.dx).toString()
        ny.value = (ny.value.toFloat() + update.dy).toString()
    }

    private fun applyShapeRectUpdate(doc: Document, update: BpmnShapeResizedAndMoved) {
        val node = doc.selectSingleNode(
                "//*[local-name()='BPMNShape'][@id='${update.diagramElementId.id}']/*[@x][@y]"
        ) as Element

        val nx = node.attribute("x")
        val ny = node.attribute("y")
        val nw = node.attribute("width")
        val nh = node.attribute("height")

        val nodeX = nx.value.toFloat()
        val nodeY = ny.value.toFloat()
        val nodeW = nw.value.toFloat()
        val nodeH = nh.value.toFloat()

        val left = Point2D.Float(nodeX, nodeY)
        val right = Point2D.Float(nodeX + nodeW, nodeY + nodeH)
        val newLeft = update.transform(left)
        val newRight = update.transform(right)

        nx.value = newLeft.x.toString()
        ny.value = newLeft.y.toString()
        nw.value = (newRight.x - newLeft.x).toString()
        nh.value = (newRight.y - newLeft.y).toString()
    }


    private fun applyNewWaypoints(doc: Document, update: NewWaypoints) {
        val node = doc.selectSingleNode(
                "//*[local-name()='BPMNEdge'][@id='${update.edgeElementId.id}'][1]"
        ) as Element

        val toRemove = mutableListOf<Node>()
        val children = node.selectNodes("*")
        for (pos in 0 until children.size) {
            val target = children[pos]
            if (target.name.contains("waypoint")) {
                toRemove.add(target)
                continue
            }
        }

        toRemove.forEach { node.remove(it) }
        trimWhitespace(node)

        update.waypoints.filter { it.physical }.sortedBy { it.internalPhysicalPos }.forEach {
            newWaypoint(it, node)
        }
    }

    private fun newWaypoint(it: IdentifiableWaypoint, parentEdgeElem: Element) {
        val elem = parentEdgeElem.addElement(NS.OMGDI.named("waypoint"))
        elem.addAttribute("x", it.x.toString())
        elem.addAttribute("y", it.y.toString())
    }

    private fun applyDiagramElementRemoved(doc: Document, update: DiagramElementRemoved) {
        val node = doc.selectSingleNode(
                "//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane']/*[@id='${update.elementId.id}'][1]"
        ) as Node

        val parent = node.parent
        node.parent.remove(node)
        trimWhitespace(parent, false)
    }

    private fun applyBpmnElementRemoved(doc: Document, update: BpmnElementRemoved) {
        val node = doc.selectSingleNode(
                "//*[local-name()='process']//*[@id='${update.elementId.id}'][1]"
        ) as Node

        val parent = node.parent
        node.parent.remove(node)
        trimWhitespace(parent, false)
    }

    private fun applyBpmnShapeObjectAdded(doc: Document, update: BpmnShapeObjectAdded) {
        val diagramParent = (
                doc.selectSingleNode("//*[local-name()='process'][1]//*[@id='${update.bpmnObject.parent.id}'][1]") as Element?
                        ?: doc.selectSingleNode("//*[local-name()='process'][@id='${update.bpmnObject.parent.id}'][1]") as Element?
                )!!

        val newNode = when(update.bpmnObject.element) {

            // Events
            // Start
            is BpmnStartEvent -> createStartEventWithType(diagramParent, null)
            is BpmnStartTimerEvent -> createStartEventWithType(diagramParent, "timerEventDefinition")
            is BpmnStartSignalEvent -> createStartEventWithType(diagramParent, "signalEventDefinition")
            is BpmnStartMessageEvent -> createStartEventWithType(diagramParent, "messageEventDefinition")
            is BpmnStartErrorEvent -> createStartEventWithType(diagramParent, "errorEventDefinition")
            is BpmnStartConditionalEvent -> createStartEventWithType(diagramParent, "conditionalEventDefinition")
            is BpmnStartEscalationEvent -> createStartEventWithType(diagramParent, "escalationEventDefinition")
            // End
            is BpmnEndEvent -> createEndEventWithType(diagramParent, null)
            is BpmnEndTerminateEvent -> createEndEventWithType(diagramParent, "terminateEventDefinition")
            is BpmnEndEscalationEvent -> createEndEventWithType(diagramParent, "escalationEventDefinition")
            is BpmnEndCancelEvent -> createEndEventWithType(diagramParent, "cancelEventDefinition")
            is BpmnEndErrorEvent -> createEndEventWithType(diagramParent, "errorEventDefinition")
            // Boundary
            is BpmnBoundaryCancelEvent -> createBoundaryEventWithType(diagramParent, "cancelEventDefinition")
            is BpmnBoundaryCompensationEvent -> createBoundaryEventWithType(diagramParent, "compensateEventDefinition")
            is BpmnBoundaryConditionalEvent -> createBoundaryEventWithType(diagramParent, "conditionalEventDefinition")
            is BpmnBoundaryErrorEvent -> createBoundaryEventWithType(diagramParent, "errorEventDefinition")
            is BpmnBoundaryEscalationEvent -> createBoundaryEventWithType(diagramParent, "escalationEventDefinition")
            is BpmnBoundaryMessageEvent -> createBoundaryEventWithType(diagramParent, "messageEventDefinition")
            is BpmnBoundarySignalEvent -> createBoundaryEventWithType(diagramParent, "signalEventDefinition")
            is BpmnBoundaryTimerEvent -> createBoundaryEventWithType(diagramParent, "timerEventDefinition")
            // Intermediate events
            // Catching
            is BpmnIntermediateTimerCatchingEvent -> createIntermediateCatchEventWithType(diagramParent, "timerEventDefinition")
            is BpmnIntermediateMessageCatchingEvent -> createIntermediateCatchEventWithType(diagramParent, "messageEventDefinition")
            is BpmnIntermediateSignalCatchingEvent -> createIntermediateCatchEventWithType(diagramParent, "signalEventDefinition")
            is BpmnIntermediateConditionalCatchingEvent -> createIntermediateCatchEventWithType(diagramParent, "conditionalEventDefinition")
            // Throwing
            is BpmnIntermediateNoneThrowingEvent -> createIntermediateThrowEventWithType(diagramParent, null)
            is BpmnIntermediateSignalThrowingEvent -> createIntermediateThrowEventWithType(diagramParent, "signalEventDefinition")
            is BpmnIntermediateEscalationThrowingEvent -> createIntermediateThrowEventWithType(diagramParent, "escalationEventDefinition")

            // Service tasks
            is BpmnUserTask -> diagramParent.addElement("userTask")
            is BpmnScriptTask -> diagramParent.addElement("scriptTask")
            is BpmnServiceTask -> createServiceTask(diagramParent)
            is BpmnBusinessRuleTask -> diagramParent.addElement("businessRuleTask")
            is BpmnReceiveTask -> diagramParent.addElement("receiveTask")
            is BpmnCamelTask -> createServiceTaskWithType(diagramParent, "camel")
            is BpmnHttpTask -> createServiceTaskWithType(diagramParent, "http")
            is BpmnMuleTask -> createServiceTaskWithType(diagramParent, "mule")
            is BpmnDecisionTask -> createServiceTaskWithType(diagramParent, "dmn")
            is BpmnShellTask -> createServiceTaskWithType(diagramParent, "shell")

            // Sub processes
            is BpmnCallActivity -> diagramParent.addElement("callActivity")
            is BpmnSubProcess -> diagramParent.addElement("subProcess")
            is BpmnEventSubprocess -> createEventSubprocess(diagramParent)
            is BpmnAdHocSubProcess -> diagramParent.addElement("adHocSubProcess")
            is BpmnTransactionalSubProcess -> diagramParent.addElement("transaction")

            // Gateways
            is BpmnExclusiveGateway -> diagramParent.addElement("exclusiveGateway")
            is BpmnParallelGateway -> diagramParent.addElement("parallelGateway")
            is BpmnInclusiveGateway -> diagramParent.addElement("inclusiveGateway")
            is BpmnEventGateway -> diagramParent.addElement("eventBasedGateway")

            else -> throw IllegalArgumentException("Can't store: " + update.bpmnObject)
        }

        update.props.forEach { setToNode(newNode, it.key, it.value.value) }
        trimWhitespace(diagramParent, false)

        val shapeParent = doc.selectSingleNode(
                "//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane'][1]"
        ) as Element
        val newShape = shapeParent.addElement( NS.BPMDI.named("BPMNShape"))
        newShape.addAttribute("id", update.shape.id.id)
        newShape.addAttribute("bpmnElement", update.bpmnObject.id.id)
        val newBounds = newShape.addElement(NS.OMGDC.named("Bounds"))
        val bounds = update.shape.rectBounds()
        newBounds.addAttribute("x", bounds.x.toString())
        newBounds.addAttribute("y", bounds.y.toString())
        newBounds.addAttribute("width", bounds.width.toString())
        newBounds.addAttribute("height", bounds.height.toString())
        trimWhitespace(shapeParent, false)
    }

    private fun createServiceTask(elem: Element): Element {
        return createServiceTaskWithType(elem)
    }

    private fun createBoundaryEventWithType(elem: Element, type: String): Element {
        val newElem = elem.addElement("boundaryEvent")
        newElem.addElement(type)
        return newElem
    }

    private fun createStartEventWithType(elem: Element, type: String?): Element {
        val newElem = elem.addElement("startEvent")
        type?.let { newElem.addElement(it) }
        return newElem
    }

    private fun createEndEventWithType(elem: Element, type: String?): Element {
        val newElem = elem.addElement("endEvent")
        type?.let { newElem.addElement(it) }
        return newElem
    }

    private fun createIntermediateCatchEventWithType(elem: Element, type: String): Element {
        val newElem = elem.addElement("intermediateCatchEvent")
        newElem.addElement(type)
        return newElem
    }

    private fun createIntermediateThrowEventWithType(elem: Element, type: String?): Element {
        val newElem = elem.addElement("intermediateThrowEvent")
        type?.let { newElem.addElement(it) }
        return newElem
    }

    private fun createServiceTaskWithType(elem: Element, type: String? = null): Element {
        val newElem = elem.addElement("serviceTask")
        type?.let { newElem.addAttribute(NS.FLOWABLE.named("type"), type) }
        return newElem
    }

    private fun createEventSubprocess(elem: Element): Element {
        val newElem = elem.addElement("subProcess")
        newElem.addAttribute("triggeredByEvent", "true")
        return newElem
    }

    private fun applyBpmnEdgeObjectAdded(doc: Document, update: BpmnEdgeObjectAdded) {
        val diagramParent = (
                doc.selectSingleNode("//*[local-name()='process'][1]//*[@id='${update.bpmnObject.parent.id}'][1]") as Element?
                        ?: doc.selectSingleNode("//*[local-name()='process'][@id='${update.bpmnObject.parent.id}'][1]") as Element?
                )!!

        val newNode = when(update.bpmnObject.element) {
            is BpmnSequenceFlow -> diagramParent.addElement("sequenceFlow")
            else -> throw IllegalArgumentException("Can't store: " + update.bpmnObject)
        }

        update.props.forEach { setToNode(newNode, it.key, it.value.value) }
        trimWhitespace(diagramParent, false)

        val shapeParent = doc.selectSingleNode(
                "//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane'][1]"
        ) as Element
        val newShape = shapeParent.addElement( NS.BPMDI.named("BPMNEdge"))
        newShape.addAttribute("id", update.edge.id.id)
        newShape.addAttribute("bpmnElement", update.bpmnObject.id.id)
        update.edge.waypoint.filter { it.physical }.forEach { newWaypoint(it, newShape) }
        trimWhitespace(shapeParent, false)
    }

    private fun applyPropertyUpdateWithId(doc: Document, update: PropertyUpdateWithId) {
        val node = doc.selectSingleNode("//*[local-name()='process'][1]//*[@id='${update.bpmnElementId.id}'][1]") as Element?
                ?: doc.selectSingleNode("//*[local-name()='process'][@id='${update.bpmnElementId.id}'][1]") as Element

        setToNode(node, update.property, update.newValue)

        if (null == update.newIdValue) {
            return
        }

        val diagramElement = doc.selectSingleNode("//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane'][1]/*[@bpmnElement='${update.bpmnElementId.id}']") as Element?
                ?: doc.selectSingleNode("//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane'][@bpmnElement='${update.bpmnElementId.id}']") as Element

        diagramElement.addAttribute("bpmnElement", update.newIdValue!!.id)
    }

    private fun applyParentChange(doc: Document, update: BpmnParentChanged) {
        if (!update.propagateToXml) {
            return
        }

        val node = doc.selectSingleNode(
                "//*[local-name()='process'][1]//*[@id='${update.bpmnElementId.id}'][1]"
        ) as Element

        val newParent = (
                doc.selectSingleNode("//*[local-name()='process'][1]//*[@id='${update.newParentId.id}'][1]") as Element?
                        ?: doc.selectSingleNode("//*[local-name()='process'][@id='${update.newParentId.id}'][1]") as Element?
                )!!

        node.parent.remove(node)
        newParent.add(node)
    }


    private fun setToNode(node: Element, type: PropertyType, value: Any?) {
        val details = PropertyTypeDetails.values().firstOrNull { it.propertyType == type }!!
        when {
            details.xmlPath.contains(".") -> setNestedToNode(node, type, details, value)
            else -> setAttributeOrValueOrCdataOrRemoveIfNull(node, details.xmlPath, details, asString(type.valueType, value))
        }
    }

    private fun setNestedToNode(node: Element, type: PropertyType, details: PropertyTypeDetails, value: Any?) {
        val segments = details.xmlPath.split(".")
        val childOf: ((Element, String, String?) -> Element?) = {target, name, attributeSelector -> nodeChildByName(target, name, attributeSelector)}

        var currentNode = node
        for (segment in 0 until segments.size - 1) {
            val nameParts = segments[segment].split("?")
            val name = nameParts[0]
            val attributeSelector = nameParts.getOrNull(1)
            if ("" == name) {
                continue
            }

            val child = childOf(currentNode, name, attributeSelector)
            if (null == child) {
                // do not create elements for null values
                if (null == value ) {
                    return
                }

                val newElem = currentNode.addElement(name)
                currentNode = newElem
                attributeSelector?.apply {
                    val (attrName, attrValue) = this.split("=")
                    newElem.addAttribute(attrName, attrValue)
                }
            } else {
                currentNode = child
            }
        }

        setAttributeOrValueOrCdataOrRemoveIfNull(currentNode, segments[segments.size - 1], details, asString(type.valueType, value))
    }

    private fun nodeChildByName(target: Element, name: String, attributeSelector: String?): Element? {
        if (null == attributeSelector) {
            return nodeChildByName(target, name)
        }

        val (attrName, attrValue) = attributeSelector.split("=")
        val children = target.selectNodes("*")
        for (pos in 0 until children.size) {
            val elem = children[pos] as Element
            if (elem.qualifiedName.contains(name) && attrValue == elem.attribute(attrName)?.value) {
                return children[pos] as Element
            }
        }
        return null
    }

    private fun nodeChildByName(target: Element, name: String): Element? {
        val children = target.selectNodes("*")
        for (pos in 0 until children.size) {
            val elem = children[pos] as Element
            if (elem.qualifiedName.contains(name)) {
                return children[pos] as Element
            }
        }
        return null
    }

    private fun setAttributeOrValueOrCdataOrRemoveIfNull(node: Element, name: String, details: PropertyTypeDetails, value: String?) {
        when (details.type) {
            XmlType.ATTRIBUTE -> setAttribute(node, name, value)
            XmlType.CDATA -> setCdata(node, name, value)
            XmlType.ELEMENT -> changeElementType(node, name, details, value)
        }
    }

    private fun changeElementType(node: Element, name: String, details: PropertyTypeDetails, value: String?) {
        if (PropertyTypeDetails.IS_TRANSACTIONAL_SUBPROCESS != details) {
            throw IllegalArgumentException("Can't change type for: ${details.javaClass.canonicalName}")
        }

        if (null == value || !value.toBoolean()) {
            node.name = "subProcess"
        } else {
            node.name = "transaction"
        }
    }

    private fun setAttribute(node: Element, name: String, value: String?) {
        if (value.isNullOrEmpty()) {
            val attr = node.attribute(name)
            if (null != attr) {
                node.remove(attr)
            }
            return
        }

        if (name.contains(":")) {
            val parts = name.split(":")
            val ns = parts[0]
            val localName = parts[1]

            node.addAttribute(byPrefix(ns).named(localName), value)
        } else {
            node.addAttribute(name, value)
        }
    }

    private fun setCdata(node: Element, name: String, value: String?) {
        if (value.isNullOrEmpty()) {
            node.content().filterIsInstance<CDATA>().forEach { node.remove(it) }
            node.content().filterIsInstance<Text>().forEach { node.remove(it) }
            return
        }

        node.text = value
    }

    private fun asString(type: PropertyValueType, value: Any?): String? {
        if (null == value || "" == value) {
            return null
        }

        return when(type) {
            STRING, CLASS, EXPRESSION, ATTACHED_SEQUENCE_SELECT -> value as String
            BOOLEAN -> (value as Boolean).toString()
        }
    }

    private fun toProcessObject(dto: BpmnFile): BpmnProcessObject {
        // TODO - Multi process support
        val process = dto.processes[0].toElement()
        // TODO - Multi diagram support
        val diagram = dto.diagrams!![0].toElement()

        return BpmnProcessObject(process, listOf(diagram))
    }

    private fun mapper(): XmlMapper {
        val mapper : ObjectMapper = XmlMapper(
                // FIXME https://github.com/FasterXML/jackson-module-kotlin/issues/138
                JacksonXmlModule().apply { setXMLTextElementName(CDATA_FIELD) }
        )
        mapper.registerModule(KotlinModule())
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper as XmlMapper
    }

    enum class NS(val namePrefix: String, val url: String) {
        OMGDI("omgdi", "http://www.omg.org/spec/DD/20100524/DI"),
        BPMDI("bpmdi", "http://www.omg.org/spec/BPMN/20100524/DI"),
        OMGDC("omgdc", "http://www.omg.org/spec/DD/20100524/DC"),
        FLOWABLE("flowable", "http://flowable.org/bpmn"),
        XSI("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        fun named(name: String): QName {
            return QName(name, Namespace(namePrefix, url))
        }
    }

    private fun byPrefix(prefix: String): NS {
        return NS.values().firstOrNull { it.namePrefix == prefix }!!
    }
}