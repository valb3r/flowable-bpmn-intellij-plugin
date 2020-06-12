package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateCatchingEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.ConditionalEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.MessageEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.SignalEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.TimerEventDefinition
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
data class IntermediateCatchEvent(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        @XmlAttribute val documentation: String?,
        val timerEventDefinition: TimerEventDefinition?,
        val signalEventDefinition: SignalEventDefinition?,
        val messageEventDefinition: MessageEventDefinition?,
        val conditionalEventDefinition: ConditionalEventDefinition?

): BpmnMappable<BpmnIntermediateCatchingEvent> {

    override fun toElement(): BpmnIntermediateCatchingEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: IntermediateCatchEvent) : BpmnIntermediateCatchingEvent
    }
}