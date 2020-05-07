package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.awt.geom.AffineTransform.getTranslateInstance
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.text.AttributedCharacterIterator
import java.text.AttributedString


class CanvasPainter(val graphics2D: Graphics2D, val camera: Camera) {

    private val textMargin = 5.0f
    private val font = Font("Courier", Font.PLAIN, 12)
    private val arrowWidth = 10;
    private val arrowStyle = Polygon(intArrayOf(0, -arrowWidth, -arrowWidth), intArrayOf(0, 5, -5), 3)
    private val arrowOpenAngle = Math.toRadians(15.0)
    private val regularLineWidth = 2f

    private val nodeRadius = 25f
    private val solidLineStroke = BasicStroke(regularLineWidth)

    fun drawGraphicsLine(start: WaypointElement, end: WaypointElement, color: Colors): Area {
        val st = camera.toCameraView(Point2D.Float(start.x, start.y))
        val en = camera.toCameraView(Point2D.Float(end.x, end.y))

        graphics2D.color = color.color
        val transform = getTranslateInstance(en.x.toDouble(), en.y.toDouble())
        transform.rotate(en.x.toDouble() - st.x.toDouble(), en.y.toDouble() - st.y.toDouble())
        val lineLen = en.distance(st).toFloat()
        val line = Area(Rectangle2D.Float(
                -lineLen,
                -regularLineWidth / 2.0f,
                lineLen,
                regularLineWidth
        ))
        line.transform(transform)
        graphics2D.fill(line)
        return line
    }

    fun drawGraphicsLineWithArrow(start: WaypointElement, end: WaypointElement, color: Colors): Area {
        val st = camera.toCameraView(Point2D.Float(start.x, start.y))
        val en = camera.toCameraView(Point2D.Float(end.x, end.y))

        graphics2D.color = color.color
        val transform = getTranslateInstance(en.x.toDouble(), en.y.toDouble())
        transform.rotate(en.x.toDouble() - st.x.toDouble(), en.y.toDouble() - st.y.toDouble())
        val arrow = Area(arrowStyle)
        val lineLen = en.distance(st).toFloat()
        val line = Area(Rectangle2D.Float(
                -lineLen,
                -regularLineWidth / 2.0f,
                lineLen - arrowWidth / 2.0f,
                regularLineWidth
        ))
        arrow.add(line)
        arrow.transform(transform)
        graphics2D.fill(arrow)
        return arrow
    }

    fun drawGraphicsRoundedRect(shape: ShapeElement, name: String?, color: Colors): Area {
        val leftTop = camera.toCameraView(Point2D.Float(shape.bounds.x, shape.bounds.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.bounds.x + shape.bounds.width, shape.bounds.y + shape.bounds.height))

        graphics2D.color = color.color
        val drawShape = RoundRectangle2D.Float(
                leftTop.x,
                leftTop.y,
                (rightBottom.x - leftTop.x),
                (rightBottom.y - leftTop.y),
                (nodeRadius * this.camera.zoom.x),
                (nodeRadius * this.camera.zoom.y)
        )
        graphics2D.fill(drawShape)
        graphics2D.color = Color.GRAY
        graphics2D.draw(drawShape)
        graphics2D.color = Color.BLACK
        name?.apply { drawWrappedText(shape, this) }
        return Area(drawShape)
    }

    fun drawWrappedText(shape: ShapeElement, text: String) {
        val leftTop = camera.toCameraView(Point2D.Float(shape.bounds.x, shape.bounds.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.bounds.x + shape.bounds.width, shape.bounds.y + shape.bounds.height))

        graphics2D.font = font // for ellipsis
        val attributedString = AttributedString(text, mutableMapOf(TextAttribute.FONT to font))
        val paragraph: AttributedCharacterIterator = attributedString.iterator
        val paragraphStart = paragraph.beginIndex
        val paragraphEnd = paragraph.endIndex
        val frc: FontRenderContext = graphics2D.getFontRenderContext()
        val lineMeasurer = LineBreakMeasurer(paragraph, frc)

        val height = rightBottom.y - leftTop.y
        val breakWidth = rightBottom.x - leftTop.x - 2.0f * textMargin
        var drawPosY = leftTop.y + textMargin
        lineMeasurer.position = paragraphStart

        // avoid non-fitting values:
        if (breakWidth < font.size * 2) {
            return
        }

        while (lineMeasurer.position < paragraphEnd) {
            val layout = lineMeasurer.nextLayout(breakWidth)
            val drawPosX = leftTop.x + textMargin
            drawPosY += layout.ascent
            layout.draw(graphics2D, drawPosX, drawPosY)
            drawPosY += layout.descent + layout.leading

            // Crop extra height and draw ellipsis
            if (drawPosY - leftTop.y + 2 * (layout.ascent + layout.descent + layout.leading) > height) {
                graphics2D.drawString("...", drawPosX, drawPosY + layout.ascent)
                break
            }
        }
    }
}