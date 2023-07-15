package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class LinkEventDefinition(
    @JacksonXmlProperty(isAttribute = true) val id: String? = null,
    @JacksonXmlProperty(isAttribute = true) val name: String? = null
)