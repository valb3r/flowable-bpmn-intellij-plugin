package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.BpmnBoundaryEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.*
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
data class BoundaryEvent(
        @Attribute val id: String,
        @Attribute val name: String?,
        @Attribute val attachedToRef: String?,
        @Attribute val cancelActivity: Boolean?,
        @PropertyElement val documentation: String?,
        @Element val timerEventDefinition: TimerEventDefinition?,
        @Element val signalEventDefinition: SignalEventDefinition?,
        @Element val messageEventDefinition: MessageEventDefinition?,
        @Element val errorEventDefinition: ErrorEventDefinition?,
        @Element val cancelEventDefinition: CancelEventDefinition?,
        @Element val compensateEventDefinition: CompensateEventDefinition?,
        @Element val conditionalEventDefinition: ConditionalEventDefinition?,
        @Element val escalationEventDefinition: EscalationEventDefinition?
): BpmnMappable<BpmnBoundaryEvent> {

    override fun toElement(): BpmnBoundaryEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: BoundaryEvent) : BpmnBoundaryEvent
    }
}