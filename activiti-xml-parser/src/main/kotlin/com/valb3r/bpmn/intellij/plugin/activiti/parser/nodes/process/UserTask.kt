package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionFormProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

data class UserTask(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        val documentation: String?,
        @JacksonXmlProperty(isAttribute = true) val async: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val isForCompensation: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val assignee: String?,
        @JacksonXmlProperty(isAttribute = true) val dueDate: String?,
        @JacksonXmlProperty(isAttribute = true) val category: String?,
        @JacksonXmlProperty(isAttribute = true) val formKey: String?,
        @JacksonXmlProperty(isAttribute = true) val formFieldValidation: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val priority: String?,
        @JsonMerge @JacksonXmlElementWrapper(useWrapping = true) val extensionElements: List<ExtensionElement>? = null
): BpmnMappable<BpmnUserTask> {

    override fun toElement(): BpmnUserTask {
        return Mappers.getMapper(UserTaskMapping::class.java).convertToDto(this)
    }

    @JsonDeserialize(using = ExtensionElementDeserializer::class)
    open class ExtensionElement(
        val name: String? = null,
        val string: String? = null,
        val expression: String? = null
    )

    class ExtensionFormPropertyValue(@JacksonXmlProperty(isAttribute = true) val id: String?, @JacksonXmlProperty(isAttribute = true) val name: String?)

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
    ) : ExtensionElement(name, string, expression)

    @JsonDeserialize(`as` = UnhandledExtensionElement::class)
    class UnhandledExtensionElement : ExtensionElement()


    @Mapper(uses = [BpmnElementIdMapper::class])
    abstract class UserTaskMapping {

        fun convertToDto(input: UserTask) : BpmnUserTask {
            val task = doConvertToDto(input)
            return task.copy(
                formPropertiesExtension = input.extensionElements?.filterIsInstance<FormProperty>()?.map { mapFormProperty(it) }
            )
        }

        @Mapping(source = "forCompensation", target = "isForCompensation")
        protected abstract fun doConvertToDto(input: UserTask) : BpmnUserTask

        protected abstract fun mapFormProperty(input: FormProperty) : ExtensionFormProperty
    }

    class ExtensionElementDeserializer(vc: Class<*>? = null) : StdDeserializer<ExtensionElement?>(vc) {

        override fun deserialize(parser: JsonParser, context: DeserializationContext?): ExtensionElement {
            val node: JsonNode = parser.codec.readTree(parser)
            val staxName = (parser as FromXmlParser).staxReader.localName
            val mapper: ObjectMapper = parser.codec as ObjectMapper

            return when (staxName) {
                "formProperty" -> readFormProperty(mapper, node)
                else -> UnhandledExtensionElement()
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
}
