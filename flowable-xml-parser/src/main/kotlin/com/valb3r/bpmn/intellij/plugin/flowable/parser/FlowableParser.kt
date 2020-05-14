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
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnFile
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.StringWriter
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
        doc.documentElement.normalize()

        doUpdate(doc, events)

        transformer.newTransformer().transform(DOMSource(doc), StreamResult(output))
    }

    /**
     * Impossible to use FasterXML - Multiple objects of same type issue:
     * https://github.com/FasterXML/jackson-dataformat-xml/issues/205
     */
    override fun update(input: String, events: List<Event>): String {
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(ByteArrayInputStream(input.toByteArray(UTF_8)))
        doc.documentElement.normalize()

        doUpdate(doc, events)

        val writer = StringWriter()
        val transformer = transformer.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.buffer.toString()
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

    private fun applyLocationUpdate(doc: Document, update: LocationUpdateWithId) {
        val xpath = xpathFactory.newXPath()
        val node = xpath.evaluate(
                """
                //BPMNShape[@id='${update.diagramElementId.id}']/*[@x][@y][${update.internalPos!! + 1}]
                | //BPMNEdge[@id='${update.diagramElementId.id}']/*[@x][@y][${update.internalPos!! + 1}]
                """,
                doc,
                XPathConstants.NODE
        ) as Node

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
        var wasRemoved = false
        for (pos in 0 until node.childNodes.length) {
            val target = node.childNodes.item(pos)
            if (target.nodeName.contains("waypoint")) {
                toRemove.add(target)
                wasRemoved = true
                continue
            }

            if (wasRemoved && "#text" == target.nodeName) {
                toRemove.add(target)
            }

            wasRemoved = false
        }

        toRemove.forEach { node.removeChild(it) }

        update.waypoints.filter { it.physical }.sortedBy { it.internalPos }.forEach {
            val elem = doc.createElementNS(OMGDI_NS, "omgdi:waypoint")
            elem.setAttribute("x", it.x.toString())
            elem.setAttribute("y", it.y.toString())
            node.appendChild(elem)
        }
    }

    private fun applyDiagramElementRemoved(doc: Document, update: DiagramElementRemoved) {
    }

    private fun applyBpmnElementRemoved(doc: Document, update: BpmnElementRemoved) {
    }

    private fun applyBpmnShapeObjectAdded(doc: Document, update: BpmnShapeObjectAdded) {
    }

    private fun applyBpmnEdgeObjectAdded(doc: Document, update: BpmnEdgeObjectAdded) {
    }

    private fun applyPropertyUpdateWithId(doc: Document, update: PropertyUpdateWithId) {
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