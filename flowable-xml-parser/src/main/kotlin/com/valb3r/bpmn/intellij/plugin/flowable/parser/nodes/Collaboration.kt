package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnCollaboration
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class Collaboration(
    @JacksonXmlProperty(isAttribute = true) val id: String,
) : BpmnMappable<BpmnCollaboration> {

    override fun toElement(): BpmnCollaboration {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    abstract class Mapping {
        abstract fun convertToDto(input: Collaboration): BpmnCollaboration
    }
}
