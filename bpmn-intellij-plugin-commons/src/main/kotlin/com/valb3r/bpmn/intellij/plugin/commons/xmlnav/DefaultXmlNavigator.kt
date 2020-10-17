package com.valb3r.bpmn.intellij.plugin.commons.xmlnav

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.commons.langinjection.getCurrentFile
import com.valb3r.bpmn.intellij.plugin.core.xmlnav.XmlNavigator
import java.nio.charset.StandardCharsets

class DefaultXmlNavigator(private val project: Project): XmlNavigator {

    override fun jumpTo(id: BpmnElementId) {
        ApplicationManager.getApplication().invokeLater {
            val virtualFile = getCurrentFile().virtualFile
            val descriptor = OpenFileDescriptor(project, virtualFile)
            val fileEditor = FileEditorManager.getInstance(project).openEditor(descriptor, true).firstOrNull()
            fileEditor?.apply {
                val ctx = DataManager.getInstance().getDataContext(this.component)
                val editor = ctx.getData(PlatformDataKeys.EDITOR)
                editor?.caretModel?.moveToOffset(findOffset(id, virtualFile))
                editor?.scrollingModel?.scrollToCaret(ScrollType.MAKE_VISIBLE)
            }
        }
    }

    private fun findOffset(id: BpmnElementId, file: VirtualFile): Int {
        val regex = "id\\s*=\\s*\"${id.id}".toRegex()
        val pos = regex.find(String(file.contentsToByteArray(), StandardCharsets.UTF_8)) ?: return 0
        val firstMatch = pos.groups.firstOrNull() ?: return 0
        return firstMatch.range.first
    }
}