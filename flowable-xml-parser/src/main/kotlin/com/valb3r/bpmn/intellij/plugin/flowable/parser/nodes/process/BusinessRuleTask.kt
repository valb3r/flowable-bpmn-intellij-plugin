package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnBusinessRuleTask
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
data class BusinessRuleTask(
        @Attribute val id: String,
        @Attribute val name: String?,
        @PropertyElement val documentation: String?,
        @Attribute val async: Boolean?,
        @Attribute val isForCompensation: Boolean?,
        @Attribute val ruleVariablesInput: String?,
        @Attribute val rules: String?,
        @Attribute val resultVariable: String?,
        @Attribute val exclude: Boolean?
): BpmnMappable<BpmnBusinessRuleTask> {

    override fun toElement(): BpmnBusinessRuleTask {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: BusinessRuleTask) : BpmnBusinessRuleTask
    }
}