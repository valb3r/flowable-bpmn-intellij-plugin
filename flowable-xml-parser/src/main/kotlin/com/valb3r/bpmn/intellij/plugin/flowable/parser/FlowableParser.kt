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
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnAdHocSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionalSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnFile
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


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
    EXCLUDE(PropertyType.EXCLUDE, "flowable:exclude", XmlType.ATTRIBUTE),
    SOURCE_REF(PropertyType.SOURCE_REF,"sourceRef", XmlType.ATTRIBUTE),
    TARGET_REF(PropertyType.TARGET_REF, "targetRef", XmlType.ATTRIBUTE),
    CONDITION_EXPR_VALUE(PropertyType.CONDITION_EXPR_VALUE, "conditionExpression.text", XmlType.CDATA),
    CONDITION_EXPR_TYPE(PropertyType.CONDITION_EXPR_TYPE, "conditionExpression.xsi:type", XmlType.ATTRIBUTE),
    COMPLETION_CONDITION(PropertyType.COMPLETION_CONDITION, "completionCondition.text", XmlType.CDATA),
    DEFAULT_FLOW(PropertyType.DEFAULT_FLOW, "default", XmlType.ATTRIBUTE)
}

enum class XmlType {

    CDATA,
    ATTRIBUTE
}

class FlowableParser : BpmnParser {

    val OMGDI_NS = "http://www.omg.org/spec/DD/20100524/DI"
    val BPMDI_NS = "http://www.omg.org/spec/BPMN/20100524/DI"
    val OMGDC_NS = "http://www.omg.org/spec/DD/20100524/DC"

    private val mapper: XmlMapper = mapper()
    private val dbFactory = DocumentBuilderFactory.newInstance()
    private val transformer = TransformerFactory.newInstance()
    private val xpathFactory = XPathFactory.newInstance()


    override fun parse(input: String): BpmnProcessObject {
        val dto = mapper.readValue<BpmnFile>(input)
        return toProcessObject(dto)
    }

    /**
     * Impossible to use FasterXML - Multiple objects of same type issue:
     * https://github.com/FasterXML/jackson-dataformat-xml/issues/205
     */
    override fun update(input: String, events: List<Event>): String {
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(ByteArrayInputStream(input.toByteArray(StandardCharsets.UTF_8)))

        val writer = StringWriter()
        parseAndWrite(doc, writer, events)
        return writer.buffer.toString()
    }

    private fun <T: Writer> parseAndWrite(doc: Document, writer: T, events: List<Event>): T {
        doc.documentElement.normalize()

        doUpdate(doc, events)

        val transformer = transformer.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer
    }

    private fun doUpdate(doc: Document, events: List<Event>) {
        for (event in events) {
            when (event) {
                is LocationUpdateWithId -> applyLocationUpdate(doc, event)
                is NewWaypoints -> applyNewWaypoints(doc, event)
                is DiagramElementRemoved -> applyDiagramElementRemoved(doc, event)
                is BpmnElementRemoved -> applyBpmnElementRemoved(doc, event)
                is BpmnShapeObjectAdded -> applyBpmnShapeObjectAdded(doc, event)
                is BpmnEdgeObjectAdded -> applyBpmnEdgeObjectAdded(doc, event)
                is PropertyUpdateWithId -> applyPropertyUpdateWithId(doc, event)
            }
        }
    }

