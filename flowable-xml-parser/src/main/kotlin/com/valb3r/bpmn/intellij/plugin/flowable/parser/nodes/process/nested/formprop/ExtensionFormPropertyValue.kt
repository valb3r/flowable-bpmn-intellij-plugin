package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class ExtensionFormPropertyValue(@JacksonXmlProperty(isAttribute = true) val id: String?, @JacksonXmlProperty(isAttribute = true) val name: String?)