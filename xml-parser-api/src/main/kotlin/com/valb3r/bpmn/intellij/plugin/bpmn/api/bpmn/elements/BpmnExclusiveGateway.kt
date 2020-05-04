package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.github.pozo.KotlinBuilder

@KotlinBuilder
data class BpmnExclusiveGateway(
        override val id: String,
        val name: String?,
        val default: String?,
        val documentation: String?
): WithId