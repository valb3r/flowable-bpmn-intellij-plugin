package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser

class FormPropExtensionElementDeserializer(vc: Class<*>? = null) : StdDeserializer<ExtensionElement?>(vc) {

    override fun deserialize(parser: JsonParser, context: DeserializationContext?): ExtensionElement {
        val node: JsonNode = parser.codec.readTree(parser)
        val staxName = (parser as FromXmlParser).staxReader.localName
        val mapper: ObjectMapper = parser.codec as ObjectMapper

        return when (staxName) {
            "formData" -> readFormDataProperty(mapper, node)
            "executionListener" -> mapper.treeToValue(node, ExecutionListener::class.java)
            // FIXME the ignored field that is updated by custom deserializer because `parser.codec.readTree(parser)` returns single object instead of array
            else -> FormPropUnhandledExtensionElement()
        }
    }

    private fun readFormDataProperty(mapper: ObjectMapper, node: JsonNode): FormDataExtensionElement {
        val parsedNode = mapper.treeToValue(node, FormDataExtensionElement::class.java)
        val nodeValue = node["formField"]
        val parsedArray = when {
            null == nodeValue || nodeValue.isNull -> null
            nodeValue.isArray -> {
                val fields = mapper.convertValue(nodeValue, object : TypeReference<List<FormField>?>() {})
                fields?.forEachIndexed { ind, field -> field.properties = readFormPropertyProperties(mapper, nodeValue.get(ind)) }
                fields
            }
            else -> {
                val fields = listOf(mapper.treeToValue(nodeValue, FormField::class.java))
                fields[0].properties = readFormPropertyProperties(mapper, nodeValue)
                fields
            }
        }

        parsedNode.formField = parsedArray
        return parsedNode
    }

    private fun readFormPropertyProperties(mapper: ObjectMapper, node: JsonNode): List<ExtensionFormPropertyValue>? {
        val nodeValue = node.get("properties")?.get("property")
        return when {
            null == nodeValue || nodeValue.isNull -> null
            nodeValue.isArray -> mapper.convertValue(nodeValue, object : TypeReference<List<ExtensionFormPropertyValue>?>() {})
            else -> listOf(mapper.treeToValue(nodeValue, ExtensionFormPropertyValue::class.java))
        }
    }
}