package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnLaxHunk
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnAssociation
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnTextAnnotation
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
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
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax.LaxXmlPreprocessor
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax.PreparedLaxXml
import org.dom4j.*
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import java.awt.geom.Point2D
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

const val CDATA_FIELD = "CDATA"

private val ARTIFACT_ELEMENT_NAMES = setOf(
    "association",
    "group",
    "textAnnotation",
)

private val ELEMENTS_AFTER_FLOW_ELEMENTS = ARTIFACT_ELEMENT_NAMES + setOf(
    "completionCondition",
    "resourceRole",
    "correlationSubscription",
    "supports",
)

private val ELEMENTS_AFTER_ARTIFACTS = setOf(
    "completionCondition",
    "resourceRole",
    "correlationSubscription",
    "supports",
)

private val ELEMENTS_AFTER_GLOBAL_MESSAGE_DEFINITIONS = setOf(
    "process",
    "collaboration",
    "choreography",
    "BPMNDiagram",
    "relationship",
)

private val BASE_ELEMENT_CHILD_ORDER = mapOf(
    "documentation" to 0,
    "extensionElements" to 1,
    "auditing" to 2,
    "monitoring" to 3,
    "categoryValueRef" to 4,
    "incoming" to 5,
    "outgoing" to 6,
)

private val MULTI_INSTANCE_CHILD_ORDER = mapOf(
    "loopCardinality" to 0,
    "loopDataInputRef" to 1,
    "loopDataOutputRef" to 2,
    "inputDataItem" to 3,
    "outputDataItem" to 4,
    "complexBehaviorDefinition" to 5,
    "completionCondition" to 6,
)

data class PropertyTypeDetails(
    val propertyType: PropertyType,
    val xmlPath: String,
    val type: XmlType,
    val forceFirst: Boolean = false
)

abstract class BaseBpmnParser(private val laxParsingEnabled: () -> Boolean = { true }): BpmnParser {

    private val laxXmlPreprocessor = LaxXmlPreprocessor()

    abstract override fun parse(input: String): BpmnProcessObject

    override fun validateForErrors(input: String): String? {
        if (!input.contains("BPMNDiagram")) {
            return "Unable to parse, missing <b>BPMNDiagram</b> XML tag that is required to build diagram<br>" +
                    "For details see:<br>" +
                    "<a href=\"https://github.com/valb3r/flowable-bpmn-intellij-plugin/issues/225#issuecomment-834159029\">https://github.com/valb3r/flowable-bpmn-intellij-plugin/issues/225</a>"
        }
        if (!input.contains("BPMNPlane")) {
            return "Unable to parse, missing <b>BPMNPlane</b> XML tag that is required to build diagram<br>" +
                    "For details see:<br>" +
                    "<a href=\"https://github.com/valb3r/flowable-bpmn-intellij-plugin/issues/225#issuecomment-834159029\">https://github.com/valb3r/flowable-bpmn-intellij-plugin/issues/225</a>"
        }

        return null
    }

    override fun validateForWarnings(input: String): String? {
        if (input.contains(engineNs().url)) {
            return null
        }

        for (otherEngine in knownEnginesNs()) {
            if (input.contains(otherEngine.url)) {
                return "This file may contain namespace for different engine (${otherEngine.namePrefix}).<br>" +
                        "It is better download relevant plugin for it:<br>" +
                        "<b>${otherEngine.namePrefix}</b> as described here: <br>" +
                        "<a href=\"https://github.com/valb3r/flowable-bpmn-intellij-plugin#installation\">https://github.com/valb3r/flowable-bpmn-intellij-plugin#installation</a>"
            }
        }

        return "This file does not seem to contain engine-specific dialect - this may lead to wrong namespaces or missing features"
    }

    /**
     * Impossible to use FasterXML - Multiple objects of same type issue:
     * https://github.com/FasterXML/jackson-dataformat-xml/issues/205
     */
    override fun update(input: String, events: List<EventPropagatableToXml>, laxHunks: List<BpmnLaxHunk>): String {
        val effectiveHunks = when {
            laxHunks.isNotEmpty() -> laxHunks
            laxParsingEnabled() -> laxXmlPreprocessor.prepare(input).hunks
            else -> emptyList()
        }
        val prepared = laxXmlPreprocessor.prepareForUpdate(input, effectiveHunks)
        val reader = SAXReader()
        val doc = reader.read(ByteArrayInputStream(prepared.xml.toByteArray(StandardCharsets.UTF_8)))

        val os = ByteArrayOutputStream()
        parseAndWrite(doc, os, events)

        return laxXmlPreprocessor.restore(os.toString(StandardCharsets.UTF_8.name()), prepared.markers)
    }

