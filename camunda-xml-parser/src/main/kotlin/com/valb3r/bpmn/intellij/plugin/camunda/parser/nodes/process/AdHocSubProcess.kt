package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnAdHocSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.CDATA_FIELD
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.ProcessBody
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

class AdHocSubProcess: BpmnMappable<BpmnAdHocSubProcess>, ProcessBody() {

    @JacksonXmlProperty(isAttribute = true) var id: String? = null
    @JacksonXmlProperty(isAttribute = true) var name: String? = null
    @JacksonXmlProperty(isAttribute = true) var ordering: String? = null
    var documentation: String? = null
    @JacksonXmlProperty(isAttribute = true) val asyncBefore: Boolean? = null
    @JacksonXmlProperty(isAttribute = true) val asyncAfter: Boolean? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = true) val incoming: List<String>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = true) val outgoing: List<String>? = null
    var completionCondition: CompletionCondition? = null

    override fun toElement(): BpmnAdHocSubProcess {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: AdHocSubProcess) : BpmnAdHocSubProcess
    }
}

data class CompletionCondition(
        @JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData val condition: String? = null
)