package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(using = FormPropExtensionElementDeserializer::class)
open class FormPropExtensionElement(
    val name: String? = null,
    val string: String? = null,
    val expression: String? = null
)