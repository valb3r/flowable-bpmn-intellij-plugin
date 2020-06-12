package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.CancelEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.ErrorEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.EscalationEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents.TerminateEventDefinition
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
data class EndEventNode(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        @XmlAttribute val documentation: String?,
        val errorEventDefinition: ErrorEventDefinition?,
        val escalationEventDefinition: EscalationEventDefinition?,
        val cancelEventDefinition: CancelEventDefinition?,
        val terminateEventDefinition: TerminateEventDefinition?
): BpmnMappable<BpmnEndEvent> {

    override fun toElement(): BpmnEndEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: EndEventNode) : BpmnEndEvent
    }
}