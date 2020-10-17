package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.BpmnMappable
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
        @JacksonXmlProperty(isAttribute = true) val skipExpression: String?
): BpmnMappable<BpmnUserTask> {

    override fun toElement(): BpmnUserTask {
        return Mappers.getMapper(UserTaskMapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface UserTaskMapping {

        @Mapping(source = "forCompensation", target = "isForCompensation")
        fun convertToDto(input: UserTask) : BpmnUserTask
    }
}