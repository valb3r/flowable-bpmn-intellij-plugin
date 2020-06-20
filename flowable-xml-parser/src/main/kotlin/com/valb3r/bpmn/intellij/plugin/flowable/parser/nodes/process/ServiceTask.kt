package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
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
        @JacksonXmlProperty(isAttribute = true) val skipExpression: String?,
        @JacksonXmlProperty(isAttribute = true) val triggerable: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val isForCompensation: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val useLocalScopeForResultVariable: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val type: String?
): BpmnMappable<BpmnServiceTask> {

    override fun toElement(): BpmnServiceTask {
        return Mappers.getMapper(ServiceTaskMapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface ServiceTaskMapping {

        @Mapping(source = "forCompensation", target = "isForCompensation")
        fun convertToDto(input: ServiceTask) : BpmnServiceTask
    }
}