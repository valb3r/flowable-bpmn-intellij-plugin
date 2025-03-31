package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.multi_instance

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.CDATA_FIELD
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

class MultiInstanceLoopCharacteristics(
    @JacksonXmlProperty(isAttribute = true, localName = "isSequential") val isSequential: Boolean?,
    @JacksonXmlProperty(isAttribute = true, localName = "collection") val collection: String?,
    @JacksonXmlProperty(isAttribute = true, localName = "elementVariable") val elementVariable: String?,
    val loopCardinality: LoopCardinality?,
    val completionCondition: CompletionCondition?
)

class LoopCardinality(
    @JacksonXmlProperty(isAttribute = true, localName = "type") val type: String?,
    @JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData val expression: String?
)

class CompletionCondition(
    @JacksonXmlProperty(isAttribute = true, localName = "type") val type: String?,
    @JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData val expression: String?
)

@Mapper
interface MultiInstanceLoopCharacteristicsMapper {

    @Mappings(
        Mapping(source = "sequential", target = "isSequential")
    )
    fun mapMultiInstanceLoopCharacteristics(input: MultiInstanceLoopCharacteristics) : com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.MultiInstanceLoopCharacteristics
}