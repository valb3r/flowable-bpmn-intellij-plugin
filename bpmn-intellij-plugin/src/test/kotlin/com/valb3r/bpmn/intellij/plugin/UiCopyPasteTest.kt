package com.valb3r.bpmn.intellij.plugin

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.copypaste.copyPasteActionHandler
import com.valb3r.bpmn.intellij.plugin.events.*
import com.valb3r.bpmn.intellij.plugin.render.lastRenderedState
import com.valb3r.bpmn.intellij.plugin.ui.components.popupmenu.CanvasPopupMenuProvider
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.datatransfer.Clipboard
import java.awt.geom.Point2D

internal class UiCopyPasteTest: BaseUiTest() {

    private val clipboard = Clipboard("1234")
    private val delta = Point2D.Float(10.0f, 10.0f)
    private val pasteStart = Point2D.Float(-1000.0f, -1000.0f)
    private val pastedElemCenter = Point2D.Float(pasteStart.x + serviceTaskSize / 2.0f, pasteStart.y + serviceTaskSize / 2.0f)
    private val end = Point2D.Float(pasteStart.x + delta.x, pasteStart.y + delta.y)

    @BeforeEach
    fun init() {
        copyPasteActionHandler(clipboard)
    }

    @Test
    fun `Flat service task can be cut and pasted`() {
        prepareTwoServiceTaskView()
        clickOnId(serviceTaskStartDiagramId)

        CanvasPopupMenuProvider.ClipboardCutter().actionPerformed(null)
        canvas.paintComponent(graphics)
        lastRenderedState()!!.state.ctx.selectedIds.shouldBeEmpty()
        lastRenderedState()!!.elementsById.keys.shouldContainSame(arrayOf(parentProcessBpmnId, serviceTaskEndBpmnId))
        verifyPlainServiceTaskWasCut()

        updateEventsRegistry().reset("")
        CanvasPopupMenuProvider.ClipboardPaster(pasteStart, parentProcessBpmnId).actionPerformed(null)
        verifyPlainServiceTaskWasPasted(2)
    }

    @Test
    fun `Flat service task can be copied and pasted`() {
        prepareTwoServiceTaskView()
        clickOnId(serviceTaskStartDiagramId)

        CanvasPopupMenuProvider.ClipboardCopier().actionPerformed(null)

        CanvasPopupMenuProvider.ClipboardPaster(pasteStart, parentProcessBpmnId).actionPerformed(null)
        canvas.paintComponent(graphics)
        verifyPlainServiceTaskWasPasted(1)
    }

    @Test
    fun `Flat service task can be copied and pasted and translated to new location after paste`() {
        `Flat service task can be copied and pasted`()

        canvas.click(pastedElemCenter)
        canvas.startSelectionOrDrag(pastedElemCenter)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(pasteStart, end)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            val shapeBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().shouldHaveSingleItem()
            val translateBpmn = lastValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(shapeBpmn, translateBpmn))

            translateBpmn.diagramElementId.shouldBeEqualTo(shapeBpmn.shape.id)
        }
    }

    @Test
    fun `Service task with attached boundary event can be cut and pasted`() {
        prepareServiceTaskWithAttachedBoundaryEventView()
        clickOnId(serviceTaskStartDiagramId)

        CanvasPopupMenuProvider.ClipboardCutter().actionPerformed(null)
        canvas.paintComponent(graphics)
        lastRenderedState()!!.state.ctx.selectedIds.shouldBeEmpty()
        // cascaded-cut:
        lastRenderedState()!!.elementsById.keys.shouldContainSame(arrayOf(parentProcessBpmnId))
        verifyServiceTaskWithBoundaryEventWereCut()

        updateEventsRegistry().reset("")
        CanvasPopupMenuProvider.ClipboardPaster(pasteStart, parentProcessBpmnId).actionPerformed(null)
        verifyServiceTaskWithBoundaryEventTaskWerePasted(2)
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

    private fun verifyPlainServiceTaskWasPasted(commitTimes: Int) {
        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter, times(commitTimes)).executeCommitAndGetHash(any(), capture(), any(), any())
            val shapeBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().shouldHaveSingleItem()
            lastValue.shouldHaveSingleItem()

            shapeBpmn.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            shapeBpmn.bpmnObject.id.shouldNotBe(serviceTaskStartBpmnId)
            shapeBpmn.bpmnObject.element.shouldBeInstanceOf(BpmnServiceTask::class)
            shapeBpmn.shape.id.shouldNotBe(serviceTaskStartDiagramId)
            shapeBpmn.shape.bpmnElement.shouldBeEqualTo(shapeBpmn.bpmnObject.id)
            shapeBpmn.shape.rectBounds().x.shouldBeEqualTo(pasteStart.x)
            shapeBpmn.shape.rectBounds().y.shouldBeEqualTo(pasteStart.y)
            shapeBpmn.shape.rectBounds().width.shouldBeEqualTo(serviceTaskSize)
            shapeBpmn.shape.rectBounds().height.shouldBeEqualTo(serviceTaskSize)
        }
    }

    private fun verifyPlainServiceTaskWasCut() {
        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                    BpmnElementRemovedEvent(serviceTaskStartBpmnId))
            )
        }
    }

    private fun verifyServiceTaskWithBoundaryEventTaskWerePasted(commitTimes: Int) {
        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter, times(commitTimes)).executeCommitAndGetHash(any(), capture(), any(), any())
            val shapeBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().shouldHaveSingleItem()
            lastValue.shouldHaveSingleItem()

            shapeBpmn.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            shapeBpmn.bpmnObject.id.shouldNotBe(serviceTaskStartBpmnId)
            shapeBpmn.bpmnObject.element.shouldBeInstanceOf(BpmnServiceTask::class)
            shapeBpmn.shape.id.shouldNotBe(serviceTaskStartDiagramId)
            shapeBpmn.shape.bpmnElement.shouldBeEqualTo(shapeBpmn.bpmnObject.id)
            shapeBpmn.shape.rectBounds().x.shouldBeEqualTo(pasteStart.x)
            shapeBpmn.shape.rectBounds().y.shouldBeEqualTo(pasteStart.y)
            shapeBpmn.shape.rectBounds().width.shouldBeEqualTo(serviceTaskSize)
            shapeBpmn.shape.rectBounds().height.shouldBeEqualTo(serviceTaskSize)
        }
    }

    private fun verifyServiceTaskWithBoundaryEventWereCut() {
        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                    BpmnElementRemovedEvent(serviceTaskStartBpmnId)
            ))
        }
    }
}