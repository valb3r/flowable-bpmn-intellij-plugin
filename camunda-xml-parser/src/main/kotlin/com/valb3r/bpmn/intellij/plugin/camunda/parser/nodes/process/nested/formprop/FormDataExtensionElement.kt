package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(`as` = FormDataExtensionElement::class)
class FormDataExtensionElement(
    @JsonIgnore var formField: List<FormField>? = null // FIXME FasterXML returns array or object, so deserialization fails, hacked by FormPropExtensionElementDeserializer
) : FormPropExtensionElement(null, null, null)