package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.multi_instance

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.CDATA_FIELD

class MultiInstanceLoopCharacteristics(
    @JacksonXmlProperty(isAttribute = true, localName = "collection") collection: String?,
    @JacksonXmlProperty(isAttribute = true, localName = "elementVariable") elementVariable: String?,
)

class LoopCardinality(
    @JacksonXmlProperty(isAttribute = true, localName = "type") type: String?,
    @JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData expression: String?
)

class CompletionCondition(
    @JacksonXmlProperty(isAttribute = true, localName = "type") type: String?,
    @JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData expression: String?
)