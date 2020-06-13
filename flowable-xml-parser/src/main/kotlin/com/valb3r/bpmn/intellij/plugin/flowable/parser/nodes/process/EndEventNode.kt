package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.CancelEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.ErrorEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.EscalationEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.TerminateEventDefinition
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
data class EndEventNode(
        @Attribute val id: String,
        @Attribute val name: String?,
        @PropertyElement val documentation: String?,
        @Element val errorEventDefinition: ErrorEventDefinition?,
        @Element val escalationEventDefinition: EscalationEventDefinition?,
        @Element val cancelEventDefinition: CancelEventDefinition?,
        @Element val terminateEventDefinition: TerminateEventDefinition?
): BpmnMappable<BpmnEndEvent> {

    override fun toElement(): BpmnEndEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: EndEventNode) : BpmnEndEvent
    }
}