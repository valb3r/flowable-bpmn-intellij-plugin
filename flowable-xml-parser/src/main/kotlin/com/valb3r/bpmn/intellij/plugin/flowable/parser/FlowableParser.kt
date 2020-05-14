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
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnFile
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.*
import java.nio.charset.StandardCharsets.UTF_8
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


const val CDATA_FIELD = "CDATA"

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

    override fun parse(input: InputStream): BpmnProcessObject {
        val dto = mapper.readValue<BpmnFile>(input)
        return toProcessObject(dto)
    }

    /**
     * Impossible to use FasterXML - Multiple objects of same type issue:
     * https://github.com/FasterXML/jackson-dataformat-xml/issues/205
     */
    override fun update(input: InputStream, output: OutputStream, events: List<Event>){
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(input)

        parseAndWrite(doc, OutputStreamWriter(output), events)
    }

    /**
     * Impossible to use FasterXML - Multiple objects of same type issue:
     * https://github.com/FasterXML/jackson-dataformat-xml/issues/205
     */
    override fun update(input: String, events: List<Event>): String {
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(ByteArrayInputStream(input.toByteArray(UTF_8)))

        return parseAndWrite(doc, StringWriter(), events).buffer.toString()
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
                    "//BPMNEdge[@id='${update.parentElementId!!.id}']/*[@x][@y][${update.internalPos!! + 1}]",
                    doc,
                    XPathConstants.NODE
            ) as Node
        } else {
            xpath.evaluate(
                    "//BPMNShape[@id='${update.diagramElementId.id}']/*[@x][@y]",
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
                "//BPMNEdge[@id='${update.edgeElementId.id}'][1]",
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

        update.waypoints.filter { it.physical }.sortedBy { it.internalPos }.forEach {
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
                "//BPMNDiagram/BPMNPlane/*[@id='${update.elementId.id}'][1]",
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
            is BpmnServiceTask -> doc.createElement("serviceTask")
            is BpmnEndEvent -> doc.createElement("endEvent")
            else -> throw IllegalArgumentException("Can't store: " + update.bpmnObject)
        }

        update.props.forEach { setPropertyToNode(doc, newNode, it.key, it.value.value) }
        trimWhitespace(diagramParent, false)
        diagramParent.appendChild(newNode)

        val shapeParent = xpath.evaluate(
                "//BPMNDiagram/BPMNPlane[1]",
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

        update.props.forEach { setPropertyToNode(doc, newNode, it.key, it.value.value) }
        trimWhitespace(diagramParent, false)
        diagramParent.appendChild(newNode)

        val shapeParent = xpath.evaluate(
                "//BPMNDiagram/BPMNPlane[1]",
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

        setPropertyToNode(doc, node, update.property, update.newValue)
    }

    private fun setPropertyToNode(doc: Document, node: Element, type: PropertyType, value: Any?) {
        when {
            type.xmlPath.contains(".") -> setNestedPropertyToNode(doc, node, type, value)
            else -> setOrRemoveIfNull(node, type.xmlPath, asString(type.valueType, value))
        }
    }

    private fun setNestedPropertyToNode(doc: Document, node: Element, type: PropertyType, value: Any?) {
        val segments = type.xmlPath.split(".")
        val childOf: ((Element, String) -> Element?) = child@{target, name ->
            for (pos in 0 until target.childNodes.length) {
                if (target.childNodes.item(pos).nodeName.contains(name)) {
                    return@child target.childNodes.item(pos) as Element
                }
            }
            return@child null
        }

        var currentNode = node
        for (segment in 0 until segments.size - 1) {
            val name = segments[segment]
            val child = childOf(currentNode, name)
            if (null == child) {
                val newElem = doc.createElement(name)
                currentNode.appendChild(newElem)
                currentNode = newElem
            } else {
                currentNode = child
            }
        }

        if (type.isCdata) {
            if (null == asString(type.valueType, value) && currentNode.textContent.isNullOrEmpty()) {
                currentNode.textContent = ""
            } else {
                val cdata = doc.createCDATASection(asString(type.valueType, value))
                currentNode.appendChild(cdata)
            }
        } else {
            setOrRemoveIfNull(currentNode, segments[segments.size - 1], asString(type.valueType, value))
        }
    }

    private fun setOrRemoveIfNull(node: Element, name: String, value: String?) {
        if (null == value && node.hasAttribute(name)) {
            node.removeAttribute(name)
        }

        node.setAttribute(name, value)
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