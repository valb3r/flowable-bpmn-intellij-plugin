package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

@JsonDeserialize(using = FormPropExtensionElementDeserializer::class)
open class ExtensionElement(
    val name: String? = null,
    val string: String? = null,
    val expression: String? = null,
    val clazz: String? = null,
    val event: String? = null,
    val fields: List<ListenerFieldName>? = null
)

@JsonDeserialize(`as` = ExecutionListener::class)
open class ExecutionListener(
    @JacksonXmlProperty(isAttribute = true, localName = "class") clazz: String?,
    @JacksonXmlProperty(isAttribute = true) event: String?,
    @JacksonXmlProperty(isAttribute = false) field: List<ListenerFieldName>?,
) : ExtensionElement(clazz = clazz, event = event, fields = field)

class ListenerFieldName(
    val name: String? = null,
    val string: String? = null
)