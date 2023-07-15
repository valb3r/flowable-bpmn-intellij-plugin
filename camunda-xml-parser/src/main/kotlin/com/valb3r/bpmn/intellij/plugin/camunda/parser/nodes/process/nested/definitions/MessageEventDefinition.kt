package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class MessageEventDefinition(
        @JacksonXmlProperty(isAttribute = true) val messageRef: String? = null
)