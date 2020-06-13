package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateCatchingEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.ConditionalEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.MessageEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.SignalEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.TimerEventDefinition
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
data class IntermediateCatchEvent(
        @Attribute val id: String,
        @Attribute val name: String?,
        @PropertyElement val documentation: String?,
        @Element val timerEventDefinition: TimerEventDefinition?,
        @Element val signalEventDefinition: SignalEventDefinition?,
        @Element val messageEventDefinition: MessageEventDefinition?,
        @Element val conditionalEventDefinition: ConditionalEventDefinition?

): BpmnMappable<BpmnIntermediateCatchingEvent> {

    override fun toElement(): BpmnIntermediateCatchingEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: IntermediateCatchEvent) : BpmnIntermediateCatchingEvent
    }
}