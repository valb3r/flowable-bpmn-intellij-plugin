package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class ExtensionFormPropertyValue(@JacksonXmlProperty(isAttribute = true) val id: String?, @JacksonXmlProperty(isAttribute = true) val value: String?)