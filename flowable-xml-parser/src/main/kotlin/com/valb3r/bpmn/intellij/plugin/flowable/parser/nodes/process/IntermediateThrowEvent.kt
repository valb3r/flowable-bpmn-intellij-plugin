package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateThrowingEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.EscalationEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.SignalEventDefinition
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
data class IntermediateThrowEvent(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        @XmlAttribute val documentation: String?,
        val signalEventDefinition: SignalEventDefinition?,
        val escalationEventDefinition: EscalationEventDefinition?

): BpmnMappable<BpmnIntermediateThrowingEvent> {

    override fun toElement(): BpmnIntermediateThrowingEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: IntermediateThrowEvent) : BpmnIntermediateThrowingEvent
    }
}

