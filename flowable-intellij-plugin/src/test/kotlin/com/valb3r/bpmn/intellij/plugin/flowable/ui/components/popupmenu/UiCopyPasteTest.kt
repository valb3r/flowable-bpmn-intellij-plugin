package com.valb3r.bpmn.intellij.plugin.flowable.ui.components.popupmenu

import com.nhaarman.mockitokotlin2.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.BpmnBoundaryErrorEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.*
import com.valb3r.bpmn.intellij.plugin.core.events.*
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D
import java.util.concurrent.atomic.AtomicReference

internal class UiCopyPasteTest: BaseUiTest() {

    private val delta = Point2D.Float(10.0f, 10.0f)
    private val pasteStart = Point2D.Float(-1000.0f, -1000.0f)
    private val pastedElemCenter = Point2D.Float(pasteStart.x + serviceTaskSize / 2.0f, pasteStart.y + serviceTaskSize / 2.0f)
    private val end = Point2D.Float(pasteStart.x + delta.x, pasteStart.y + delta.y)

    private val buffer: AtomicReference<String> = AtomicReference()

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
    }

    @Test
    fun `Flat service task can be cut and pasted`() {
        prepareTwoServiceTaskView(bpmnServiceTaskStart.copy(isForCompensation = true), bpmnServiceTaskEnd.copy(isForCompensation = true)) // Jackson behaves weirdly for 'is' fields
        clickOnId(serviceTaskStartDiagramId)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        cutToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldBeEmpty()
        lastRenderedState(project)!!.elementsById.keys.shouldContainSame(arrayOf(parentProcessBpmnId, serviceTaskEndBpmnId))
        verifyPlainServiceTaskWasCut()

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifyPlainServiceTaskWasPasted(2)
    }

    @Test
    fun `Flat service task can be copied and pasted`() {
        prepareTwoServiceTaskView(bpmnServiceTaskStart.copy(isForCompensation = true), bpmnServiceTaskEnd.copy(isForCompensation = true)) // Jackson behaves weirdly for 'is' fields
        clickOnId(serviceTaskStartDiagramId)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        copyToClipboard(project)

        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
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

        argumentCaptor<List<EventPropagatableToXml>>().apply {
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
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        cutToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldBeEmpty()
        // cascaded-cut:
        lastRenderedState(project)!!.elementsById.keys.shouldContainSame(arrayOf(parentProcessBpmnId))
        verifyServiceTaskWithBoundaryEventWereCut()

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifyServiceTaskWithBoundaryEventTaskWerePasted()
    }

    @Test
    fun `Service task with attached boundary event can be copied and pasted`() {
        prepareServiceTaskWithAttachedBoundaryEventView()
        clickOnId(serviceTaskStartDiagramId)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        copyToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldHaveSize(1)

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifyServiceTaskWithBoundaryEventTaskWerePasted(1)
    }

    @Test
    fun `Edge can be cut and pasted`() {
        prepareTwoServiceTaskView()
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedAtLeastOnceAndSelectOne()
        clickOnId(addedEdge.edge.id)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        cutToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldBeEmpty()
        // cascaded-cut:
        lastRenderedState(project)!!.elementsById.keys.shouldContainSame(arrayOf(parentProcessBpmnId, serviceTaskStartBpmnId, serviceTaskEndBpmnId))
        verifyEdgeWasCut(addedEdge.bpmnObject.id, addedEdge.edge.id)

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifyEdgeWasPasted(addedEdge.bpmnObject.id, addedEdge.edge.id)
    }

    @Test
    fun `Edge can be copied and pasted`() {
        prepareTwoServiceTaskView()
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedAtLeastOnceAndSelectOne()
        clickOnId(addedEdge.edge.id)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        copyToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldHaveSize(1)

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifyEdgeWasPasted(addedEdge.bpmnObject.id, addedEdge.edge.id, 2)
    }

    @Test
    fun `Service tasks with linking edge can be cut and pasted`() {
        prepareTwoServiceTaskView()
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedAtLeastOnceAndSelectOne()
        val begin = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.click(begin)
        canvas.startSelectionOrDrag(begin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(endElemX + serviceTaskSize, endElemX + serviceTaskSize))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        cutToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldBeEmpty()
        // cascaded-cut:
        lastRenderedState(project)!!.elementsById.keys.shouldContainSame(arrayOf(parentProcessBpmnId))
        verifyEdgeWithServiceTasksWereCut(addedEdge.bpmnObject.id, addedEdge.edge.id)

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifyEdgeWithServiceTasksWerePasted(addedEdge.bpmnObject.id, addedEdge.edge.id)
    }

    @Test
    fun `Service tasks with linking edge can be copied and pasted`() {
        prepareTwoServiceTaskView()
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedAtLeastOnceAndSelectOne()
        val begin = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.click(begin)
        canvas.startSelectionOrDrag(begin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(endElemX + serviceTaskSize, endElemX + serviceTaskSize))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        copyToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldHaveSize(6)

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifyEdgeWithServiceTasksWerePasted(addedEdge.bpmnObject.id, addedEdge.edge.id, 2)
    }

    @Test
    fun `Subprocess with children can be cut and pasted`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()
        clickOnId(subprocessDiagramId)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        cutToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldBeEmpty()
        // cascaded-cut:
        lastRenderedState(project)!!.elementsById.keys.shouldContainSame(arrayOf(parentProcessBpmnId))
        verifySubprocessWithServiceTaskAndBoundaryEventOnItWasCut()

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifySubprocessWithServiceTaskAndBoundaryEventOnItWasPasted()
    }

    @Test
    fun `Subprocess with children can be copied and pasted`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()
        clickOnId(subprocessDiagramId)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        copyToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldHaveSize(1)

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifySubprocessWithServiceTaskAndBoundaryEventOnItWasPasted(1)
    }


    @Test
    fun `Boundary event can be cut out from service task in subprocess and pasted to parent`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()
        clickOnId(optionalBoundaryErrorEventDiagramId)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        cutToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldBeEmpty()
        // cascaded-cut:
        lastRenderedState(project)!!.elementsById.keys.shouldContainSame(arrayOf(parentProcessBpmnId, subprocessBpmnId, serviceTaskStartBpmnId))
        verifyBoundaryEventWasCut()

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifyBoundaryEventWasPasted()
    }

    @Test
    fun `Boundary event can be copied from service task in subprocess and pasted to parent`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()
        clickOnId(optionalBoundaryErrorEventDiagramId)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeTrue()

        copyToClipboard(project)
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.ctx.selectedIds.shouldHaveSize(1)

        updateEventsRegistry(project).reset("")
        pasteFromClipboard(project, pasteStart, parentProcessBpmnId)
        canvas.paintComponent(graphics) // forcefully paint for state
        verifyBoundaryEventWasPasted(1)
    }

    @Test
    fun `Non-copiable selected elements detection works`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()
        clickOnId(serviceTaskStartDiagramId)
        val newLink = findExactlyOneNewLinkElem().shouldNotBeNull()
        clickOnId(newLink)

        lastRenderedState(project)!!.state.ctx.selectedIds.shouldHaveSize(1)
        lastRenderedState(project)!!.canCopyOrCut().shouldBeFalse()
    }


    private fun verifyPlainServiceTaskWasPasted(commitTimes: Int) {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(commitTimes)).executeCommitAndGetHash(any(), capture(), any(), any())
            val shapeBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().shouldHaveSingleItem()
            lastValue.shouldHaveSingleItem()

            shapeBpmn.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            shapeBpmn.bpmnObject.parentIdForXml.shouldBe(parentProcessBpmnId)
            shapeBpmn.bpmnObject.id.shouldNotBe(serviceTaskStartBpmnId)
            shapeBpmn.bpmnObject.element.shouldBeInstanceOf(BpmnServiceTask::class)
            shapeBpmn.shape.id.shouldNotBe(serviceTaskStartDiagramId)
            shapeBpmn.shape.bpmnElement.shouldBeEqualTo(shapeBpmn.bpmnObject.id)
            shapeBpmn.shape.rectBounds().x.shouldBeEqualTo(pasteStart.x)
            shapeBpmn.shape.rectBounds().y.shouldBeEqualTo(pasteStart.y)
            shapeBpmn.shape.rectBounds().width.shouldBeEqualTo(serviceTaskSize)
            shapeBpmn.shape.rectBounds().height.shouldBeEqualTo(serviceTaskSize)
            shapeBpmn.props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(true)

            lastRenderedState(project)!!.state.ctx.selectedIds.shouldContainSame(listOf(shapeBpmn.shape.id))
        }
    }

    private fun verifyPlainServiceTaskWasCut() {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                    BpmnElementRemovedEvent(serviceTaskStartBpmnId))
            )
        }
    }

    private fun verifyServiceTaskWithBoundaryEventTaskWerePasted(commitTimes: Int = 2) {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(commitTimes)).executeCommitAndGetHash(any(), capture(), any(), any())
            val serviceTaskBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().filter { it.bpmnObject.element is BpmnServiceTask}.shouldHaveSingleItem()
            val boundaryEventBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().filter { it.bpmnObject.element is BpmnBoundaryErrorEvent }.shouldHaveSingleItem()
            lastValue.shouldHaveSize(2)

            serviceTaskBpmn.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            serviceTaskBpmn.bpmnObject.parentIdForXml.shouldBe(parentProcessBpmnId)
            serviceTaskBpmn.bpmnObject.id.shouldNotBe(serviceTaskStartBpmnId)
            serviceTaskBpmn.shape.id.shouldNotBe(serviceTaskStartDiagramId)
            serviceTaskBpmn.shape.bpmnElement.shouldBeEqualTo(serviceTaskBpmn.bpmnObject.id)
            serviceTaskBpmn.shape.rectBounds().x.shouldBeEqualTo(pasteStart.x)
            serviceTaskBpmn.shape.rectBounds().y.shouldBeEqualTo(pasteStart.y)
            serviceTaskBpmn.shape.rectBounds().width.shouldBeEqualTo(serviceTaskSize)
            serviceTaskBpmn.shape.rectBounds().height.shouldBeEqualTo(serviceTaskSize)

            boundaryEventBpmn.bpmnObject.parent.shouldBe(serviceTaskBpmn.bpmnObject.id)
            boundaryEventBpmn.bpmnObject.parentIdForXml.shouldBe(parentProcessBpmnId)
            boundaryEventBpmn.bpmnObject.id.shouldNotBe(optionalBoundaryErrorEventBpmnId)
            boundaryEventBpmn.shape.id.shouldNotBe(optionalBoundaryErrorEventDiagramId)
            boundaryEventBpmn.shape.bpmnElement.shouldBeEqualTo(boundaryEventBpmn.bpmnObject.id)
            boundaryEventBpmn.shape.rectBounds().x.shouldBeGreaterThan(pasteStart.x)
            boundaryEventBpmn.shape.rectBounds().y.shouldBeGreaterThan(pasteStart.y)
            boundaryEventBpmn.shape.rectBounds().x.shouldBeLessThan(pasteStart.x + serviceTaskSize)
            boundaryEventBpmn.shape.rectBounds().y.shouldBeLessThan(pasteStart.y + serviceTaskSize)
            boundaryEventBpmn.shape.rectBounds().width.shouldBeEqualTo(boundaryEventSize)
            boundaryEventBpmn.shape.rectBounds().height.shouldBeEqualTo(boundaryEventSize)

            lastRenderedState(project)!!.state.ctx.selectedIds.shouldContainSame(listOf(serviceTaskBpmn.shape.id))
        }
    }

    private fun verifyServiceTaskWithBoundaryEventWereCut() {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(optionalBoundaryErrorEventDiagramId),
                    DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                    BpmnElementRemovedEvent(optionalBoundaryErrorEventBpmnId),
                    BpmnElementRemovedEvent(serviceTaskStartBpmnId)
            ))
        }
    }

    private fun verifyEdgeWasPasted(sourceBpmnElement: BpmnElementId, sourceDiagramElement: DiagramElementId, commitTimes: Int = 3) {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(commitTimes)).executeCommitAndGetHash(any(), capture(), any(), any())
            val edge = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            lastValue.shouldHaveSize(1)

            edge.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            edge.bpmnObject.parentIdForXml.shouldBe(parentProcessBpmnId)
            edge.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edge.bpmnObject.id.shouldNotBe(sourceBpmnElement)
            edge.edge.id.shouldNotBe(sourceDiagramElement)
            edge.edge.waypoint.shouldHaveSize(3)
            edge.edge.waypoint.filter { it.physical }.shouldHaveSize(2)
            edge.props[PropertyType.TARGET_REF]!!.value.shouldBeEqualTo("")
            edge.props[PropertyType.SOURCE_REF]!!.value.shouldBeEqualTo("")

            lastRenderedState(project)!!.state.ctx.selectedIds.shouldContainSame(listOf(edge.edge.id) + edge.edge.waypoint.map { it.id })
        }
    }

    private fun verifyEdgeWasCut(bpmnElement: BpmnElementId, diagramElement: DiagramElementId) {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.subList(1, lastValue.size).shouldContainSame(
                    listOf(
                            StringValueUpdatedEvent(serviceTaskStartBpmnId, property = PropertyType.BPMN_OUTGOING, bpmnElement.id, propertyIndex = listOf(bpmnElement.id)),
                            StringValueUpdatedEvent(serviceTaskEndBpmnId, property = PropertyType.BPMN_INCOMING, bpmnElement.id, propertyIndex = listOf(bpmnElement.id)),
                            DiagramElementRemovedEvent(diagramElement),
                            BpmnElementRemovedEvent(bpmnElement)
                    )
            )
        }
    }

    private fun verifyEdgeWithServiceTasksWerePasted(sourceBpmnElement: BpmnElementId, sourceDiagramElement: DiagramElementId, commitTimes: Int = 3) {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(commitTimes)).executeCommitAndGetHash(any(), capture(), any(), any())
            val edge = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val startTask = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>()
                    .filter { it.shape.rectBounds().x == pasteStart.x && it.shape.rectBounds().y == pasteStart.y }
                    .shouldHaveSingleItem()
            val endTask = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().filter { it.bpmnObject.id != startTask.bpmnObject.id }.shouldHaveSingleItem()
            lastValue.shouldContainSame(arrayOf(edge, startTask, endTask))

            startTask.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            startTask.bpmnObject.parentIdForXml.shouldBe(parentProcessBpmnId)
            startTask.bpmnObject.id.shouldNotBe(serviceTaskStartBpmnId)
            startTask.shape.id.shouldNotBe(serviceTaskStartDiagramId)
            startTask.shape.bpmnElement.shouldBeEqualTo(startTask.bpmnObject.id)
            startTask.shape.rectBounds().x.shouldBeEqualTo(pasteStart.x)
            startTask.shape.rectBounds().y.shouldBeEqualTo(pasteStart.y)
            startTask.shape.rectBounds().width.shouldBeEqualTo(serviceTaskSize)
            startTask.shape.rectBounds().height.shouldBeEqualTo(serviceTaskSize)

            endTask.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            endTask.bpmnObject.parentIdForXml.shouldBe(parentProcessBpmnId)
            endTask.bpmnObject.id.shouldNotBe(serviceTaskStartBpmnId)
            endTask.shape.id.shouldNotBe(serviceTaskStartDiagramId)
            endTask.shape.bpmnElement.shouldBeEqualTo(endTask.bpmnObject.id)
            endTask.shape.rectBounds().x.shouldBeEqualTo(pasteStart.x + endElemX)
            endTask.shape.rectBounds().y.shouldBeEqualTo(pasteStart.y + endElemY)
            endTask.shape.rectBounds().width.shouldBeEqualTo(serviceTaskSize)
            endTask.shape.rectBounds().height.shouldBeEqualTo(serviceTaskSize)

            edge.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            edge.bpmnObject.parentIdForXml.shouldBe(parentProcessBpmnId)
            edge.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edge.bpmnObject.id.shouldNotBe(sourceBpmnElement)
            edge.edge.id.shouldNotBe(sourceDiagramElement)
            edge.edge.waypoint.shouldHaveSize(3)
            edge.edge.waypoint.filter { it.physical }.shouldHaveSize(2)
            edge.props[PropertyType.SOURCE_REF]!!.value.shouldBeEqualTo(startTask.bpmnObject.id.id)
            edge.props[PropertyType.TARGET_REF]!!.value.shouldBeEqualTo(endTask.bpmnObject.id.id)

            lastRenderedState(project)!!.state.ctx.selectedIds.shouldContainSame(listOf(edge.edge.id) + edge.edge.waypoint.map { it.id } + startTask.shape.id + endTask.shape.id)
        }
    }

    private fun verifyEdgeWithServiceTasksWereCut(bpmnElement: BpmnElementId, diagramElement: DiagramElementId) {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.subList(1, lastValue.size).shouldContainSame(
                    listOf(
                            StringValueUpdatedEvent(serviceTaskStartBpmnId, property = PropertyType.BPMN_OUTGOING, bpmnElement.id, propertyIndex = listOf(bpmnElement.id)),
                            StringValueUpdatedEvent(serviceTaskEndBpmnId, property = PropertyType.BPMN_INCOMING, bpmnElement.id, propertyIndex = listOf(bpmnElement.id)),
                            DiagramElementRemovedEvent(diagramElement),
                            DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                            DiagramElementRemovedEvent(serviceTaskEndDiagramId),
                            BpmnElementRemovedEvent(bpmnElement),
                            BpmnElementRemovedEvent(serviceTaskStartBpmnId),
                            BpmnElementRemovedEvent(serviceTaskEndBpmnId)
                    )
            )
        }
    }

    private fun verifySubprocessWithServiceTaskAndBoundaryEventOnItWasPasted(commitTimes: Int = 2) {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(commitTimes)).executeCommitAndGetHash(any(), capture(), any(), any())
            val subprocessBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().filter { it.bpmnObject.element is BpmnSubProcess }.shouldHaveSingleItem()
            val serviceTaskBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().filter { it.bpmnObject.element is BpmnServiceTask}.shouldHaveSingleItem()
            val boundaryEventBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().filter { it.bpmnObject.element is BpmnBoundaryErrorEvent }.shouldHaveSingleItem()
            lastValue.shouldHaveSize(3)

            subprocessBpmn.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            subprocessBpmn.bpmnObject.parentIdForXml.shouldBe(parentProcessBpmnId)
            subprocessBpmn.bpmnObject.id.shouldNotBe(subprocessBpmnId)
            subprocessBpmn.shape.id.shouldNotBe(subprocessDiagramId)
            subprocessBpmn.shape.bpmnElement.shouldBeEqualTo(subprocessBpmn.bpmnObject.id)

            serviceTaskBpmn.bpmnObject.parent.shouldBe(subprocessBpmn.bpmnObject.id)
            serviceTaskBpmn.bpmnObject.parentIdForXml.shouldBe(subprocessBpmn.bpmnObject.id)
            serviceTaskBpmn.bpmnObject.id.shouldNotBe(serviceTaskStartBpmnId)
            serviceTaskBpmn.shape.id.shouldNotBe(serviceTaskStartDiagramId)
            serviceTaskBpmn.shape.bpmnElement.shouldBeEqualTo(serviceTaskBpmn.bpmnObject.id)
            serviceTaskBpmn.shape.rectBounds().x.shouldBeEqualTo(pasteStart.x)
            serviceTaskBpmn.shape.rectBounds().y.shouldBeEqualTo(pasteStart.y)
            serviceTaskBpmn.shape.rectBounds().width.shouldBeEqualTo(serviceTaskSize)
            serviceTaskBpmn.shape.rectBounds().height.shouldBeEqualTo(serviceTaskSize)

            boundaryEventBpmn.bpmnObject.parent.shouldBe(serviceTaskBpmn.bpmnObject.id)
            boundaryEventBpmn.bpmnObject.parentIdForXml.shouldBe(subprocessBpmn.bpmnObject.id)
            boundaryEventBpmn.bpmnObject.id.shouldNotBe(optionalBoundaryErrorEventBpmnId)
            boundaryEventBpmn.shape.id.shouldNotBe(optionalBoundaryErrorEventDiagramId)
            boundaryEventBpmn.shape.bpmnElement.shouldBeEqualTo(boundaryEventBpmn.bpmnObject.id)
            boundaryEventBpmn.shape.rectBounds().x.shouldBeGreaterThan(pasteStart.x)
            boundaryEventBpmn.shape.rectBounds().y.shouldBeGreaterThan(pasteStart.y)
            boundaryEventBpmn.shape.rectBounds().x.shouldBeLessThan(pasteStart.x + serviceTaskSize)
            boundaryEventBpmn.shape.rectBounds().y.shouldBeLessThan(pasteStart.y + serviceTaskSize)
            boundaryEventBpmn.shape.rectBounds().width.shouldBeEqualTo(boundaryEventSize)
            boundaryEventBpmn.shape.rectBounds().height.shouldBeEqualTo(boundaryEventSize)

            lastRenderedState(project)!!.state.ctx.selectedIds.shouldContainSame(listOf(subprocessBpmn.shape.id))
        }
    }

    private fun verifySubprocessWithServiceTaskAndBoundaryEventOnItWasCut() {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            // TODO: These all are nested in subprocess, so probably simply removing subprocess would be enough
            lastValue.shouldContainSame(
                    listOf(
                            DiagramElementRemovedEvent(subprocessDiagramId),
                            DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                            DiagramElementRemovedEvent(optionalBoundaryErrorEventDiagramId),
                            BpmnElementRemovedEvent(subprocessBpmnId),
                            BpmnElementRemovedEvent(serviceTaskStartBpmnId),
                            BpmnElementRemovedEvent(optionalBoundaryErrorEventBpmnId)
                    )
            )
        }
    }

    private fun verifyBoundaryEventWasPasted(commitTimes: Int = 2) {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(commitTimes)).executeCommitAndGetHash(any(), capture(), any(), any())
            val boundaryEventBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().filter { it.bpmnObject.element is BpmnBoundaryErrorEvent }.shouldHaveSingleItem()
            lastValue.shouldHaveSize(1)

            boundaryEventBpmn.bpmnObject.parent.shouldBe(parentProcessBpmnId)
            boundaryEventBpmn.bpmnObject.parentIdForXml.shouldBe(parentProcessBpmnId)
            boundaryEventBpmn.bpmnObject.id.shouldNotBe(optionalBoundaryErrorEventBpmnId)
            boundaryEventBpmn.shape.id.shouldNotBe(optionalBoundaryErrorEventDiagramId)
            boundaryEventBpmn.shape.bpmnElement.shouldBeEqualTo(boundaryEventBpmn.bpmnObject.id)
            boundaryEventBpmn.shape.rectBounds().width.shouldBeEqualTo(boundaryEventSize)
            boundaryEventBpmn.shape.rectBounds().height.shouldBeEqualTo(boundaryEventSize)

            lastRenderedState(project)!!.state.ctx.selectedIds.shouldContainSame(listOf(boundaryEventBpmn.shape.id))
        }
    }

    private fun verifyBoundaryEventWasCut() {
        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldContainSame(
                    listOf(
                            DiagramElementRemovedEvent(optionalBoundaryErrorEventDiagramId),
                            BpmnElementRemovedEvent(optionalBoundaryErrorEventBpmnId)
                    )
            )
        }
    }
}
