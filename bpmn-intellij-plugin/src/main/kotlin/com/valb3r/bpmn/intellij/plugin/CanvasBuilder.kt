package com.valb3r.bpmn.intellij.plugin

import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import java.io.File

class CanvasBuilder {

    fun build(canvas: Canvas, file: File) {
        file.inputStream().use {
            val process = FlowableParser().parse(it)
            canvas.reset(process.toView(), BpmnProcessRenderer())
        }
    }
}