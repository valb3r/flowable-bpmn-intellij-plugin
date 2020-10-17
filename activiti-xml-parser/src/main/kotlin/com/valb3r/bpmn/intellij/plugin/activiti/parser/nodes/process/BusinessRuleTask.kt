package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnBusinessRuleTask
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

data class BusinessRuleTask(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        val documentation: String?,
        @JacksonXmlProperty(isAttribute = true) val async: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val isForCompensation: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val ruleVariablesInput: String?,
        @JacksonXmlProperty(isAttribute = true) val rules: String?,
        @JacksonXmlProperty(isAttribute = true) val resultVariable: String?,
        @JacksonXmlProperty(isAttribute = true) val exclude: Boolean?
): BpmnMappable<BpmnBusinessRuleTask> {

    override fun toElement(): BpmnBusinessRuleTask {
        return Mappers.getMapper(BusinessRuleTaskMapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface BusinessRuleTaskMapping {

        @Mapping(source = "forCompensation", target = "isForCompensation")
        fun convertToDto(input: BusinessRuleTask) : BpmnBusinessRuleTask
    }
}