package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ListenerField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop.*
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class UserTask(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        val documentation: String?,
        @JacksonXmlProperty(isAttribute = true) val asyncBefore: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val asyncAfter: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val isForCompensation: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val assignee: String?,
        @JacksonXmlProperty(isAttribute = true) val candidateUsers: String?,
        @JacksonXmlProperty(isAttribute = true) val candidateGroups: String?,
        @JacksonXmlProperty(isAttribute = true) val dueDate: String?,
        @JacksonXmlProperty(isAttribute = true) val category: String?,
        @JacksonXmlProperty(isAttribute = true) val formKey: String?,
        @JacksonXmlProperty(isAttribute = true) val formFieldValidation: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val priority: String?,
        @JacksonXmlProperty(isAttribute = true) val skipExpression: String?,
        @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val incoming: List<String>?,
        @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val outgoing: List<String>?,
        @JsonMerge @JacksonXmlElementWrapper(useWrapping = true) val extensionElements: List<ExtensionElement>? = null
): BpmnMappable<BpmnUserTask> {

    override fun toElement(): BpmnUserTask {
        return Mappers.getMapper(UserTaskMapping::class.java).convertToDto(this)
    }


    @Mapper(uses = [BpmnElementIdMapper::class])
    abstract class UserTaskMapping {

        private val mapper = Mappers.getMapper(FormFieldMapper::class.java)

        fun convertToDto(input: UserTask) : BpmnUserTask {
            val task = doConvertToDto(input)
            return task.copy(
                formPropertiesExtension = input.extensionElements?.filterIsInstance<FormDataExtensionElement>()
                    ?.flatMap { it.formField ?: emptyList() }
                    ?.map { mapper.mapFormProperty(it) },
                executionListener = input.extensionElements?.filterIsInstance<ExecutionListener>()?.map { ExeсutionListener(it.clazz, it.event, it.fields?.map { ListenerField(it.name, it.string) }) },
                )
        }

        protected abstract fun doConvertToDto(input: UserTask) : BpmnUserTask
    }
}
