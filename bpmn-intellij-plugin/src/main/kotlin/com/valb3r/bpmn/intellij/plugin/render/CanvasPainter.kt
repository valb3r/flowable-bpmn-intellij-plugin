package com.valb3r.bpmn.intellij.plugin.render

import com.google.common.cache.Cache
import com.google.common.hash.Hashing
import com.intellij.util.ui.UIUtil
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.awt.geom.*
import java.awt.geom.AffineTransform.getTranslateInstance
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.text.AttributedCharacterIterator
import java.text.AttributedString
import javax.swing.Icon


class CanvasPainter(val graphics2D: Graphics2D, val camera: Camera, val svgCachedIcons: Cache<String, BufferedImage>) {

    private val iconMargin = 5.0f
    private val textMargin = 5.0f
    private val font = Font("Courier", Font.PLAIN, 10)
    private val arrowWidth = 10;
    private val arrowStyle = Polygon(intArrayOf(0, -arrowWidth, -arrowWidth), intArrayOf(0, 5, -5), 3)
    private val regularLineWidth = 2f
    private val nodeRadius = 25f

    fun drawZeroAreaLine(start: Point2D.Float, end: Point2D.Float, stroke: Stroke, color: Color): Area {
        val st = camera.toCameraView(Point2D.Float(start.x, start.y))
        val en = camera.toCameraView(Point2D.Float(end.x, end.y))

        val oldColor = graphics2D.color
        val oldStroke = graphics2D.stroke
        graphics2D.stroke = stroke
        graphics2D.color = color
        graphics2D.drawLine(st.x.toInt(), st.y.toInt(), en.x.toInt(), en.y.toInt())
        graphics2D.color = oldColor
        graphics2D.stroke = oldStroke
        return Area()
    }

