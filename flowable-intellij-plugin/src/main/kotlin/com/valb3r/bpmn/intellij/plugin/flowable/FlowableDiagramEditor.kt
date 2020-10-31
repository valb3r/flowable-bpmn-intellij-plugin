package com.valb3r.bpmn.intellij.plugin.flowable

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.valb3r.bpmn.intellij.plugin.BpmnPluginToolWindow
import com.valb3r.bpmn.intellij.plugin.commons.langinjection.registerCurrentFile
import com.valb3r.bpmn.intellij.plugin.core.MouseEventHandler
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.registerPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.core.xmlnav.registerXmlNavigator
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.ui.components.popupmenu.FlowableCanvasPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.flowable.xmlnav.FlowableXmlNavigator
import java.beans.PropertyChangeListener
import javax.swing.JComponent


class FlowableBpmnDiagramEditor(private val project: Project, protected val markdownFile: VirtualFile, document: Document) : UserDataHolderBase(), Disposable, FileEditor {
    protected var previewIsUpToDate = false
    var isPreviewIsSelected = false
        protected set
    protected var document: Document?
    protected var mouseEventHandler: MouseEventHandler? = null

    override fun getState(fileEditorStateLevel: FileEditorStateLevel): FileEditorState {
        return FileEditorState.INSTANCE
    }

    override fun getName(): String {
        return "Flowable BPMN111"
    }

    override fun getComponent(): JComponent {
        val bpmnWindow = BpmnPluginToolWindow(FlowableParser()) {
            registerCurrentFile(it)
            registerXmlNavigator(FlowableXmlNavigator(project))
        }

        // register the call graph tool window as a project service, so it can be accessed by editor menu actions.
        val windowService = ServiceManager.getService(project, FlowableBpmnPluginToolWindowProjectService::class.java)
        windowService.bpmnToolWindow = bpmnWindow
        return bpmnWindow.getContent()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return component
    }

    override fun setState(fileEditorState: FileEditorState) {
        //empty
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun isValid(): Boolean {
        return document != null
    }

    /**
     * @return if preview should be updated even if it's not selected
     */
    val isImmediateUpdate: Boolean
        get() = false

    /**
     * Invoked when the editor is selected.
     */
    override fun selectNotify() {
        isPreviewIsSelected = true
        if (!previewIsUpToDate) {
            updatePreview()
        }
    }

    /**
     * Invoked when the editor is deselected.
     */
    override fun deselectNotify() {
        isPreviewIsSelected = false
    }

    override fun addPropertyChangeListener(propertyChangeListener: PropertyChangeListener) {
        //empty
    }

    override fun removePropertyChangeListener(propertyChangeListener: PropertyChangeListener) {
        //empty
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        return null
    }

    override fun getCurrentLocation(): FileEditorLocation? {
        return null
    }

    override fun getStructureViewBuilder(): StructureViewBuilder? {
        return null
    }

    override fun dispose() {
        Disposer.dispose(this)
        if (mouseEventHandler != null) {
            component.removeMouseListener(mouseEventHandler)
        }
    }

    /**
     * Updates preview ignoring com.github.shyykoserhiy.gfm.editor.AbstractGfmPreview#previewIsUpToDate value
     */
    fun updatePreview() {
        previewIsUpToDate = true //todo
        // markdownParser.queueMarkdownHtmlRequest(markdownFile.parent.canonicalPath, markdownFile.name, document!!.text, true)
    }


    protected fun addPopupListener() {
        mouseEventHandler = MouseEventHandler(currentCanvas())
        component.addMouseListener(mouseEventHandler)
    }


    private fun addListeners() {
        // Listen to the document modifications.
        /*val documentListener: DocumentListener = object : DocumentListener() {
            override fun documentChanged(e: DocumentEvent) {
                previewIsUpToDate = false
                if (isPreviewIsSelected || isImmediateUpdate) { //todo offline only?
                    selectNotify()
                }
            }
        }
        document!!.addDocumentListener(documentListener, this)
        settings.addGlobalSettingsChangedListener(settingsChangedListener, this)*/
        addPopupListener()
    }


    init {
        registerPopupMenuProvider(FlowableCanvasPopupMenuProvider())
        registerNewElementsFactory(FlowableObjectFactory())
        this.document = document
        addListeners()
    }
}