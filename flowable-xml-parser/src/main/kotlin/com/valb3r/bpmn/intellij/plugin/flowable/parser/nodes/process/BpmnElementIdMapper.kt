package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import org.mapstruct.Mapper

@Mapper
open class BpmnElementIdMapper {

    fun map(value: String): BpmnElementId {
        return BpmnElementId(value)
    }
}