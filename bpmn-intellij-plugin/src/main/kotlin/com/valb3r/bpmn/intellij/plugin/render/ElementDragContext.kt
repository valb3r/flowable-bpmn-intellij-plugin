package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import java.awt.geom.Point2D

data class ElementDragContext(val draggedIds: Set<DiagramElementId>, val start: Point2D.Float, val current: Point2D.Float)