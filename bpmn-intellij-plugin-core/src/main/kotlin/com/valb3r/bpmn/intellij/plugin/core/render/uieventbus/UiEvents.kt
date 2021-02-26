package com.valb3r.bpmn.intellij.plugin.core.render.uieventbus

import java.awt.geom.Rectangle2D

interface UiEvent

class ZoomInEvent: UiEvent
class ZoomOutEvent: UiEvent
class CenterModelEvent: UiEvent
class ResetAndCenterEvent: UiEvent
data class ViewRectangleChangeEvent(val onScreenModel: Rectangle2D.Float): UiEvent
