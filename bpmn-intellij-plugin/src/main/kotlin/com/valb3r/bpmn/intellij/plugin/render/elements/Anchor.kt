package com.valb3r.bpmn.intellij.plugin.render.elements

import java.awt.geom.Point2D

data class Anchor(val point: Point2D.Float, val priority: Int = 0)