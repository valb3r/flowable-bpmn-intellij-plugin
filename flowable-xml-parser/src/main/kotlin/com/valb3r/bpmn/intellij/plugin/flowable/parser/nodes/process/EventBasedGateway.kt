package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnEventGateway
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
data class EventBasedGateway(
        @Attribute val id: String,
        @Attribute val name: String?,
        @Attribute(name = "default") val defaultElement: String?,
        @PropertyElement val documentation: String?
): BpmnMappable<BpmnEventGateway> {

    override fun toElement(): BpmnEventGateway {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: EventBasedGateway) : BpmnEventGateway
    }
}