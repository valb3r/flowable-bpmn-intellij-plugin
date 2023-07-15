package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class SignalEventDefinition(
    @JacksonXmlProperty(isAttribute = true) val signalRef: String? = null
)