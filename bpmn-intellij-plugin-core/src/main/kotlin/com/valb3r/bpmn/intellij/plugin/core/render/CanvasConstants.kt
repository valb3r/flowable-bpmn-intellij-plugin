package com.valb3r.bpmn.intellij.plugin.core.render

import com.intellij.util.ui.UIUtil
import java.awt.geom.Point2D

interface CanvasConstants {
   val epsilon: Float
   val anchorAttractionThreshold: Float
   val baseCursorSize: Float
   val defaultCameraOrigin: Point2D.Float
   val defaultZoomRatio: Float
}

data class DefaultCanvasConstants(
        override val epsilon: Float = 0.1f,
        override val anchorAttractionThreshold: Float = 5.0f,
        override val baseCursorSize: Float = if (UIUtil.isJreHiDPIEnabled()) 20.0f else 10.0f,
        override val defaultCameraOrigin: Point2D.Float = Point2D.Float(0f, 0f),
        override val defaultZoomRatio: Float = 1f
): CanvasConstants
