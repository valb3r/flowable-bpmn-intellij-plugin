package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnTextAnnotation
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.TextAnnotationText
import org.mapstruct.Mapper

data class TextAnnotation(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val textFormat: String?,
    val text: String?,
) {
    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun map(input: String?): TextAnnotationText? = input?.let(::TextAnnotationText)

        fun convertToDto(input: TextAnnotation): BpmnTextAnnotation
    }
}
