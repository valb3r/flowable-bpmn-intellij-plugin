package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionFormProperty
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

class FormField(
    @JacksonXmlProperty(isAttribute = true) val id: String?,
    @JacksonXmlProperty(isAttribute = true) val label: String?,
    @JacksonXmlProperty(isAttribute = true) val type: String?,
    @JacksonXmlProperty(isAttribute = true) val string: String?,
    @JacksonXmlProperty(isAttribute = true) val expression: String?,
    @JacksonXmlProperty(isAttribute = true) val variable: String?,
    @JacksonXmlProperty(isAttribute = true) val defaultValue: String?,
    @JacksonXmlProperty(isAttribute = true) val datePattern: String?,
    @JsonIgnore // FIXME this is ignored field that is updated by custom deserializer because `parser.codec.readTree(parser)` returns single object instead of array
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var properties: List<ExtensionFormPropertyValue>? = null
)

@Mapper(uses = [ExtensionFormPropertyValueMapper::class])
interface FormFieldMapper {

    @Mappings(
        Mapping(source = "label", target = "name"),
        Mapping(source = "defaultValue", target = "default"),
        Mapping(source = "properties", target = "value"),
    )
    fun mapFormProperty(input: FormField) : ExtensionFormProperty
}