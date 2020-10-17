package com.valb3r.bpmn.intellij.plugin.bpmn.api.mappers

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionalSubProcess
import org.mapstruct.Mapper
import org.mapstruct.Mapping

// TODO: Move this to some shared module or even API
@Mapper
interface MapTransactionalSubprocessToSubprocess {

    @Mapping(target = "transactionalSubprocess", constant = "false")
    fun map(input: BpmnTransactionalSubProcess): BpmnSubProcess

    @Mapping(target = "transactionalSubprocess", constant = "true")
    fun map(input: BpmnSubProcess): BpmnTransactionalSubProcess
}