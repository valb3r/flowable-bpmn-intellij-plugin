package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.*
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
data class StartEventNode(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        val documentation: String?,
        val timerEventDefinition: TimerEventDefinition?,
        val signalEventDefinition: SignalEventDefinition?,
        val messageEventDefinition: MessageEventDefinition?,
        val errorEventDefinition: ErrorEventDefinition?,
        val escalationEventDefinition: EscalationEventDefinition?,
        val conditionalEventDefinition: ConditionalEventDefinition?
): BpmnMappable<BpmnStartEvent> {

    override fun toElement(): BpmnStartEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: StartEventNode) : BpmnStartEvent
    }
}