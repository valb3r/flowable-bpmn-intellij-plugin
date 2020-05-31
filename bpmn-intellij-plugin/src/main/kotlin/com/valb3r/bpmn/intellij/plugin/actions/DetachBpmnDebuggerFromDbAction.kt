package com.valb3r.bpmn.intellij.plugin.actions

import com.intellij.database.model.DasNamespace
import com.intellij.database.psi.DbElement
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.valb3r.bpmn.intellij.plugin.debugger.currentDebugger
import com.valb3r.bpmn.intellij.plugin.debugger.detachDebugger

class DetachBpmnDebuggerFromDbAction : AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        anActionEvent.project ?: return
        detachDebugger()
    }

    override fun update(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
        anActionEvent.presentation.isEnabledAndVisible =
                project != null && null != properElem(anActionEvent) && (null != currentDebugger())
    }

    private fun properElem(anActionEvent: AnActionEvent): DbElement? {
        return psiElements(anActionEvent)
                ?.filterIsInstance<DasNamespace>()
                ?.filterIsInstance<DbElement>()
                ?.firstOrNull()
    }

    private fun psiElements(anActionEvent: AnActionEvent) =
            anActionEvent.getData(LangDataKeys.PSI_ELEMENT_ARRAY)
}