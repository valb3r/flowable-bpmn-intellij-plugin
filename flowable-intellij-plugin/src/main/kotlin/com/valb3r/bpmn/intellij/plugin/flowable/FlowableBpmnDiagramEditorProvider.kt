package com.valb3r.bpmn.intellij.plugin.flowable

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element

class FlowableBpmnDiagramEditorProvider : FileEditorProvider {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.name.endsWith("bpmn20.xml")
    }

    override fun createEditor(project: Project, virtualFile: VirtualFile): FileEditor {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        return FlowableBpmnDiagramEditor(project, virtualFile, document!!)
    }

    override fun disposeEditor(fileEditor: FileEditor) {
        fileEditor.dispose()
    }

    override fun readState(element: Element, project: Project, virtualFile: VirtualFile): FileEditorState {
        return FileEditorState.INSTANCE
    }

    override fun writeState(fileEditorState: FileEditorState, project: Project, element: Element) {
        //nothing to do here. Preview is stateless.
    }

    override fun getEditorTypeId(): String {
        return EDITOR_TYPE_ID
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
    }

    companion object {
        private val EDITOR_TYPE_ID: String = "gfm.editor.type"
    }
}