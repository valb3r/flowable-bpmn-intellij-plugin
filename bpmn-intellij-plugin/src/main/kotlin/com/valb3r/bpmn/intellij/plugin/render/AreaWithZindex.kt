package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import java.awt.geom.Area

const val DEFAULT_Z_INDEX = 10000
const val ANCHOR_Z_INDEX = 1000

data class AreaWithZindex(val area: Area, val index: Int = DEFAULT_Z_INDEX, val parentToSelect: DiagramElementId? = null)