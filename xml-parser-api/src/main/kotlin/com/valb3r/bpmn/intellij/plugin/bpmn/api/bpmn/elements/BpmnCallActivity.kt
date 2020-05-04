package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.github.pozo.KotlinBuilder

@KotlinBuilder
data class BpmnCallActivity(
        override val id: String,
        val name: String?,
        val documentation: String?,
        val async: Boolean?,
        val calledElement: String,
        val calledElementType: String?,
        val inheritVariables: Boolean?,
        val fallbackToDefaultTenant: Boolean?,
        val extensionElements: ExtensionElements?
) : WithId

@KotlinBuilder
data class ExtensionElements(
        val out: List<OutExtensionElement>?
)

@KotlinBuilder
data class OutExtensionElement(val source: String?, val target: String?)