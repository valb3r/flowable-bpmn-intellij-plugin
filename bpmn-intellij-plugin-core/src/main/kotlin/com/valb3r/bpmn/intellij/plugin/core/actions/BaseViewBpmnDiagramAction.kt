package com.valb3r.bpmn.intellij.plugin.core.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings

abstract class BaseViewBpmnDiagramAction : AnAction() {

    abstract val toolWindowName: String

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        val file = psiElement(anActionEvent)?.containingFile ?: return
        if (!isValidFileName(file.name)) {
            notificationBalloon(project, "Invalid file name: ${file.name}", NotificationType.ERROR)
            return
        }

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolWindowName)!!
        toolWindow.title = file.name
        toolWindow.activate {
            generateContent(project, file)
        }
    }

    override fun update(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
        val psiElement = psiElement(anActionEvent)
        anActionEvent.presentation.isEnabledAndVisible = project != null && isValidFileName(psiElement?.containingFile?.name)
    }

    protected abstract fun generateContent(project: Project, file: PsiFile)
    protected abstract fun notificationBalloon(project: Project, message: String, type: NotificationType)

    protected fun psiElement(anActionEvent: AnActionEvent) = anActionEvent.getData(CommonDataKeys.PSI_FILE)

    protected fun isValidFileName(fileName: String?): Boolean {
        val name = fileName ?: return false
        val allowedExt = currentSettings().openExtensions
        return allowedExt.any { name.endsWith(it) }
    }
}
