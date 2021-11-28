package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.collaboration

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class Collaboration(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val participant: List<Participant>?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val messageFlow: List<MessageFlow>?,
)

data class Participant(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JacksonXmlProperty(isAttribute = true) val processRef: String,
    val documentation: String?
)

data class MessageFlow(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    val documentation: String?,
    @JacksonXmlProperty(isAttribute = true) val sourceRef: String,
    @JacksonXmlProperty(isAttribute = true) val targetRef: String,
)

