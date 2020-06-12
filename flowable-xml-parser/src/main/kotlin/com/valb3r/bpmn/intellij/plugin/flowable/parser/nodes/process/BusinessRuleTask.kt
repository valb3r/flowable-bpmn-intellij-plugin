package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnBusinessRuleTask
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
data class BusinessRuleTask(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        val documentation: String?,
        @XmlAttribute val async: Boolean?,
        @XmlAttribute val isForCompensation: Boolean?,
        @XmlAttribute val ruleVariablesInput: String?,
        @XmlAttribute val rules: String?,
        @XmlAttribute val resultVariable: String?,
        @XmlAttribute val exclude: Boolean?
): BpmnMappable<BpmnBusinessRuleTask> {

    override fun toElement(): BpmnBusinessRuleTask {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: BusinessRuleTask) : BpmnBusinessRuleTask
    }
}