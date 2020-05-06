package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.psi.PsiFile
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser

class CanvasBuilder {

    fun build(canvas: Canvas, bpmnFile: PsiFile) {
        bpmnFile.virtualFile.inputStream.use {
            val process = FlowableParser().parse(it)
            canvas.reset(process.toView(), BpmnProcessRenderer())
        }
    }
}