    fun trimWhitespace(node: Node, recurse: Boolean = true) {
        val children = node.childNodes
        val toRemove = mutableListOf<Node>()
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.TEXT_NODE && child.textContent.trim().isBlank()) {
                toRemove.add(child)
            } else if (recurse) {
                trimWhitespace(child)
            }
        }
        toRemove.forEach { it.parentNode.removeChild(it) }
    }

    private fun applyLocationUpdate(doc: Document, update: LocationUpdateWithId) {
        val xpath = xpathFactory.newXPath()
        val node = if (null != update.internalPos) {
            // Internal waypoint update
            xpath.evaluate(
                    "//*[local-name()='BPMNEdge'][@id='${update.parentElementId!!.id}']/*[@x][@y][${update.internalPos!! + 1}]",
                    doc,
                    XPathConstants.NODE
            ) as Node
        } else {
            xpath.evaluate(
                    "//*[local-name()='BPMNShape'][@id='${update.diagramElementId.id}']/*[@x][@y]",
                    doc,
                    XPathConstants.NODE
            ) as Node
        }

        val nx = node.attributes.getNamedItem("x")
        val ny = node.attributes.getNamedItem("y")

        nx.nodeValue = (nx.nodeValue.toFloat() + update.dx).toString()
        ny.nodeValue = (ny.nodeValue.toFloat() + update.dy).toString()
    }

    private fun applyNewWaypoints(doc: Document, update: NewWaypoints) {
        val xpath = xpathFactory.newXPath()
        val node = xpath.evaluate(
                "//*[local-name()='BPMNEdge'][@id='${update.edgeElementId.id}'][1]",
                doc,
                XPathConstants.NODE
        ) as Node

        val toRemove = mutableListOf<Node>()
        for (pos in 0 until node.childNodes.length) {
            val target = node.childNodes.item(pos)
            if (target.nodeName.contains("waypoint")) {
                toRemove.add(target)
                continue
            }
        }

        toRemove.forEach { node.removeChild(it) }
        trimWhitespace(node)

        update.waypoints.filter { it.physical }.sortedBy { it.internalPhysicalPos }.forEach {
            newWaypoint(doc, it, node)
        }
    }

    private fun newWaypoint(doc: Document, it: IdentifiableWaypoint, parentEdgeElem: Node) {
        val elem = doc.createElementNS(OMGDI_NS, "omgdi:waypoint")
        elem.setAttribute("x", it.x.toString())
        elem.setAttribute("y", it.y.toString())
        parentEdgeElem.appendChild(elem)
    }

    private fun applyDiagramElementRemoved(doc: Document, update: DiagramElementRemoved) {
        val xpath = xpathFactory.newXPath()
        val node = xpath.evaluate(
                "//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane']/*[@id='${update.elementId.id}'][1]",
                doc,
                XPathConstants.NODE
        ) as Node

        val parent = node.parentNode
        node.parentNode.removeChild(node)
        trimWhitespace(parent, false)
    }

    private fun applyBpmnElementRemoved(doc: Document, update: BpmnElementRemoved) {
        val xpath = xpathFactory.newXPath()
        val node = xpath.evaluate(
                "//process/*[@id='${update.elementId.id}'][1]",
                doc,
                XPathConstants.NODE
        ) as Node

        val parent = node.parentNode
        node.parentNode.removeChild(node)
        trimWhitespace(parent, false)
    }

    private fun applyBpmnShapeObjectAdded(doc: Document, update: BpmnShapeObjectAdded) {
        val xpath = xpathFactory.newXPath()
        val diagramParent = xpath.evaluate(
                "//process[1]",
                doc,
                XPathConstants.NODE
        ) as Node

        val newNode = when(update.bpmnObject) {
            is BpmnStartEvent -> doc.createElement("startEvent")
            is BpmnCallActivity -> doc.createElement("callActivity")
            is BpmnExclusiveGateway -> doc.createElement("exclusiveGateway")
            is BpmnSequenceFlow -> doc.createElement("sequenceFlow")
            is BpmnUserTask -> doc.createElement("userTask")
            is BpmnScriptTask -> doc.createElement("scriptTask")
            is BpmnServiceTask -> createServiceTask(doc)
            is BpmnBusinessRuleTask -> doc.createElement("businessRuleTask")
            is BpmnReceiveTask -> doc.createElement("receiveTask")
            is BpmnCamelTask -> createServiceTaskWithType(doc, "camel")
            is BpmnHttpTask -> createServiceTaskWithType(doc, "http")
            is BpmnMuleTask -> createServiceTaskWithType(doc, "mule")
            is BpmnDecisionTask -> createServiceTaskWithType(doc, "dmn")
            is BpmnShellTask -> createServiceTaskWithType(doc, "shell")
            is BpmnSubProcess -> doc.createElement("subProcess")
            is BpmnAdHocSubProcess -> doc.createElement("adHocSubProcess")
            is BpmnTransactionalSubProcess -> doc.createElement("transaction")
            is BpmnEndEvent -> doc.createElement("endEvent")
            else -> throw IllegalArgumentException("Can't store: " + update.bpmnObject)
        }

        update.props.forEach { setToNode(doc, newNode, it.key, it.value.value) }
        trimWhitespace(diagramParent, false)
        diagramParent.appendChild(newNode)

        val shapeParent = xpath.evaluate(
                "//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane'][1]",
                doc,
                XPathConstants.NODE
        ) as Node
        val newShape = doc.createElementNS(BPMDI_NS, "bpmndi:BPMNShape")
        newShape.setAttribute("id", update.shape.id.id)
        newShape.setAttribute("bpmnElement", update.bpmnObject.id.id)
        shapeParent.appendChild(newShape)
        val newBounds = doc.createElementNS(OMGDC_NS, "omgdc:Bounds")
        newBounds.setAttribute("x", update.shape.bounds.x.toString())
        newBounds.setAttribute("y", update.shape.bounds.y.toString())
        newBounds.setAttribute("width", update.shape.bounds.width.toString())
        newBounds.setAttribute("height", update.shape.bounds.height.toString())
        newShape.appendChild(newBounds)
        trimWhitespace(shapeParent, false)
    }

    private fun createServiceTask(doc: Document): Element {
        return createServiceTaskWithType(doc)
    }

    private fun createServiceTaskWithType(doc: Document, type: String? = null): Element {
        val elem = doc.createElement("serviceTask")
        type?.let { elem.setAttribute("flowable:type", type) }
        return elem
    }

    private fun applyBpmnEdgeObjectAdded(doc: Document, update: BpmnEdgeObjectAdded) {
        val xpath = xpathFactory.newXPath()
        val diagramParent = xpath.evaluate(
                "//process[1]",
                doc,
                XPathConstants.NODE
        ) as Node

        val newNode = when(update.bpmnObject) {
            is BpmnSequenceFlow -> doc.createElement("sequenceFlow")
            else -> throw IllegalArgumentException("Can't store: " + update.bpmnObject)
        }

        update.props.forEach { setToNode(doc, newNode, it.key, it.value.value) }
        trimWhitespace(diagramParent, false)
        diagramParent.appendChild(newNode)

        val shapeParent = xpath.evaluate(
                "//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane'][1]",
                doc,
                XPathConstants.NODE
        ) as Node
        val newShape = doc.createElementNS(BPMDI_NS, "bpmndi:BPMNEdge")
        newShape.setAttribute("id", update.edge.id.id)
        newShape.setAttribute("bpmnElement", update.bpmnObject.id.id)
        shapeParent.appendChild(newShape)
        update.edge.waypoint.filter { it.physical }.forEach { newWaypoint(doc, it, newShape) }
        trimWhitespace(shapeParent, false)
    }

    private fun applyPropertyUpdateWithId(doc: Document, update: PropertyUpdateWithId) {
        val xpath = xpathFactory.newXPath()
        val node = xpath.evaluate(
                "//process/*[@id='${update.bpmnElementId.id}'][1]",
                doc,
                XPathConstants.NODE
        ) as Element

        setToNode(doc, node, update.property, update.newValue)

        if (null == update.newIdValue) {
            return
        }

        val diagramElement = xpath.evaluate(
                "//*[local-name()='BPMNDiagram']/*[local-name()='BPMNPlane'][1]/*[@bpmnElement='${update.bpmnElementId.id}']",
                doc,
                XPathConstants.NODE
        ) as Element

        diagramElement.setAttribute("bpmnElement", update.newIdValue!!.id)
    }


    private fun setToNode(doc: Document, node: Element, type: PropertyType, value: Any?) {
        val details = PropertyTypeDetails.values().filter { it.propertyType == type }.firstOrNull()!!
        when {
            details.xmlPath.contains(".") -> setNestedToNode(doc, node, type, details, value)
            else -> setAttributeOrValueOrCdataOrRemoveIfNull(node, details.xmlPath, details, asString(type.valueType, value))
        }
    }

    private fun setNestedToNode(doc: Document, node: Element, type: PropertyType, details: PropertyTypeDetails, value: Any?) {
        val segments = details.xmlPath.split(".")
        val childOf: ((Element, String) -> Element?) = {target, name -> nodeChildByName(target, name)}

        var currentNode = node
        for (segment in 0 until segments.size - 1) {
            val name = segments[segment]
            if ("" == name) {
                continue
            }

            val child = childOf(currentNode, name)
            if (null == child) {
                // do not create elements for null values
                if (null == value ) {
                    return
                }

                val newElem = doc.createElement(name)
                currentNode.appendChild(newElem)
                currentNode = newElem
            } else {
                currentNode = child
            }
        }

        setAttributeOrValueOrCdataOrRemoveIfNull(currentNode, segments[segments.size - 1], details, asString(type.valueType, value))
    }

    private fun nodeChildByName(target: Element, name: String): Element? {
        for (pos in 0 until target.childNodes.length) {
            if (target.childNodes.item(pos).nodeName.contains(name)) {
                return target.childNodes.item(pos) as Element
            }
        }
        return null
    }

    private fun setAttributeOrValueOrCdataOrRemoveIfNull(node: Element, name: String, details: PropertyTypeDetails, value: String?) {
        when (details.type) {
            XmlType.ATTRIBUTE -> setAttribute(node, name, value)
            XmlType.CDATA -> setCdata(node, name, value)
        }
    }

    private fun setAttribute(node: Element, name: String, value: String?) {
        if (value.isNullOrEmpty()) {
            if (node.hasAttribute(name)) {
                node.removeAttribute(name)
            }
            return
        }

        node.setAttribute(name, value)
    }

    private fun setCdata(node: Element, name: String, value: String?) {
        if (value.isNullOrEmpty()) {
            if (node.textContent.isNotBlank()) {
                node.textContent = null
            }
            return
        }

        node.textContent = value
    }

    private fun asString(type: PropertyValueType, value: Any?): String? {
        if (null == value || "" == value) {
            return null
        }

        return when(type) {
            STRING, CLASS, EXPRESSION -> value as String
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
}