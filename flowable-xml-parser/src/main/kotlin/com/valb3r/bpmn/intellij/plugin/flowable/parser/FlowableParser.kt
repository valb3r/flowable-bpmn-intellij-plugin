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
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnFile
import java.io.InputStream

const val CDATA_FIELD = "CDATA"

class FlowableParser : BpmnParser {

    private val mapper: XmlMapper = mapper()

    override fun parse(input: String): BpmnProcessObject {
        val dto = mapper.readValue<BpmnFile>(input)
        return toProcessObject(dto)
    }

    override fun parse(input: InputStream): BpmnProcessObject {
        val dto = mapper.readValue<BpmnFile>(input)
        return toProcessObject(dto)
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