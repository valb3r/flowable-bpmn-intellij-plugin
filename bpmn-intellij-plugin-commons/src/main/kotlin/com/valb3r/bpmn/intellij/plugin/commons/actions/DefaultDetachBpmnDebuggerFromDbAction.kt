package com.valb3r.bpmn.intellij.plugin.commons.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.valb3r.bpmn.intellij.plugin.core.CANVAS_PAINT_TOPIC
import com.valb3r.bpmn.intellij.plugin.core.debugger.detachDebugger

abstract class DefaultDetachBpmnDebuggerFromDbAction : AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        detachDebugger(project)
        ApplicationManager.getApplication().invokeLater {
            anActionEvent.project!!.messageBus.syncPublisher(CANVAS_PAINT_TOPIC).repaint()
        }
    }

    override fun update(anActionEvent: AnActionEvent) {
        // NOP
    }


    private fun psiElements(anActionEvent: AnActionEvent) =
            anActionEvent.getData(LangDataKeys.PSI_ELEMENT_ARRAY)
}