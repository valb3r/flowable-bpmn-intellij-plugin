package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers




data class ServiceTask(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        val documentation: String?,
        @JacksonXmlProperty(isAttribute = true) val async: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val expression: String?,
        @JacksonXmlProperty(isAttribute = true) val delegateExpression: String?,
        @JacksonXmlProperty(isAttribute = true, localName = "class") val clazz: String?,
        @JacksonXmlProperty(isAttribute = true) val resultVariableName: String?,
        @JacksonXmlProperty(isAttribute = true) val triggerable: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val isForCompensation: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val useLocalScopeForResultVariable: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val type: String?,
        @JsonMerge @JacksonXmlElementWrapper(useWrapping = true) val extensionElements: List<ExtensionElement>? = null
): BpmnMappable<BpmnServiceTask> {

    override fun toElement(): BpmnServiceTask {
        return Mappers.getMapper(ServiceTaskMapping::class.java).convertToDto(this)
    }

    // Can't use interface due to:
    // https://github.com/mapstruct/mapstruct/issues/1577
    @Mapper(uses = [BpmnElementIdMapper::class])
    abstract class ServiceTaskMapping {

        fun convertToDto(input: ServiceTask) : BpmnServiceTask {
            val task = doConvertToDto(input)
            return task.copy(
                    failedJobRetryTimeCycleExtension = input.extensionElements?.filter { null != it.failedJobRetryTimeCycle }?.map { it.failedJobRetryTimeCycle }?.firstOrNull()
            )
        }

        @Mapping(source = "forCompensation", target = "isForCompensation")
        protected abstract fun doConvertToDto(input: ServiceTask) : BpmnServiceTask
    }

    @JsonDeserialize(using = ExtensionElementDeserializer::class)
    open class ExtensionElement(
        val name: String? = null,
        val string: String? = null,
        val expression: String? = null,
        val failedJobRetryTimeCycle: String? = null
    )

    @JsonDeserialize(`as` = FieldExtensionElement::class)
    class FieldExtensionElement(
            @JacksonXmlProperty(isAttribute = true) name: String?,
            @JacksonXmlProperty(isAttribute = false) string: String?,
            @JacksonXmlProperty(isAttribute = false) expression: String?
    ) : ExtensionElement(name, string, expression)

    @JsonDeserialize(`as` = FailedJobRetryTimeCycleExtensionElement::class)
    class FailedJobRetryTimeCycleExtensionElement(failedJobRetryTimeCycle: String) : ExtensionElement(failedJobRetryTimeCycle = failedJobRetryTimeCycle)

    @JsonDeserialize(`as` = UnhandledExtensionElement::class)
    class UnhandledExtensionElement : ExtensionElement()

    class ExtensionElementDeserializer(vc: Class<*>? = null) : StdDeserializer<ExtensionElement?>(vc) {

        override fun deserialize(parser: JsonParser, context: DeserializationContext?): ExtensionElement {
            val node: JsonNode = parser.codec.readTree(parser)
            val staxName = (parser as FromXmlParser).staxReader.localName
            val mapper: ObjectMapper = parser.codec as ObjectMapper

            return when {
                "failedJobRetryTimeCycle" == staxName -> {
                    FailedJobRetryTimeCycleExtensionElement(node.textValue())
                }
                node.has("name") -> {
                    mapper.treeToValue(node, FieldExtensionElement::class.java)
                }
                else -> {
                    UnhandledExtensionElement()
                }
            }
        }
    }
}