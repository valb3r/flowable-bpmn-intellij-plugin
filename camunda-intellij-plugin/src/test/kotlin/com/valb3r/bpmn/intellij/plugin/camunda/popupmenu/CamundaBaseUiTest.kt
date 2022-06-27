package com.valb3r.bpmn.intellij.plugin.camunda.popupmenu

import com.nhaarman.mockitokotlin2.*
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.ui.components.popupmenu.CamundaCanvasPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.CopyPasteActionHandler
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.DATA_FLAVOR
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.SystemClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.setCopyPasteActionHandler
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.registerPopupMenuProvider
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.atomic.AtomicReference

abstract class CamundaBaseUiTest: BaseUiTest() {

    protected val buffer: AtomicReference<String> = AtomicReference()

    @BeforeEach
    fun init() {
        registerNewElementsFactory(project, CamundaObjectFactory())
        val clipboard = mock<SystemClipboard>()
        doAnswer { buffer.get() }.whenever(clipboard).getData(any())
        doAnswer { true }.whenever(clipboard).isDataFlavorAvailable(any())
        doAnswer {
            buffer.set(
                it.getArgument(0, CopyPasteActionHandler.ClipboardFlavor::class.java).getTransferData(DATA_FLAVOR) as String)
        }.whenever(clipboard).setContents(any(), anyOrNull())

        setCopyPasteActionHandler(project, CopyPasteActionHandler(clipboard))
        popupMenuProvider = spy(CamundaCanvasPopupMenuProvider(project))
        registerPopupMenuProvider(project, popupMenuProvider)
    }
}