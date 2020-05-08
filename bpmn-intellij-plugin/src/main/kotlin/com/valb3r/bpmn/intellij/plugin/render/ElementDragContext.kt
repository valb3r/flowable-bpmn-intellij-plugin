package com.valb3r.bpmn.intellij.plugin.render

import java.awt.geom.Point2D

data class ElementDragContext(val draggedIds: Set<String>, val start: Point2D.Float, val current: Point2D.Float)