package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.definitions.ErrorEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.definitions.EscalationEventDefinition
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.formprop.ExtensionElement
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class EndEventNode(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JacksonXmlProperty(isAttribute = true) val documentation: String?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val errorEventDefinition: ErrorEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val escalationEventDefinition: EscalationEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val cancelEventDefinition: CancelEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val terminateEventDefinition: TerminateEventDefinition?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = true) val extensionElements: List<ExtensionElement>? = null
): BpmnMappable<BpmnEndEvent> {

    override fun toElement(): BpmnEndEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: EndEventNode) : BpmnEndEvent
    }

    data class CancelEventDefinition(
            val cancelRef: String? = null
    )

    data class TerminateEventDefinition(
            val terminateAll: Boolean? = null
    )
}