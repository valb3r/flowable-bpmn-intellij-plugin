package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.flowable.parser.CDATA_FIELD
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class SequenceFlow(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        val documentation: String?,
        @JacksonXmlProperty(isAttribute = true) val sourceRef: String,
        @JacksonXmlProperty(isAttribute = true) val targetRef: String,
        val conditionExpression: ConditionExpression?
): BpmnMappable<BpmnSequenceFlow> {

    override fun toElement(): BpmnSequenceFlow {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: SequenceFlow) : BpmnSequenceFlow
    }
}

data class ConditionExpression(
        val type: String,
        @JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData val text: String
)