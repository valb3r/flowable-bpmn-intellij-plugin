package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndEvent
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions.EscalationEventDefinition
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class EndEventNode(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JacksonXmlProperty(isAttribute = true) val documentation: String?,
    @JacksonXmlProperty(isAttribute = true) val asyncBefore: Boolean?,
    @JacksonXmlProperty(isAttribute = true) val asyncAfter: Boolean?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val incoming: List<String>?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val outgoing: List<String>?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val errorEventDefinition: ErrorEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val escalationEventDefinition: EscalationEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val cancelEventDefinition: CancelEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val terminateEventDefinition: TerminateEventDefinition?
): BpmnMappable<BpmnEndEvent> {

    override fun toElement(): BpmnEndEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: EndEventNode) : BpmnEndEvent
    }

    data class ErrorEventDefinition(
            val errorRef: String? = null
    )

    data class CancelEventDefinition(
            val cancelRef: String? = null
    )

    data class TerminateEventDefinition(
            val terminateAll: Boolean? = null
    )
}