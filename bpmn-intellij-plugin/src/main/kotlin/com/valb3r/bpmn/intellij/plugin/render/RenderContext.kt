package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView

data class RenderContext(val canvas: CanvasPainter, val selectedIds: Set<String>, val dragContext: ElementDragContext, val diagram: BpmnProcessObjectView?)