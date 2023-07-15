package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.CDATA_FIELD

data class ConditionalEventDefinition(
    val condition: Condition? = null
) {
    data class Condition(
        @JacksonXmlProperty(isAttribute = true) val type: String? = null,
        @JacksonXmlProperty(isAttribute = true) val language: String? = null,
        @JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData val script: String? = null
    )
}