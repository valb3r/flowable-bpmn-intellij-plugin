package com.valb3r.bpmn.intellij.plugin.core.render.uieventbus

import com.valb3r.bpmn.intellij.plugin.core.render.Camera
import java.awt.geom.Rectangle2D

data class CameraChangeEvent(val camera: Camera): UiEvent
data class ModelRectangleChangeEvent(val model: Rectangle2D.Float): UiEvent
