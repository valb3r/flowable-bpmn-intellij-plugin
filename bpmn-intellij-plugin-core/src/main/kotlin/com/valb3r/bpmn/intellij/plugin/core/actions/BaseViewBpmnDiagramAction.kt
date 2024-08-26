package com.valb3r.bpmn.intellij.plugin.core.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings

abstract class BaseViewBpmnDiagramAction : AnAction() {

    abstract val toolWindowName: String

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        val file = psiElement(anActionEvent)?.containingFile ?: return
        if (!isValidFileName(file.name)) {
            notificationBalloon(project, "Invalid file name: ${file.name}", NotificationType.ERROR)
            return
        }

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolWindowName)!!
        toolWindow.activate {
            val task = object : Backgroundable(project, "Loading tool window") {
                override fun run(indicator: ProgressIndicator) {
                    indicator.text = "Opening ${file.name}"
                    generateContent(project, file)
                    indicator.text = "Successfully opened ${file.name}"
                    invokeLater { toolWindow.title = file.name }
                }
            }

            val indicator = BackgroundableProcessIndicator(task)
            indicator.isIndeterminate = true

            invokeLater { toolWindow.title = "[Loading...] ${file.name}" }
            ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, indicator)
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
