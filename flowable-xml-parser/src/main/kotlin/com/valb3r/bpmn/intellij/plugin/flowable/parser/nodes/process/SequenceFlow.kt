package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
data class SequenceFlow(
        @Attribute val id: String,
        @Attribute val name: String?,
        @PropertyElement val documentation: String?,
        @Attribute val sourceRef: String?,
        @Attribute val targetRef: String?,
        @Element val conditionExpression: ConditionExpression?
): BpmnMappable<BpmnSequenceFlow> {

    override fun toElement(): BpmnSequenceFlow {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: SequenceFlow) : BpmnSequenceFlow
    }
}

@Xml
data class ConditionExpression(
        @Attribute val type: String?  = null,
        @PropertyElement(writeAsCData = true) val text: String? = null
)