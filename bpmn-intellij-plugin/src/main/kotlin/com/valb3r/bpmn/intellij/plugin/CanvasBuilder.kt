package com.valb3r.bpmn.intellij.plugin

import com.intellij.psi.PsiFile
import com.intellij.ui.EditorTextField
import com.valb3r.bpmn.intellij.plugin.events.setUpdateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.newelements.NewElementsProvider
import com.valb3r.bpmn.intellij.plugin.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.render.BpmnProcessRenderer
import com.valb3r.bpmn.intellij.plugin.render.Canvas
import javax.swing.JTable

class CanvasBuilder {

    private var newObjectsFactory: NewElementsProvider? = null

    fun build(properties: JTable, editorFactory: (value: String) -> EditorTextField, canvas: Canvas, bpmnFile: PsiFile) {
        val parser = FlowableParser()
        setUpdateEventsRegistry(parser, bpmnFile.project, bpmnFile.virtualFile)
        updateEventsRegistry().reset()
        bpmnFile.virtualFile.inputStream.use {
            val process = parser.parse(it)
            newObjectsFactory = newElementsFactory(FlowableObjectFactory())
            canvas.reset(properties, editorFactory, process.toView(newObjectsFactory!!), BpmnProcessRenderer())
        }
    }
}