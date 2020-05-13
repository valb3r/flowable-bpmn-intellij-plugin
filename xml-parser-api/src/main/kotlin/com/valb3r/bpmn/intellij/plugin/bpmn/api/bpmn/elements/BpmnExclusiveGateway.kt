package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId

@KotlinBuilder
data class BpmnExclusiveGateway(
        override val id: BpmnElementId,
        val name: String?,
        val defaultElement: String?,
        val documentation: String?
): WithBpmnId