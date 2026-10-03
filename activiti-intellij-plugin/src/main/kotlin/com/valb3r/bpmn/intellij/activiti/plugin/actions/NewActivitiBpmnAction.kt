package com.valb3r.bpmn.intellij.activiti.plugin.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiDirectory
import com.valb3r.bpmn.intellij.plugin.core.render.currentIconProvider
import java.util.*

private const val NEW_FILE = "New Activiti 6.x BPMN 2.0 file"

class NewActivitiBpmnAction:
        CreateFileFromTemplateAction(
            NEW_FILE,
            "Create new Activiti BPMN process",
            currentIconProvider().script), DumbAware {

    companion object {
        fun createProperties(project: Project, processName: String): Properties {
            val properties = FileTemplateManager.getInstance(project).defaultProperties
            properties += "PROCESS_NAME" to processName
            return properties
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?) = NEW_FILE

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
                .setTitle(NEW_FILE)
                .addKind("File", currentIconProvider().script, "new-activiti-diagram.bpmn20.xml")
    }

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory) = try {
        val fileName = FileUtilRt.getNameWithoutExtension(name)
        val project = dir.project
        val properties = createProperties(project, fileName)
        CreateFromTemplateDialog(project, dir, template, AttributesDefaults("$fileName.bpmn20").withFixedName(true), properties)
                .create()
                .containingFile
    } catch (e: Exception) {
        LOG.error("Error while creating new file", e)
        null
    }
}