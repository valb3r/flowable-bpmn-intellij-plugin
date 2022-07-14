package com.valb3r.bpmn.intellij.plugin.flowable.ui.components.popupmenu

import com.nhaarman.mockitokotlin2.*
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.CopyPasteActionHandler
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.DATA_FLAVOR
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.SystemClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.setCopyPasteActionHandler
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.registerPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.atomic.AtomicReference

abstract class FlowableBaseUiTest: BaseUiTest() {

    protected val buffer: AtomicReference<String> = AtomicReference()

    @BeforeEach
    fun init() {
        registerNewElementsFactory(project, FlowableObjectFactory())
        val clipboard = mock<SystemClipboard>()
        doAnswer { buffer.get() }.whenever(clipboard).getData(any())
        doAnswer { true }.whenever(clipboard).isDataFlavorAvailable(any())
        doAnswer {
            buffer.set(
                it.getArgument(0, CopyPasteActionHandler.ClipboardFlavor::class.java).getTransferData(DATA_FLAVOR) as String)
        }.whenever(clipboard).setContents(any(), anyOrNull())

        setCopyPasteActionHandler(project, CopyPasteActionHandler(clipboard))
        popupMenuProvider = spy(FlowableCanvasPopupMenuProvider(project))
        registerPopupMenuProvider(project, popupMenuProvider)
    }
}