package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlValue

@XmlAccessorType(XmlAccessType.FIELD)
data class SequenceFlow(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        val documentation: String?,
        @XmlAttribute val sourceRef: String?,
        @XmlAttribute val targetRef: String?,
        val conditionExpression: ConditionExpression?
): BpmnMappable<BpmnSequenceFlow> {

    override fun toElement(): BpmnSequenceFlow {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: SequenceFlow) : BpmnSequenceFlow
    }
}

data class ConditionExpression(
        val type: String?  = null,
        @XmlValue val text: String? = null
)