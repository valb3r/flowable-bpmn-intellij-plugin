package com.valb3r.bpmn.intellij.plugin.core.render

import com.google.common.cache.Cache
import com.google.common.hash.Hashing
import com.intellij.ui.paint.PaintUtil
import com.intellij.ui.scale.ScaleContext
import com.intellij.util.ui.UIUtil
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings
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
import java.text.BreakIterator
import javax.swing.Icon


class CanvasPainter(val graphics2D: Graphics2D, val camera: Camera, val svgCachedIcons: Cache<String, BufferedImage>) {

    private val iconMargin = 5.0f
    private val textMargin = 5.0f
    private val font = Font(currentSettings().uiFontName, Font.PLAIN, currentSettings().uiFontSize)
    private val arrowWidth = 10
    private val arrowStyle = Polygon(intArrayOf(0, -arrowWidth, -arrowWidth), intArrayOf(0, 5, -5), 3)
    private val arrowArea = Area(arrowStyle)
    private val regularLineWidth = currentSettings().lineThickness
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

    fun drawLine(start: Point2D.Float, end: Point2D.Float, color: Color): Area {
        val st = camera.toCameraView(start)
        val en = camera.toCameraView(end)

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

    fun drawLineWithArrow(start: Point2D.Float, end: Point2D.Float, color: Color): Area {
        val st = camera.toCameraView(start)
        val en = camera.toCameraView(end)

        graphics2D.color = color
        val transform = getTranslateInstance(en.x.toDouble(), en.y.toDouble())
        transform.rotate(en.x.toDouble() - st.x.toDouble(), en.y.toDouble() - st.y.toDouble())
        val arrow = arrowArea.clone() as Area
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

    fun drawLineSlash(start: Point2D.Float, end: Point2D.Float, color: Color): Area {
        val st = camera.toCameraView(start)
        val en = camera.toCameraView(end)

        val stLen = camera.toCameraView(Point2D.Float(0.0f, 0.0f))
        val enLen = camera.toCameraView(Point2D.Float(15.0f, 0.0f))

        graphics2D.color = color
        val transform = getTranslateInstance((st.x + (en.x - st.x) / 5.0f).toDouble(), (st.y + (en.y - st.y) / 5.0f).toDouble())
        transform.rotate(en.x.toDouble() - st.x.toDouble(), en.y.toDouble() - st.y.toDouble())
        transform.rotate(- Math.PI / 4.0f)
        val len = stLen.distance(enLen).toFloat()
        val line = Area(Rectangle2D.Float(
                -regularLineWidth / 4.0f,
                -len / 2.0f,
                regularLineWidth / 2.0f,
                len
        ))
        line.transform(transform)
        graphics2D.fill(line)
        return line
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

    fun drawEllipse(bounds: Rectangle2D.Float, background: Color, border: Color): Area {
        val leftTop = camera.toCameraView(Point2D.Float(bounds.x, bounds.y))
        val rightBottom = camera.toCameraView(Point2D.Float(bounds.x + bounds.width, bounds.y + bounds.height))

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

    fun drawRectNoCameraTransform(location: Point2D.Float, width: Float, height: Float, stroke: Stroke, color: Color): Area {
        val oldStroke = graphics2D.stroke
        graphics2D.stroke = stroke
        graphics2D.color = color
        val drawShape = Rectangle2D.Float(
                location.x,
                location.y,
                width,
                height
        )

        graphics2D.draw(drawShape)
        graphics2D.stroke = oldStroke
        return Area(drawShape)
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

    fun drawRoundedRect(shape: Rectangle2D.Float, name: String?, background: Color, border: Color, textColor: Color, borderStroke: Stroke? = null): Area {
        val leftTop = camera.toCameraView(Point2D.Float(shape.x, shape.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.x + shape.width, shape.y + shape.height))

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
        val oldStroke = graphics2D.stroke
        borderStroke?.apply { graphics2D.stroke = this }
        graphics2D.draw(drawShape)
        graphics2D.stroke = oldStroke
        graphics2D.color = textColor
        name?.apply { drawWrappedText(shape, this) }
        return Area(drawShape)
    }

    fun drawRoundedRectWithIconAtCorner(shape: Rectangle2D.Float, icon: Icon, name: String?, background: Color, border: Color, textColor: Color): Area {
        val leftTop = camera.toCameraView(Point2D.Float(shape.x, shape.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.x + shape.width, shape.y + shape.height))

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
        val cropTo = drawIconAtTopLeftAndWrapShape(shape, icon)
        graphics2D.color = textColor
        name?.apply { drawWrappedText(cropTo, this) }
        return Area(drawShape)
    }

    fun drawRoundedRectWithIconAtBottom(shape: Rectangle2D.Float, icon: Icon, name: String?, background: Color, border: Color, textColor: Color): Area {
        val leftTop = camera.toCameraView(Point2D.Float(shape.x, shape.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.x + shape.width, shape.y + shape.height))

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
        val cropTo = drawIconAtMidBottomAndWrapShape(shape, icon)
        graphics2D.color = textColor
        name?.apply { drawWrappedText(cropTo, this) }
        return Area(drawShape)
    }

    fun drawFilledIconAtScreen(position: Point2D.Float, icon: Icon, background: Color, border: Color? = null): Area {
        val shape = Rectangle2D.Float(
                position.x,
                position.y,
                icon.iconWidth.toFloat(),
                icon.iconHeight.toFloat()
        )

        graphics2D.color = background
        graphics2D.fill(shape)
        icon.paintIcon(null, graphics2D, position.x.toInt(), position.y.toInt())

        if (null != border) {
            graphics2D.color = border
            graphics2D.draw(shape)
        }
        return Area(shape)
    }

    fun drawIcon(position: Point2D.Float, icon: Icon, border: Color?): Area {
        val iconTop = camera.toCameraView(position)

        val shape = Rectangle2D.Float(
                iconTop.x,
                iconTop.y,
                icon.iconWidth.toFloat(),
                icon.iconHeight.toFloat()
        )

        icon.paintIcon(null, graphics2D, iconTop.x.toInt(), iconTop.y.toInt())
        if (null != border) {
            graphics2D.color = border
            graphics2D.draw(shape)
        }

        return Area(shape)
    }

    fun drawIconNoCameraTransform(bounds: BoundsElement, svgIcon: String): Area {
        val width = bounds.width.toInt()
        val height = bounds.height.toInt()

        if (0 == width || 0 == height) {
            return Area()
        }

        val highlightedShape = Rectangle2D.Float(
                bounds.x,
                bounds.y,
                width.toFloat(),
                height.toFloat()
        )

        val resizedImg = rasterizeSvg(svgIcon, width.toFloat(), height.toFloat(), UIUtil.isUnderDarcula())
        graphics2D.drawImage(resizedImg, bounds.x.toInt(), bounds.y.toInt(), width, height, null)

        return Area(highlightedShape)
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

    fun drawWrappedIconWithLayer(shape: Rectangle2D.Float, svgIcon: String, selected: Boolean, selectedColor: Color, layer: (Rectangle2D.Float) -> Shape, layerColor: Color): Area {
        val leftTop = camera.toCameraView(Point2D.Float(shape.x, shape.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.x + shape.width, shape.y + shape.height))

        val width = (rightBottom.x - leftTop.x).toInt()
        val height = (rightBottom.y - leftTop.y).toInt()

        if (0 == width || 0 == height) {
            return Area()
        }

        val resizedImg = rasterizeSvg(svgIcon, width.toFloat(), height.toFloat(), UIUtil.isUnderDarcula())
        val iconRect = Rectangle2D.Float(
                leftTop.x.toInt().toFloat(),
                leftTop.y.toInt().toFloat(),
                width.toFloat(),
                height.toFloat()
        )

        if (selected) {
            graphics2D.color = selectedColor
            graphics2D.fill(iconRect)
        } else {
            graphics2D.color = layerColor
            graphics2D.fill(layer(iconRect))
        }

        graphics2D.drawImage(resizedImg, leftTop.x.toInt(), leftTop.y.toInt(), width, height, null)

        return Area(iconRect)
    }

    fun drawWrappedIcon(shape: Rectangle2D.Float, svgIcon: String, selected: Boolean, selectedColor: Color): Area {
        val leftTop = camera.toCameraView(Point2D.Float(shape.x, shape.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.x + shape.width, shape.y + shape.height))

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

    fun drawText(location: Point2D.Float, text: String, textColor: Color): Area {
        if ("" == text) {
            return Area()
        }

        val textLocation = camera.toCameraView(location)

        graphics2D.font = font // for ellipsis
        graphics2D.color = textColor
        graphics2D.drawString(text, textLocation.x.toInt(), textLocation.y.toInt())
        val rect = graphics2D.fontMetrics.getStringBounds(text, graphics2D)
        return Area(Rectangle2D.Float(textLocation.x + rect.x.toFloat(), textLocation.y + rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat()))
    }

    fun drawTextNoCameraTransform(location: Point2D.Float, text: String, textColor: Color, background: Color? = null) {
        val textLocation = camera.toCameraView(location)
        if ("" == text) {
            return
        }

        val rect = graphics2D.fontMetrics.getStringBounds(text, graphics2D)
        background.apply {
            graphics2D.color = background
            graphics2D.fill(Ellipse2D.Float(
                    textLocation.x + rect.x.toFloat(),
                    textLocation.y + rect.y.toFloat(),
                    rect.width.toFloat(),
                    rect.height.toFloat()

            ))
        }
        graphics2D.font = font // for ellipsis
        graphics2D.color = textColor
        graphics2D.drawString(text, textLocation.x.toInt(), textLocation.y.toInt())
    }

    fun drawWrappedSingleLine(start: Point2D.Float, end: Point2D.Float, text: String, textColor: Color) {
        if ("" == text) {
            return
        }

        val st = camera.toCameraView(start)
        val en = camera.toCameraView(end)
        val maxWidth = en.distance(st).toFloat()
        graphics2D.font = font // for ellipsis
        val rotTransform = AffineTransform()

        if (maxWidth < font.size * 2) {
            return
        }

        rotTransform.translate(st.x.toDouble(), st.y.toDouble())
        rotTransform.rotate(en.x.toDouble() - st.x.toDouble(), en.y.toDouble() - st.y.toDouble())
        rotTransform.translate(textMargin.toDouble(), -textMargin.toDouble())
        val rotatedFont: Font = font.deriveFont(rotTransform)
        graphics2D.font = rotatedFont

        val attributedString = AttributedString(text, mutableMapOf(TextAttribute.FONT to rotatedFont))
        val paragraph: AttributedCharacterIterator = attributedString.iterator
        val paragraphStart = paragraph.beginIndex
        val frc: FontRenderContext = graphics2D.fontRenderContext
        val lineMeasurer = LineBreakMeasurer(paragraph, BreakIterator.getCharacterInstance(), frc)


        val layout = lineMeasurer.nextLayout(maxWidth - 10.0f)
        lineMeasurer.position = paragraphStart

        graphics2D.color = textColor
        layout.draw(graphics2D, 0.0f, 0.0f)
        graphics2D.font = font
    }

    fun drawWrappedText(shape: Rectangle2D.Float, text: String) {
        if ("" == text) {
            return
        }

        val leftTop = camera.toCameraView(Point2D.Float(shape.x, shape.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.x + shape.width, shape.y + shape.height))

        graphics2D.font = font // for ellipsis
        val attributedString = AttributedString(text, mutableMapOf(TextAttribute.FONT to font))
        val paragraph: AttributedCharacterIterator = attributedString.iterator
        val paragraphStart = paragraph.beginIndex
        val paragraphEnd = paragraph.endIndex
        val frc: FontRenderContext = graphics2D.fontRenderContext
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

    fun drawIconAtTopLeftAndWrapShape(shape: Rectangle2D.Float, icon: Icon): Rectangle2D.Float {
        val leftTop = camera.toCameraView(Point2D.Float(shape.x, shape.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.x + shape.width, shape.y + shape.height))
        if (rightBottom.x - leftTop.x < (iconMargin + icon.iconWidth)) {
            return shape
        }

        if (rightBottom.y - leftTop.y < (iconMargin + icon.iconHeight)) {
            return shape
        }

        icon.paintIcon(null, graphics2D, (leftTop.x + iconMargin).toInt(), (leftTop.y + iconMargin).toInt())

        val iconTop = camera.fromCameraView(Point2D.Float(leftTop.x, leftTop.y + iconMargin + icon.iconHeight))
        val iconBottom = camera.fromCameraView(Point2D.Float(rightBottom.x, rightBottom.y))

        return Rectangle2D.Float(
                    iconTop.x,
                    iconTop.y,
                    iconBottom.x - iconTop.x,
                    iconBottom.y - iconTop.y
        )
    }

    fun drawIconAtMidBottomAndWrapShape(shape: Rectangle2D.Float, icon: Icon): Rectangle2D.Float {
        val leftTop = camera.toCameraView(Point2D.Float(shape.x, shape.y))
        val rightBottom = camera.toCameraView(Point2D.Float(shape.x + shape.width, shape.y + shape.height))
        if (rightBottom.x - leftTop.x < (iconMargin + icon.iconWidth)) {
            return shape
        }

        if (rightBottom.y - leftTop.y < (iconMargin + icon.iconHeight)) {
            return shape
        }

        val iconX = (leftTop.x + rightBottom.x - icon.iconWidth) / 2.0f
        val iconY = rightBottom.y - iconMargin - icon.iconHeight
        icon.paintIcon(null, graphics2D, iconX.toInt(), iconY.toInt())

        val shapeBottom = camera.fromCameraView(Point2D.Float(iconX, iconY))

        return Rectangle2D.Float(
                shape.x,
                shape.y,
                shape.width,
                shapeBottom.y - shape.y
        )
    }

    fun rasterizeSvg(svgFile: String, width: Float, height: Float, invertColors: Boolean): BufferedImage {
        val cacheKey = Hashing.goodFastHash(32).hashString(svgFile + ":" + width.toInt() + "@" + height.toInt() + "@" + invertColors, UTF_8).toString()

        return svgCachedIcons.get(cacheKey) {
            val imageTranscoder = BufferedImageTranscoder()
            imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, width * 2.0f)
            imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, height * 2.0f)

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