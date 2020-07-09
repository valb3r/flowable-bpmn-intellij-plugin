package com.valb3r.bpmn.intellij.plugin

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.copypaste.copyPasteActionHandler
import com.valb3r.bpmn.intellij.plugin.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.events.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.events.DiagramElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.ui.components.popupmenu.CanvasPopupMenuProvider
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.datatransfer.Clipboard
import java.awt.geom.Point2D

internal class UiCopyPasteTest: BaseUiTest() {

    private val clipboard = Clipboard("1234")

    @BeforeEach
    fun init() {
        copyPasteActionHandler(clipboard)
    }

    @Test
    fun `Flat service task can be cut and pasted`() {
        prepareTwoServiceTaskView()
        clickOnId(serviceTaskStartDiagramId)

        CanvasPopupMenuProvider.ClipboardCutter().actionPerformed(null)
        verifyServiceTaskWasCut()

        updateEventsRegistry().reset("")
        CanvasPopupMenuProvider.ClipboardPaster(Point2D.Float(0.0f, 0.0f), parentProcessBpmnId).actionPerformed(null)
        verifyServiceTaskWasPasted()
    }

    @Test
    fun `Flat service task can be copied and pasted`() {
    }

    @Test
    fun `Flat service task can be cut and after cut canvas has no selection ids`() {
    }

    @Test
    fun `Flat service task can be copied and pasted and translated to new location after paste`() {
    }

    @Test
    fun `Service task with attached boundary event can be cut and pasted`() {
    }

    @Test
    fun `Service task with attached boundary event can be copied and pasted`() {
    }

    @Test
    fun `Service task with attached boundary event can be copied and pasted and translated to new location after paste`() {
    }

    @Test
    fun `Edge can be cut and pasted`() {
    }

    @Test
    fun `Edge can be copied and pasted`() {
    }

    @Test
    fun `Subprocess with children can be cut and pasted`() {
    }

    @Test
    fun `Subprocess with children can be copied and pasted`() {
    }

    @Test
    fun `Subprocess with children can be copied and pasted and translated to new location after paste`() {
    }

    private fun verifyServiceTaskWasPasted() {
        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            val shapeBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().shouldHaveSingleItem()
            lastValue.shouldHaveSingleItem()

            shapeBpmn.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            shapeBpmn.bpmnObject.id.shouldNotBe(serviceTaskStartBpmnId)
            shapeBpmn.bpmnObject.element.shouldBeInstanceOf(BpmnServiceTask::class)
            shapeBpmn.shape.id.shouldNotBe(serviceTaskStartDiagramId)
            shapeBpmn.shape.bpmnElement.shouldBeEqualTo(shapeBpmn.bpmnObject.id)
            shapeBpmn.shape.rectBounds().x.shouldBeEqualTo(0.0f)
            shapeBpmn.shape.rectBounds().y.shouldBeEqualTo(0.0f)
            shapeBpmn.shape.rectBounds().width.shouldBeEqualTo(serviceTaskSize)
            shapeBpmn.shape.rectBounds().height.shouldBeEqualTo(serviceTaskSize)
        }
    }

    private fun verifyServiceTaskWasCut() {
        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                    BpmnElementRemovedEvent(serviceTaskStartBpmnId))
            )
        }
    }
}