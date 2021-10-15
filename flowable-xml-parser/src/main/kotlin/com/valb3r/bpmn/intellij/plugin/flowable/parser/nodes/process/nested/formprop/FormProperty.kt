package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

@JsonDeserialize(`as` = FormProperty::class)
class FormProperty(
    @JacksonXmlProperty(isAttribute = true) val id: String?,
    @JacksonXmlProperty(isAttribute = true) name: String?,
    @JacksonXmlProperty(isAttribute = true) val type: String?,
    @JacksonXmlProperty(isAttribute = true) string: String?,
    @JacksonXmlProperty(isAttribute = true) expression: String?,
    @JacksonXmlProperty(isAttribute = true) val variable: String?,
    @JacksonXmlProperty(isAttribute = true) val default: String?,
    @JacksonXmlProperty(isAttribute = true) val datePattern: String?,
    @JsonIgnore // FIXME this is ignored field that is updated by custom deserializer because `parser.codec.readTree(parser)` returns single object instead of array
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var value: List<ExtensionFormPropertyValue>? = null
) : FormPropExtensionElement(name, string, expression)