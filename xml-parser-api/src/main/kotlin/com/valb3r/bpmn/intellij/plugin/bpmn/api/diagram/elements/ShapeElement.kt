package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

import com.github.pozo.KotlinBuilder

@KotlinBuilder
data class ShapeElement(
        val id: String,
        val bpmnElement: String,
        val bounds: BoundsElement
)

@KotlinBuilder
data class BoundsElement(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
)