package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

import com.github.pozo.KotlinBuilder

@KotlinBuilder
data class EdgeElement (
        val id: String,
        val bpmnElement: String?,
        val waypoint: List<WaypointElement>?
)

@KotlinBuilder
data class WaypointElement (
        val x: Float,
        val y: Float
)