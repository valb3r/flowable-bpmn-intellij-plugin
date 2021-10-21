package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import org.mapstruct.Mapper
import org.mapstruct.Mapping

class ExtensionFormPropertyValue(@JacksonXmlProperty(isAttribute = true) val id: String?, @JacksonXmlProperty(isAttribute = true) val value: String?)

@Mapper
interface ExtensionFormPropertyValueMapper {

    @Mapping(source = "value", target = "name")
    fun map(input: ExtensionFormPropertyValue): com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionFormPropertyValue
}