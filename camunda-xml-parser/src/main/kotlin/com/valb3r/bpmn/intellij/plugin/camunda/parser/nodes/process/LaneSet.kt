package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.CDATA_FIELD


data class LaneSet(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val lane: List<Lane>?,
)

data class Lane(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    val documentation: String?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val flowNodeRef: List<FlowNodeRef>?,
)

data class FlowNodeRef(@JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData val ref: String? = null)
