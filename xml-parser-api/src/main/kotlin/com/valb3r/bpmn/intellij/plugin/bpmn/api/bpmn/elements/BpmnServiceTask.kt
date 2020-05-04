package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.github.pozo.KotlinBuilder

@KotlinBuilder
data class BpmnServiceTask(
        override val id: String,
        val name: String?,
        val documentation: String?,
        val async: Boolean?,
        val exclusive: Boolean?,
        val delegateExpression: String?,
        val triggerable: Boolean?
): WithId