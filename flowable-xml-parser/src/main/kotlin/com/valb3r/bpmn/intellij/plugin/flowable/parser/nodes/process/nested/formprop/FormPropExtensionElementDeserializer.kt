package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser

class FormPropExtensionElementDeserializer(vc: Class<*>? = null) : StdDeserializer<FormPropExtensionElement?>(vc) {

    override fun deserialize(parser: JsonParser, context: DeserializationContext?): FormPropExtensionElement {
        val node: JsonNode = parser.codec.readTree(parser)
        val staxName = (parser as FromXmlParser).staxReader.localName
        val mapper: ObjectMapper = parser.codec as ObjectMapper

        return when (staxName) {
            "formProperty" -> readFormProperty(
                mapper,
                node
            ) // FIXME the ignored field that is updated by custom deserializer because `parser.codec.readTree(parser)` returns single object instead of array
            else -> FormPropUnhandledExtensionElement()
        }
    }

    private fun readFormProperty(mapper: ObjectMapper, node: JsonNode): FormProperty {
        val parsedNode = mapper.treeToValue(node, FormProperty::class.java)
        val nodeValue = node["value"]
        val parsedArray = when {
            null == nodeValue || nodeValue.isNull -> null
            nodeValue.isArray -> mapper.convertValue(nodeValue, object : TypeReference<List<ExtensionFormPropertyValue>?>() {})
            else -> listOf(mapper.treeToValue(nodeValue, ExtensionFormPropertyValue::class.java))
        }

        parsedNode.value = parsedArray
        return parsedNode
    }
}