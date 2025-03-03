package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.definitions

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class ErrorEventDefinition(
    @JacksonXmlProperty(isAttribute = true) val errorRef: String? = null,
    @JacksonXmlProperty(isAttribute = true) val errorVariableName: String? = null,
    @JacksonXmlProperty(isAttribute = true) val errorVariableLocalScope: Boolean? = null,
    @JacksonXmlProperty(isAttribute = true) val errorVariableTransient: Boolean? = null
)