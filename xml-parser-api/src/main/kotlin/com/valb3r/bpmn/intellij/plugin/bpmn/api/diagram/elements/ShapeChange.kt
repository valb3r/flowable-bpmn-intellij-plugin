package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

import java.awt.geom.Point2D

interface Translatable<T> {
    fun copyAndTranslate(dx: Float, dy: Float): T
}

interface Resizeable<T> {
    fun copyAndResize(transform: (Point2D.Float) -> Point2D.Float): T
}