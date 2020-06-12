package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
data class ExclusiveGateway(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        @XmlAttribute(name ="default") val defaultElement: String?,
        val documentation: String?
): BpmnMappable<BpmnExclusiveGateway> {

    override fun toElement(): BpmnExclusiveGateway {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: ExclusiveGateway) : BpmnExclusiveGateway
    }
}