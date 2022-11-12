package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

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
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ListenerField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.UnmappedProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

data class ServiceTask(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        val documentation: String?,
        @JacksonXmlProperty(isAttribute = true) val asyncBefore: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val asyncAfter: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val expression: String?,
        @JacksonXmlProperty(isAttribute = true) val delegateExpression: String?,
        @JacksonXmlProperty(isAttribute = true, localName = "class") val clazz: String?,
        @JacksonXmlProperty(isAttribute = true) val resultVariableName: String?,
        @JacksonXmlProperty(isAttribute = true) val skipExpression: String?,
        @JacksonXmlProperty(isAttribute = true) val triggerable: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val isForCompensation: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val useLocalScopeForResultVariable: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val type: String?,
        //Unmapped
        @JacksonXmlProperty(isAttribute = true, localName = "topic") var jobTopic: String?,
        @JacksonXmlProperty(isAttribute = true) var taskPriority: String?,
        @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val incoming: List<String>?,
        @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val outgoing: List<String>?,
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
                    fieldsExtension = input.extensionElements?.filterIsInstance<FieldExtensionElement>()?.map { ExtensionField(it.name, it.string, it.expression) },
                    unmappedProperties = buildUnmappedProperties(
                        UnmappedProperty("jobTopic", input.jobTopic),
                        UnmappedProperty("taskPriority", input.taskPriority),
                    ),
                    failedJobRetryTimeCycle = input.extensionElements?.filter { null != it.failedJobRetryTimeCycle }?.map { it.failedJobRetryTimeCycle }?.firstOrNull(),
                    executionListener = input.extensionElements?.filterIsInstance<ExecutionListener>()?.map { ExeсutionListener(it.clazz, it.event, it.fields?.map { ListenerField(it.name, it.string) }) },
                )
        }

        private fun buildUnmappedProperties(vararg unmappedProp:UnmappedProperty) : List<UnmappedProperty>{
            return unmappedProp.filter { null != it.name && null != it.string }.map{ it }
        }


        @Mapping(source = "forCompensation", target = "isForCompensation")
        protected abstract fun doConvertToDto(input: ServiceTask) : BpmnServiceTask
    }

    @JsonDeserialize(using = ExtensionElementDeserializer::class)
    open class ExtensionElement(
        open val name: String? = null,
        open val string: String? = null,
        val expression: String? = null,
        val failedJobRetryTimeCycle: String? = null,
        val source: String? = null,
        val target: String? = null,
        val type: String? = null,
        val clazz: String? = null,
        val event: String? = null,
        val fields: List<ListenerFieldName>? = null
    )

    class ListenerFieldName(
        val name: String? = null,
        val string: String? = null
    )

    @JsonDeserialize(`as` = ExecutionListener::class)
    open class ExecutionListener(
        @JacksonXmlProperty(isAttribute = true, localName = "class") clazz: String?,
        @JacksonXmlProperty(isAttribute = true) event: String?,
        @JacksonXmlProperty(isAttribute = false) field: List<ListenerFieldName>?,
    ) : ExtensionElement(clazz = clazz, event = event, fields = field)

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

            return when (staxName) {
                "failedJobRetryTimeCycle" -> FailedJobRetryTimeCycleExtensionElement(node.textValue())
                "field" -> mapper.treeToValue(node, FieldExtensionElement::class.java)
                "executionListener" -> mapper.treeToValue(node, ExecutionListener::class.java)
                else -> UnhandledExtensionElement()
            }
        }
    }
}