    fun drawLine(start: WaypointElement, end: WaypointElement, color: Color): Area {
        val st = camera.toCameraView(Point2D.Float(start.x, start.y))
        val en = camera.toCameraView(Point2D.Float(end.x, end.y))

        graphics2D.color = color
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

    fun drawLineWithArrow(start: WaypointElement, end: WaypointElement, color: Color): Area {
        val st = camera.toCameraView(Point2D.Float(start.x, start.y))
        val en = camera.toCameraView(Point2D.Float(end.x, end.y))

        graphics2D.color = color
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

    fun drawCircle(center: WaypointElement, radius: Float, color: Color): Area {
        return drawCircle(center, radius, color, color)
    }

    fun drawCircle(center: WaypointElement, radius: Float, background: Color, border: Color): Area {
        val leftTop = camera.toCameraView(Point2D.Float(center.x - radius, center.y - radius))
        val rightBottom = camera.toCameraView(Point2D.Float(center.x + radius, center.y + radius))

        graphics2D.color = background
        val drawShape = Ellipse2D.Float(
                leftTop.x,
                leftTop.y,
                rightBottom.x - leftTop.x,
                rightBottom.y - leftTop.y
        )

        graphics2D.fill(drawShape)
        graphics2D.color = border
        graphics2D.draw(drawShape)

        return Area(drawShape)
    }

    fun drawCircle(shape: ShapeElement, background: Color, border: Color): Area {
        val leftTop = camera.toCameraView(Point2D.Float(shape.bounds.x, shape.bounds.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.bounds.x + shape.bounds.width, shape.bounds.y + shape.bounds.height))

        graphics2D.color = background
        val drawShape = Ellipse2D.Float(
                leftTop.x,
                leftTop.y,
                rightBottom.x - leftTop.x,
                rightBottom.y - leftTop.y
        )

        graphics2D.fill(drawShape)
        graphics2D.color = border
        graphics2D.draw(drawShape)

        return Area(drawShape)
    }

    fun drawCircle(center: Point2D.Float, radius: Float, color: Color): Area {
        return drawCircle(WaypointElement(center.x, center.y), radius, color, color)
    }

    fun drawRectNoFill(location: Point2D.Float, width: Float, height: Float, stroke: Stroke, color: Color): Area {
        val leftTop = camera.toCameraView(location)
        val rightBottom = camera.toCameraView(Point2D.Float(location.x + width, location.y + height))

        val oldStroke = graphics2D.stroke
        graphics2D.stroke = stroke
        graphics2D.color = color
        val drawShape = Rectangle2D.Float(
                leftTop.x,
                leftTop.y,
                (rightBottom.x - leftTop.x),
                (rightBottom.y - leftTop.y)
        )

        graphics2D.draw(drawShape)
        graphics2D.stroke = oldStroke
        return Area(drawShape)
    }

    fun drawRoundedRect(shape: ShapeElement, name: String?, background: Color, border: Color, textColor: Color): Area {
        val leftTop = camera.toCameraView(Point2D.Float(shape.bounds.x, shape.bounds.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.bounds.x + shape.bounds.width, shape.bounds.y + shape.bounds.height))

        graphics2D.color = background
        val drawShape = RoundRectangle2D.Float(
                leftTop.x,
                leftTop.y,
                (rightBottom.x - leftTop.x),
                (rightBottom.y - leftTop.y),
                (nodeRadius * this.camera.zoom.x),
                (nodeRadius * this.camera.zoom.y)
        )
        graphics2D.fill(drawShape)
        graphics2D.color = border
        graphics2D.draw(drawShape)
        graphics2D.color = textColor
        name?.apply { drawWrappedText(shape, this) }
        return Area(drawShape)
    }

    fun drawRoundedRectWithIcon(shape: ShapeElement, icon: Icon, name: String?, background: Color, border: Color, textColor: Color): Area {
        val leftTop = camera.toCameraView(Point2D.Float(shape.bounds.x, shape.bounds.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.bounds.x + shape.bounds.width, shape.bounds.y + shape.bounds.height))

        graphics2D.color = background
        val drawShape = RoundRectangle2D.Float(
                leftTop.x,
                leftTop.y,
                (rightBottom.x - leftTop.x),
                (rightBottom.y - leftTop.y),
                (nodeRadius * this.camera.zoom.x),
                (nodeRadius * this.camera.zoom.y)
        )
        graphics2D.fill(drawShape)
        graphics2D.color = border
        graphics2D.draw(drawShape)
        val cropTo = drawIconAndWrapShape(shape, icon)
        graphics2D.color = textColor
        name?.apply { drawWrappedText(cropTo, this) }
        return Area(drawShape)
    }

    fun drawIconAtScreen(position: Point2D.Float, icon: Icon): Area {
        val shape = Rectangle2D.Float(
                position.x,
                position.y,
                icon.iconWidth.toFloat(),
                icon.iconHeight.toFloat()
        )

        icon.paintIcon(null, graphics2D, position.x.toInt(), position.y.toInt())
        return Area(shape)
    }

    fun drawIcon(bounds: BoundsElement, svgIcon: String): Area {
        val leftTop = camera.toCameraView(Point2D.Float(bounds.x, bounds.y))

        val width = bounds.width.toInt()
        val height = bounds.height.toInt()

        if (0 == width || 0 == height) {
            return Area()
        }

        val highlightedShape = Rectangle2D.Float(
                leftTop.x,
                leftTop.y,
                width.toFloat(),
                height.toFloat()
        )

        val resizedImg = rasterizeSvg(svgIcon, width.toFloat(), height.toFloat(), UIUtil.isUnderDarcula())
        graphics2D.drawImage(resizedImg, leftTop.x.toInt(), leftTop.y.toInt(), width, height, null)

        return Area(highlightedShape)
    }

    fun drawWrappedIcon(shape: ShapeElement, svgIcon: String, selected: Boolean, selectedColor: Color): Area {
        val leftTop = camera.toCameraView(Point2D.Float(shape.bounds.x, shape.bounds.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.bounds.x + shape.bounds.width, shape.bounds.y + shape.bounds.height))

        val width = (rightBottom.x - leftTop.x).toInt()
        val height = (rightBottom.y - leftTop.y).toInt()

        if (0 == width || 0 == height) {
            return Area()
        }

        val highlightedShape = Rectangle2D.Float(
                leftTop.x,
                leftTop.y,
                (rightBottom.x - leftTop.x),
                (rightBottom.y - leftTop.y)
        )

        if (selected) {
            graphics2D.color = selectedColor
            graphics2D.fill(highlightedShape)
        }

        val resizedImg = rasterizeSvg(svgIcon, width.toFloat(), height.toFloat(), UIUtil.isUnderDarcula())
        graphics2D.drawImage(resizedImg, leftTop.x.toInt(), leftTop.y.toInt(), width, height, null)

        return Area(highlightedShape)
    }

    fun drawWrappedText(shape: ShapeElement, text: String) {
        if ("" == text) {
            return
        }

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

        if (height - textMargin < font.size) {
            return
        }

        while (lineMeasurer.position < paragraphEnd) {
            val layout = lineMeasurer.nextLayout(breakWidth)
            val drawPosX = leftTop.x + textMargin
            drawPosY += layout.ascent
            // Crop extra height and draw ellipsis
            if (drawPosY - leftTop.y + (layout.ascent + layout.descent + layout.leading) > height) {
                graphics2D.drawString("...", drawPosX, drawPosY)
                break
            }
            layout.draw(graphics2D, drawPosX, drawPosY)
            drawPosY += layout.descent + layout.leading
        }
    }

    fun drawIconAndWrapShape(shape: ShapeElement, icon: Icon): ShapeElement {
        val leftTop = camera.toCameraView(Point2D.Float(shape.bounds.x, shape.bounds.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.bounds.x + shape.bounds.width, shape.bounds.y + shape.bounds.height))
        if (rightBottom.x - leftTop.x < (iconMargin + icon.iconWidth)) {
            return shape
        }

        if (rightBottom.y - leftTop.y < (iconMargin + icon.iconHeight)) {
            return shape
        }

        icon.paintIcon(null, graphics2D, (leftTop.x + iconMargin).toInt(), (leftTop.y + iconMargin).toInt())

        val iconTop = camera.fromCameraView(Point2D.Float(leftTop.x, leftTop.y + iconMargin + icon.iconHeight))
        val iconBottom = camera.fromCameraView(Point2D.Float(rightBottom.x, rightBottom.y))

        return shape.copy(
                bounds = BoundsElement(
                        iconTop.x,
                        iconTop.y,
                        iconBottom.x - iconTop.x,
                        iconBottom.y - iconTop.y
                )
        )
    }

    fun rasterizeSvg(svgFile: String, width: Float, height: Float, invertColors: Boolean): BufferedImage {
        val cacheKey = Hashing.goodFastHash(32).hashString(svgFile + ":" + width.toInt() + "@" + height.toInt(), UTF_8).toString()

        return svgCachedIcons.get(cacheKey) {
            val imageTranscoder = BufferedImageTranscoder()
            imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, width)
            imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, height)

            val input = TranscoderInput(ByteArrayInputStream(svgFile.toByteArray(UTF_8)))
            imageTranscoder.transcode(input, null)

            if (!invertColors) {
                return@get imageTranscoder.bufferedImage!!
            } else {
                return@get invertColors(imageTranscoder.bufferedImage!!)
            }
        }
    }

    private fun invertColors(image: BufferedImage): BufferedImage {
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val rgba: Int = image.getRGB(x, y)
                var color = Color(rgba, true)
                color = Color(255 - color.red, 255 - color.green, 255 - color.blue, color.alpha)
                image.setRGB(x, y, color.rgb)
            }
        }

        return image
    }

    internal class BufferedImageTranscoder : ImageTranscoder() {
        override fun createImage(w: Int, h: Int): BufferedImage {
            return UIUtil.createImage(w, h, BufferedImage.TYPE_INT_ARGB)
        }

        override fun writeImage(img: BufferedImage, output: TranscoderOutput?) {
            bufferedImage = img
        }

        var bufferedImage: BufferedImage? = null
            private set
    }
}