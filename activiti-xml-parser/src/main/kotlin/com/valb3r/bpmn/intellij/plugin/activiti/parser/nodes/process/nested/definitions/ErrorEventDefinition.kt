package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.definitions

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class ErrorEventDefinition(
    @JacksonXmlProperty(isAttribute = true) val errorRef: String? = null
)