    protected fun prepareLaxXmlForJackson(input: String) = if (laxParsingEnabled()) {
        laxXmlPreprocessor.prepare(input)
    } else {
        PreparedLaxXml(input, emptyList())
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

    protected open fun knownEnginesNs(): List<NS> {
        return listOf(
            NS("activiti", "http://activiti.org/bpmn"),
            NS("camunda", "http://camunda.org/schema/1.0/bpmn"),
            NS("flowable", "http://flowable.org/bpmn")
        )
    }

    protected abstract fun modelNs(): NS
    protected abstract fun bpmndiNs(): NS
    protected abstract fun omgdcNs(): NS
    protected abstract fun omgdiNs(): NS
    protected abstract fun xsiNs(): NS
    protected abstract fun engineNs(): NS

    protected abstract fun propertyTypeDetails(): List<PropertyTypeDetails>
    protected abstract fun changeElementType(node: Element, name: String, details: PropertyTypeDetails, value: String?)

    protected fun mapper(): XmlMapper {
        val mapper: ObjectMapper = XmlMapper(
            // FIXME https://github.com/FasterXML/jackson-module-kotlin/issues/138
            JacksonXmlModule().apply { setXMLTextElementName(CDATA_FIELD) }
        )
        mapper.registerModule(KotlinModule.Builder().build())
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        return mapper as XmlMapper
    }

    private fun parseAndWrite(doc: Document, os: OutputStream, events: List<EventPropagatableToXml>) {
        doUpdate(doc, events)

        val format = OutputFormat.createPrettyPrint()
        format.isPadText = false
        format.isNewLineAfterDeclaration = false
        val writer = CustomizedXmlWriter(os, format)
        writer.write(doc)
    }

    private fun doUpdate(doc: Document, events: List<EventPropagatableToXml>) {
        for (event in events) {
            when (event) {
                //my event ->
                is LocationUpdateWithId -> applyLocationUpdate(doc, event)
                is BpmnShapeResizedAndMoved -> applyShapeRectUpdate(doc, event)
                is NewWaypoints -> applyNewWaypoints(doc, event)
                is DiagramElementRemoved -> applyDiagramElementRemoved(doc, event)
                is BpmnElementRemoved -> applyBpmnElementRemoved(doc, event.bpmnElementId)
                is BpmnElementChange -> applyChangeElement(doc, event)
                is BpmnShapeObjectAdded -> applyBpmnShapeObjectAdded(doc, event)
                is BpmnEdgeObjectAdded -> applyBpmnEdgeObjectAdded(doc, event)
                is PropertyUpdateWithId -> applyPropertyUpdateWithId(doc, event)
                is BpmnParentChanged -> applyParentChange(doc, event)
            }
        }

        placeGlobalMessageDefinitions(doc)
    }

    private fun placeGlobalMessageDefinitions(doc: Document) {
        doc.rootElement.elements()
            .filter { it.name.substringAfter(':') == "message" }
            .forEach { moveBeforeFirst(it, ELEMENTS_AFTER_GLOBAL_MESSAGE_DEFINITIONS) }
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
        val elem = parentEdgeElem.addElement(omgdiNs().named("waypoint"))
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

    private fun applyChangeElement(doc: Document, update: BpmnElementChange) {
        val node = doc.selectSingleNode(
            "//*[local-name()='process']//*[@id='${update.elementId.id}'][1]"
        ) as Node

        val parent = node.parent
        node.parent.remove(node)
        trimWhitespace(parent, false)

        val newNode = createBpmnObject(update.newBpmnElement, parent) ?: throw IllegalArgumentException("Can't store: " + update.parentIdForXml)
        placeFlowElementOrArtifact(newNode, update.newBpmnElement is BpmnTextAnnotation)
        update.props.forEach { k, v -> setToNode(newNode, k, v.value, v.index?.toMutableList()) }
        trimWhitespace(parent, false)
    }

    private fun applyBpmnElementRemoved(doc: Document, bpmnElementId: BpmnElementId) {
        val node = doc.selectSingleNode(
            "//*[local-name()='process']//*[@id='${bpmnElementId.id}'][1]"
        ) as Node

        val parent = node.parent
        node.parent.remove(node)
        trimWhitespace(parent, false)
    }

    private fun applyBpmnShapeObjectAdded(doc: Document, update: BpmnShapeObjectAdded) {
        val diagramParent = (
                doc.selectSingleNode("//*[local-name()='process'][1]//*[@id='${update.bpmnObject.parentIdForXml.id}'][1]") as Element?
                    ?: doc.selectSingleNode("//*[local-name()='process'][@id='${update.bpmnObject.parentIdForXml.id}'][1]") as Element?
                )!!

        val newNode = createBpmnObject(update.bpmnObject.element, diagramParent) ?: throw IllegalArgumentException("Can't store: " + update.bpmnObject)
        placeFlowElementOrArtifact(newNode, update.bpmnObject.element is BpmnTextAnnotation)

        update.props.forEach { k, v -> setToNode(newNode, k, v.value, v.index?.toMutableList()) }
        trimWhitespace(diagramParent, false)

        val shapeParent = doc.selectSingleNode(
            "//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane'][1]"
        ) as Element
        val newShape = shapeParent.addElement(bpmndiNs().named("BPMNShape"))
        newShape.addAttribute("id", update.shape.id.id)
        newShape.addAttribute("bpmnElement", update.bpmnObject.id.id)
        val newBounds = newShape.addElement(omgdcNs().named("Bounds"))
        val bounds = update.shape.rectBounds()
        newBounds.addAttribute("x", bounds.x.toString())
        newBounds.addAttribute("y", bounds.y.toString())
        newBounds.addAttribute("width", bounds.width.toString())
        newBounds.addAttribute("height", bounds.height.toString())
        trimWhitespace(shapeParent, false)
    }

    protected open fun createBpmnObject(element: WithBpmnId, diagramParent: Element) = when (element) {
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
        is BpmnIntermediateTimerCatchingEvent -> createIntermediateCatchEventWithType(
            diagramParent,
            "timerEventDefinition"
        )
        is BpmnIntermediateMessageCatchingEvent -> createIntermediateCatchEventWithType(
            diagramParent,
            "messageEventDefinition"
        )
        is BpmnIntermediateSignalCatchingEvent -> createIntermediateCatchEventWithType(
            diagramParent,
            "signalEventDefinition"
        )
        is BpmnIntermediateConditionalCatchingEvent -> createIntermediateCatchEventWithType(
            diagramParent,
            "conditionalEventDefinition"
        )
        // Throwing
        is BpmnIntermediateNoneThrowingEvent -> createIntermediateThrowEventWithType(diagramParent, null)
        is BpmnIntermediateSignalThrowingEvent -> createIntermediateThrowEventWithType(
            diagramParent,
            "signalEventDefinition"
        )
        is BpmnIntermediateEscalationThrowingEvent -> createIntermediateThrowEventWithType(
            diagramParent,
            "escalationEventDefinition"
        )

        // Service tasks
        is BpmnUserTask -> diagramParent.addElement(modelNs().named("userTask"))
        is BpmnScriptTask -> diagramParent.addElement(modelNs().named("scriptTask"))
        is BpmnServiceTask -> createServiceTask(diagramParent)
        is BpmnBusinessRuleTask -> diagramParent.addElement(modelNs().named("businessRuleTask"))
        is BpmnReceiveTask -> diagramParent.addElement(modelNs().named("receiveTask"))
        is BpmnManualTask -> diagramParent.addElement(modelNs().named("manualTask"))
        is BpmnCamelTask -> createServiceTaskWithType(diagramParent, "camel")
        is BpmnSendEventTask -> createServiceTaskWithType(diagramParent, "send-event")
        is BpmnHttpTask -> createServiceTaskWithType(diagramParent, "http")
        is BpmnExternalTask -> createServiceTaskWithType(diagramParent, "external")
        is BpmnMailTask -> createServiceTaskWithType(diagramParent, "mail")
        is BpmnMuleTask -> createServiceTaskWithType(diagramParent, "mule")
        is BpmnDecisionTask -> createServiceTaskWithType(diagramParent, "dmn")
        is BpmnShellTask -> createServiceTaskWithType(diagramParent, "shell")
        is BpmnTextAnnotation -> diagramParent.addElement(modelNs().named("textAnnotation"))

        // Sub processes
        is BpmnCallActivity -> diagramParent.addElement(modelNs().named("callActivity"))
        is BpmnSubProcess -> diagramParent.addElement(modelNs().named("subProcess"))
        is BpmnEventSubprocess -> createEventSubprocess(diagramParent)
        is BpmnAdHocSubProcess -> diagramParent.addElement(modelNs().named("adHocSubProcess"))
        is BpmnTransactionalSubProcess -> diagramParent.addElement(modelNs().named("transaction"))

        // Gateways
        is BpmnExclusiveGateway -> diagramParent.addElement(modelNs().named("exclusiveGateway"))
        is BpmnParallelGateway -> diagramParent.addElement(modelNs().named("parallelGateway"))
        is BpmnInclusiveGateway -> diagramParent.addElement(modelNs().named("inclusiveGateway"))
        is BpmnEventGateway -> diagramParent.addElement(modelNs().named("eventBasedGateway"))

        else -> null
    }

    private fun createServiceTask(elem: Element): Element {
        return createServiceTaskWithType(elem)
    }

    private fun createBoundaryEventWithType(elem: Element, type: String): Element {
        val newElem = elem.addElement(modelNs().named("boundaryEvent"))
        newElem.addElement(modelNs().named(type))
        return newElem
    }

    private fun createStartEventWithType(elem: Element, type: String?): Element {
        val newElem = elem.addElement(modelNs().named("startEvent"))
        type?.let { newElem.addElement(modelNs().named(it)) }
        return newElem
    }

    private fun createEndEventWithType(elem: Element, type: String?): Element {
        val newElem = elem.addElement(modelNs().named("endEvent"))
        type?.let { newElem.addElement(modelNs().named(it)) }
        return newElem
    }

    protected fun createIntermediateCatchEventWithType(elem: Element, type: String): Element {
        val newElem = elem.addElement(modelNs().named("intermediateCatchEvent"))
        newElem.addElement(modelNs().named(type))
        return newElem
    }

    protected fun createIntermediateThrowEventWithType(elem: Element, type: String?): Element {
        val newElem = elem.addElement(modelNs().named("intermediateThrowEvent"))
        type?.let { newElem.addElement(modelNs().named(it)) }
        return newElem
    }

    protected fun createServiceTaskWithType(elem: Element, type: String? = null): Element {
        val newElem = elem.addElement(modelNs().named("serviceTask"))
        type?.let { newElem.addAttribute(engineNs().named("type"), it) }
        return newElem
    }

    private fun createEventSubprocess(elem: Element): Element {
        val newElem = elem.addElement(modelNs().named("subProcess"))
        newElem.addAttribute("triggeredByEvent", "true")
        return newElem
    }

    private fun applyBpmnEdgeObjectAdded(doc: Document, update: BpmnEdgeObjectAdded) {
        val diagramParent = (
                doc.selectSingleNode("//*[local-name()='process'][1]//*[@id='${update.bpmnObject.parentIdForXml.id}'][1]") as Element?
                    ?: doc.selectSingleNode("//*[local-name()='process'][@id='${update.bpmnObject.parentIdForXml.id}'][1]") as Element?
                )!!

        val newNode = when (update.bpmnObject.element) {
            is BpmnSequenceFlow -> diagramParent.addElement(modelNs().named("sequenceFlow"))
            is BpmnAssociation -> diagramParent.addElement(modelNs().named("association"))
            else -> throw IllegalArgumentException("Can't store: " + update.bpmnObject)
        }
        placeFlowElementOrArtifact(newNode, update.bpmnObject.element is BpmnAssociation)

        update.props.forEach { k, v ->  setToNode(newNode, k, v.value) }
        trimWhitespace(diagramParent, false)

        val shapeParent = doc.selectSingleNode(
            "//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane'][1]"
        ) as Element
        val newShape = shapeParent.addElement(bpmndiNs().named("BPMNEdge"))
        newShape.addAttribute("id", update.edge.id.id)
        newShape.addAttribute("bpmnElement", update.bpmnObject.id.id)
        update.edge.waypoint.filter { it.physical }.forEach { newWaypoint(it, newShape) }
        trimWhitespace(shapeParent, false)
    }

    private fun applyPropertyUpdateWithId(doc: Document, update: PropertyUpdateWithId) {
        val node =
            doc.selectSingleNode("//*[local-name()='process'][1]//*[@id='${update.bpmnElementId.id}'][1]") as Element?
                ?: doc.selectSingleNode("//*[local-name()='process'][@id='${update.bpmnElementId.id}'][1]") as Element

            setToNode(node, update.property, update.newValue, update.propertyIndex?.toMutableList())

        if (null == update.newIdValue) {
            return
        }

        val diagramElement =
            doc.selectSingleNode("//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane'][1]/*[@bpmnElement='${update.bpmnElementId.id}']") as Element?
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
        placeFlowElementOrArtifact(node, node.name in ARTIFACT_ELEMENT_NAMES)
    }

    private fun placeFlowElementOrArtifact(element: Element, isArtifact: Boolean) {
        moveBeforeFirst(element, if (isArtifact) ELEMENTS_AFTER_ARTIFACTS else ELEMENTS_AFTER_FLOW_ELEMENTS)
    }

    private fun moveBeforeFirst(element: Element, followingElementNames: Set<String>) {
        val parent = element.parent as Element
        val firstTailElement = parent.elements().firstOrNull {
            it !== element && it.name in followingElementNames
        } ?: return

        moveBefore(element, firstTailElement)
    }

    private fun moveBefore(element: Element, followingElement: Element) {
        val parent = element.parent as Element
        val content = parent.content()
        val elementIndex = content.indexOf(element)
        val tailElementIndex = content.indexOf(followingElement)
        if (elementIndex < tailElementIndex) {
            return
        }

        element.detach()
        content.add(tailElementIndex, element)
    }

    private fun setToNode(node: Element, type: PropertyType, value: Any?, valueIndexInArray: MutableList<String>? = null) {
        val details = propertyTypeDetails().firstOrNull { it.propertyType == type } ?: return
        val path = details.xmlPath
        when {
            path.contains(".") -> setNestedToNode(node, path, type, details, value, valueIndexInArray)
            else -> setAttributeOrValueOrCdataOrRemoveIfNull(
                node,
                path,
                details,
                asString(type.valueType, value)
            )
        }
    }

    private fun setNestedToNode(node: Element, path: String, type: PropertyType, details: PropertyTypeDetails, value: Any?, valueIndexInArray: MutableList<String>? = null) {
        val segments = path.split(".")
        val childOf: (Element, String, String?, String?) -> Element? =
            { target, name, attrName, attrValue -> nodeChildByName(target, name, attrName, attrValue) }

        var currentNode = node
        for (segment in 0 until segments.size - 1) {
            val nameParts = segments[segment].split("?")
            val name = nameParts[0]
            val attributeSelector = nameParts.getOrNull(1)
            if ("" == name) {
                continue
            }

            var (attrName, attrValue) = attributeSelector?.split("=") ?: listOf(null, null)
            if (true == attrValue?.contains('@')) {
                if (null == value || null == valueIndexInArray) { // Skip null unindexable props
                    return
                }
                attrValue = attrValue.replace("@", valueIndexInArray.removeAt(0))
            }

            val child = childOf(currentNode, name, attrName, attrValue)

            if (null == child) {
                // do not create elements for null values
                if (null == value) {
                    return
                }

                // Sorting data in CustomizedXmlWriter is expensive performance-wise
                val newElem = currentNode.addElement(name)
                placeNestedElement(currentNode, newElem, details.forceFirst)

                currentNode = newElem
                // TODO Handle this with setAttributeOrValueOrCdataOrRemoveIfNull ?
                if (attrName != "\$") {
                    newElem.addAttribute(attrName, attrValue)
                } else {
                    newElem.text = attrValue
                }
            } else {
                currentNode = child
            }
        }

        setAttributeOrValueOrCdataOrRemoveIfNull(
            currentNode,
            segments[segments.size - 1],
            details,
            asString(type.valueType, value)
        )
    }

    private fun placeNestedElement(parent: Element, element: Element, forceFirst: Boolean) {
        if (forceFirst) {
            element.detach()
            parent.content().add(0, element)
            return
        }

        val elementName = element.name.substringAfter(':')
        val firstFollowingElement = when {
            elementName in BASE_ELEMENT_CHILD_ORDER -> {
                val elementOrder = BASE_ELEMENT_CHILD_ORDER.getValue(elementName)
                parent.elements().firstOrNull {
                    it !== element && (BASE_ELEMENT_CHILD_ORDER[it.name.substringAfter(':')] ?: Int.MAX_VALUE) > elementOrder
                }
            }
            parent.name.substringAfter(':') == "multiInstanceLoopCharacteristics" -> {
                val elementOrder = MULTI_INSTANCE_CHILD_ORDER[elementName] ?: return
                parent.elements().firstOrNull {
                    it !== element && (MULTI_INSTANCE_CHILD_ORDER[it.name.substringAfter(':')] ?: -1) > elementOrder
                }
            }
            elementName.endsWith("EventDefinition") -> parent.elements().firstOrNull {
                it !== element && it.name.substringAfter(':') == "eventDefinitionRef"
            }
            elementName == "multiInstanceLoopCharacteristics" -> parent.elements().firstOrNull {
                it !== element && it.name.substringAfter(':') == "rendering"
            }
            else -> null
        }

        firstFollowingElement?.let { moveBefore(element, it) }
    }

    private fun nodeChildByName(target: Element, name: String, attrName: String?, attrValue: String?): Element? {
        if (null == attrName) {
            return nodeChildByName(target, name)
        }

        val matchAttrValue = { elem: Element ->
            if (attrName.startsWith("\$")) {
                attrValue == elem.text
            } else {
                attrValue == elem.attribute(attrName)?.value
            }
        }

        val children = target.selectNodes("*")
        for (pos in 0 until children.size) {
            val elem = children[pos] as Element
            if (elem.qualifiedName.contains(name) && matchAttrValue(elem)) {
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

    private fun setAttributeOrValueOrCdataOrRemoveIfNull(
        node: Element,
        name: String,
        details: PropertyTypeDetails,
        value: String?
    ) {
        when (details.type) {
            XmlType.ATTRIBUTE -> setAttribute(node, details, name, value)
            XmlType.CDATA -> setCdata(node, details, name, value)
            XmlType.ELEMENT -> changeElementType(node, name, details, value)
        }
    }

    private fun setAttribute(node: Element, details: PropertyTypeDetails, name: String, value: String?) {
            val qname = qname(name)

        if (value.isNullOrEmpty()) {
            if (destroyEnclosingNode(details, node)) return

            val attr = node.attribute(qname)
            if (null != attr) {
                node.remove(attr)
            }
            return
        }

        node.addAttribute(qname, value)
    }

    private fun qname(name: String): QName {
        if (!name.contains(":")) {
            return QName(name)
        }

        val parts = name.split(":")
        val ns = parts[0]
        val localName = parts[1]

        return byPrefix(ns).named(localName)
    }

    private fun setCdata(node: Element, details: PropertyTypeDetails, name: String, value: String?) {
        if (value.isNullOrEmpty()) {
            if (destroyEnclosingNode(details, node)) return
            node.content().filter { it is CDATA || it is Text }.forEach { node.remove(it) }
            return
        }

        if (requiresWrappingForFormatting(value)) {
            node.content().filter { it is CDATA || it is Text }.forEach { node.remove(it) }
            node.addCDATA(value)
        } else {
            node.content().filter { it is CDATA || it is Text }.forEach { node.remove(it) }
            node.text = value
        }
    }

    private fun destroyEnclosingNode(details: PropertyTypeDetails, node: Element): Boolean {
        if (!details.propertyType.removeEnclosingNodeIfNullOrEmpty) {
            return false
        }
        node.parent.remove(node)
        return true
    }

    private fun requiresWrappingForFormatting(value: String): Boolean {
        return value.startsWith(" ") || value.endsWith(" ") || value.contains("\n")
    }

    private fun asString(type: PropertyValueType, value: Any?): String? {
        if (null == value || "" == value) {
            return null
        }

        return when (type) {
            STRING, CLASS, EXPRESSION, ATTACHED_SEQUENCE_SELECT, LIST_SELECT -> value as String
            BOOLEAN -> (value as Boolean).toString()
        }
    }

    private fun byPrefix(prefix: String): NS {
        return listOf(modelNs(), bpmndiNs(), omgdcNs(), omgdiNs(), xsiNs(), engineNs()).firstOrNull { it.namePrefix == prefix }!!
    }
}

data class NS(val namePrefix: String, val url: String) {
    fun named(name: String): QName {
        return QName(name, Namespace(namePrefix, url))
    }
}

enum class XmlType {

    CDATA,
    ATTRIBUTE,
    ELEMENT
}
