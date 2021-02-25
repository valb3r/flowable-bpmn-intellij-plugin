package com.valb3r.bpmn.intellij.plugin.core.render.uieventbus

import java.awt.geom.Rectangle2D

data class ViewRectangleChangeEvent(val onScreenModel: Rectangle2D.Float): UiEvent
