package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.github.pozo.KotlinBuilder

@KotlinBuilder
data class BpmnOutExtensionElement(
        val source: String,
        val target: String
) : BpmnExtensionElement()