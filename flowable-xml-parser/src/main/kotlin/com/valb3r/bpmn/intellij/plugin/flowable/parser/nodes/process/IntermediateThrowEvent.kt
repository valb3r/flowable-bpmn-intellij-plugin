package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateThrowingEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.EscalationEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.SignalEventDefinition
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
data class IntermediateThrowEvent(
        @Attribute val id: String,
        @Attribute val name: String?,
        @PropertyElement val documentation: String?,
        @Element val signalEventDefinition: SignalEventDefinition?,
        @Element val escalationEventDefinition: EscalationEventDefinition?

): BpmnMappable<BpmnIntermediateThrowingEvent> {

    override fun toElement(): BpmnIntermediateThrowingEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: IntermediateThrowEvent) : BpmnIntermediateThrowingEvent
    }
}

