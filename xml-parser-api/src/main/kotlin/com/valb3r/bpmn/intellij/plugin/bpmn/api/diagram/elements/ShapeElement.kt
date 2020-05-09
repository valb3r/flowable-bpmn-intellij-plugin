package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId

@KotlinBuilder
data class ShapeElement(
        val id: DiagramElementId,
        val bpmnElement: BpmnElementId,
        val bounds: BoundsElement
): Translatable<ShapeElement> {

    override fun copyAndTranslate(dx: Float, dy: Float): ShapeElement {
        return this.copy(bounds = BoundsElement(this.bounds.x + dx, this.bounds.y + dy, this.bounds.width, this.bounds.height))
    }
}

@KotlinBuilder
data class BoundsElement(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
)