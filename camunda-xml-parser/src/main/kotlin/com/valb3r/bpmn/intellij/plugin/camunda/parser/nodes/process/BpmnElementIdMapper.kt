package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import org.mapstruct.Mapper

@Mapper
open class BpmnElementIdMapper {

    fun map(value: String?): BpmnElementId? {
        if (null == value) {
            return null
        }

        return BpmnElementId(value)
    }
}