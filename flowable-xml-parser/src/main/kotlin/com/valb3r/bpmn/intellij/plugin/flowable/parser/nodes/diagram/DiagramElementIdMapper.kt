package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import org.mapstruct.Mapper

@Mapper
open class DiagramElementIdMapper {

    fun map(value: String): DiagramElementId {
        return DiagramElementId(value)
    }
}