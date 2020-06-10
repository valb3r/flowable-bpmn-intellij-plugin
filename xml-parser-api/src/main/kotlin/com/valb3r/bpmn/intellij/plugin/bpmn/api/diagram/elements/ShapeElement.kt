package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

@KotlinBuilder
data class ShapeElement(
        override val id: DiagramElementId,
        val bpmnElement: BpmnElementId,
        private val bounds: BoundsElement
): Translatable<ShapeElement>, Resizeable<ShapeElement>, WithDiagramId {

    override fun copyAndTranslate(dx: Float, dy: Float): ShapeElement {
        return this.copy(bounds = BoundsElement(this.bounds.x + dx, this.bounds.y + dy, this.bounds.width, this.bounds.height))
    }

    override fun copyAndResize(transform: (Point2D.Float) -> Point2D.Float): ShapeElement {
        val left = Point2D.Float(this.bounds.x, this.bounds.y)
        val right = Point2D.Float(this.bounds.x + this.bounds.width, this.bounds.y + this.bounds.height)
        val newLeft = transform(left)
        val newRight = transform(right)

        return this.copy(bounds = BoundsElement(
                newLeft.x,
                newLeft.y,
                newRight.x - newLeft.x,
                newRight.y - newLeft.y
        ))
    }

    fun bounds(): Pair<ShapeBoundsAnchorElement, ShapeBoundsAnchorElement> {
        return Pair(
                ShapeBoundsAnchorElement(id, bpmnElement, DiagramElementId("0:" + id.id), this.bounds.x, this.bounds.y),
                ShapeBoundsAnchorElement(id, bpmnElement, DiagramElementId("1:" + id.id), this.bounds.x + this.bounds.width, this.bounds.y + this.bounds.height)
        )
    }

    fun rectBounds(): Rectangle2D.Float {
        return Rectangle2D.Float(this.bounds.x, this.bounds.y, this.bounds.width, this.bounds.height)
    }
}

@KotlinBuilder
data class BoundsElement(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
)

@KotlinBuilder
data class ShapeBoundsAnchorElement(
        val parentId: DiagramElementId,
        val bpmnElement: BpmnElementId,
        override val id: DiagramElementId,
        val x: Float,
        val y: Float
): Translatable<ShapeBoundsAnchorElement>, WithDiagramId {

    override fun copyAndTranslate(dx: Float, dy: Float): ShapeBoundsAnchorElement {
        return this.copy(x = this.x + dx, y = this.y + dy)
    }
}