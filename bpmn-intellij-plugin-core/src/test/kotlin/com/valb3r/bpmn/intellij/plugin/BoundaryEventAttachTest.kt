package com.valb3r.bpmn.intellij.plugin

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnEdgeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnParentChangedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.DraggedToEvent
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D

internal class BoundaryEventAttachTest: BaseUiTest() {

    private val onProcessDragEnd = Point2D.Float(-1000.0f, -1000.0f)

    @BeforeEach
    fun `Prepare object factory`() {
        registerNewElementsFactory(project, FlowableObjectFactory())
    }

    @Test
    fun `Boundary event attaches to service task`() {
        prepareServiceTaskWithBoundaryEventOnRootView()
        val target = clickOnId(serviceTaskStartDiagramId)

        val start = clickOnId(optionalBoundaryErrorEventDiagramId)
        canvas.paintComponent(graphics)
        canvas.startSelectionAndDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, target)
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[optionalBoundaryErrorEventBpmnId]!!.parentIdForXml.shouldBeEqualTo(parentProcessBpmnId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedTo = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val xmlParentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSingleItem()
            val parentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            val attachedRef = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            firstValue.indexOf(xmlParentChanged).shouldBeLessThan(firstValue.indexOf(parentChanged))
            firstValue.shouldHaveSize(4)

            draggedTo.diagramElementId.shouldBeEqualTo(optionalBoundaryErrorEventDiagramId)
            xmlParentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            xmlParentChanged.newParentId.shouldBeEqualTo(parentProcessBpmnId)
            parentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            parentChanged.newParentId.shouldBeEqualTo(serviceTaskStartBpmnId)
            attachedRef.property.shouldBeEqualTo(PropertyType.ATTACHED_TO_REF)
            attachedRef.newValue.shouldBeEqualTo(serviceTaskStartBpmnId.id)
        }
    }

    @Test
    fun `Boundary event attaches to service task in subprocess`() {
        prepareServiceTaskInSubprocessWithBoundaryEventOnRootView()
        val target = clickOnId(serviceTaskStartDiagramId)

        val start = clickOnId(optionalBoundaryErrorEventDiagramId)
        canvas.paintComponent(graphics)
        canvas.startSelectionAndDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, target)
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[optionalBoundaryErrorEventBpmnId]!!.parentIdForXml.shouldBeEqualTo(subprocessBpmnId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedTo = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val xmlParentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSingleItem()
            val parentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            val attachedRef = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            firstValue.indexOf(xmlParentChanged).shouldBeLessThan(firstValue.indexOf(parentChanged))
            firstValue.shouldHaveSize(4)

            draggedTo.diagramElementId.shouldBeEqualTo(optionalBoundaryErrorEventDiagramId)
            xmlParentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            xmlParentChanged.newParentId.shouldBeEqualTo(subprocessBpmnId)
            parentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            parentChanged.newParentId.shouldBeEqualTo(serviceTaskStartBpmnId)
            attachedRef.property.shouldBeEqualTo(PropertyType.ATTACHED_TO_REF)
            attachedRef.newValue.shouldBeEqualTo(serviceTaskStartBpmnId.id)
        }
    }

    @Test
    fun `Boundary event attaches to subprocess`() {
        prepareServiceTaskInSubprocessWithBoundaryEventOnRootView()

        val start = clickOnId(optionalBoundaryErrorEventDiagramId)
        canvas.paintComponent(graphics)
        canvas.startSelectionAndDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, elementCenter(subprocessDiagramId))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[optionalBoundaryErrorEventBpmnId]!!.parentIdForXml.shouldBeEqualTo(parentProcessBpmnId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedTo = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val xmlParentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSingleItem()
            val parentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            val attachedRef = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            firstValue.indexOf(xmlParentChanged).shouldBeLessThan(firstValue.indexOf(parentChanged))
            firstValue.shouldHaveSize(4)

            draggedTo.diagramElementId.shouldBeEqualTo(optionalBoundaryErrorEventDiagramId)
            xmlParentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            xmlParentChanged.newParentId.shouldBeEqualTo(parentProcessBpmnId)
            parentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            parentChanged.newParentId.shouldBeEqualTo(subprocessBpmnId)
            attachedRef.property.shouldBeEqualTo(PropertyType.ATTACHED_TO_REF)
            attachedRef.newValue.shouldBeEqualTo(subprocessBpmnId.id)
        }
    }

    @Test
    fun `Boundary event attaches to subprocess in subprocess`() {
        prepareOneSubProcessThenNestedSubProcessWithOneServiceTaskViewWithBoundaryErrorOnRoot()

        val start = clickOnId(optionalBoundaryErrorEventDiagramId)
        canvas.paintComponent(graphics)
        canvas.startSelectionAndDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, elementCenter(subprocessInSubProcessDiagramId))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[optionalBoundaryErrorEventBpmnId]!!.parent.shouldBeEqualTo(subprocessInSubProcessBpmnId)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[optionalBoundaryErrorEventBpmnId]!!.parentIdForXml.shouldBeEqualTo(subprocessBpmnId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedTo = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val xmlParentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSingleItem()
            val parentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            val attachedRef = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            firstValue.indexOf(xmlParentChanged).shouldBeLessThan(firstValue.indexOf(parentChanged))
            firstValue.shouldHaveSize(4)

            draggedTo.diagramElementId.shouldBeEqualTo(optionalBoundaryErrorEventDiagramId)
            xmlParentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            xmlParentChanged.newParentId.shouldBeEqualTo(subprocessBpmnId)
            parentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            parentChanged.newParentId.shouldBeEqualTo(subprocessInSubProcessBpmnId)
            attachedRef.property.shouldBeEqualTo(PropertyType.ATTACHED_TO_REF)
            attachedRef.newValue.shouldBeEqualTo(subprocessInSubProcessBpmnId.id)
        }
    }

    @Test
    fun `Boundary event detaches from service task (plain) to subprocess`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()
        val target = clickOnId(subprocessDiagramId)

        val start = clickOnId(optionalBoundaryErrorEventDiagramId)
        canvas.paintComponent(graphics)
        canvas.startSelectionAndDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, target)
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[optionalBoundaryErrorEventBpmnId]!!.parent.shouldBeEqualTo(subprocessBpmnId)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[optionalBoundaryErrorEventBpmnId]!!.parentIdForXml.shouldBeEqualTo(parentProcessBpmnId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedTo = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val xmlParentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSingleItem()
            val parentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            val attachedRef = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            firstValue.indexOf(xmlParentChanged).shouldBeLessThan(firstValue.indexOf(parentChanged))
            firstValue.shouldHaveSize(4)

            draggedTo.diagramElementId.shouldBeEqualTo(optionalBoundaryErrorEventDiagramId)
            xmlParentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            xmlParentChanged.newParentId.shouldBeEqualTo(parentProcessBpmnId)
            parentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            parentChanged.newParentId.shouldBeEqualTo(subprocessBpmnId)
            attachedRef.property.shouldBeEqualTo(PropertyType.ATTACHED_TO_REF)
            attachedRef.newValue.shouldBeEqualTo(subprocessBpmnId.id)
        }
    }

    @Test
    fun `Boundary event detaches from service task (plain) to root process`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()

        val start = clickOnId(optionalBoundaryErrorEventDiagramId)
        canvas.paintComponent(graphics)
        canvas.startSelectionAndDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, onProcessDragEnd)
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[optionalBoundaryErrorEventBpmnId]!!.parentIdForXml.shouldBeEqualTo(parentProcessBpmnId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedTo = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val xmlParentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSingleItem()
            val parentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            val attachedRef = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            firstValue.indexOf(xmlParentChanged).shouldBeLessThan(firstValue.indexOf(parentChanged))
            firstValue.shouldHaveSize(4)

            draggedTo.diagramElementId.shouldBeEqualTo(optionalBoundaryErrorEventDiagramId)
            xmlParentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            xmlParentChanged.newParentId.shouldBeEqualTo(parentProcessBpmnId)
            parentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            parentChanged.newParentId.shouldBeEqualTo(parentProcessBpmnId)
            attachedRef.property.shouldBeEqualTo(PropertyType.ATTACHED_TO_REF)
            attachedRef.newValue.shouldBeEqualTo(parentProcessBpmnId.id)
        }
    }

    @Test
    fun `Boundary event follows service task when it is dragged to subprocess`() {
        prepareOneSubProcessAndOnRootServiceTaskWithAttachedBoundaryEventView()

        val start = clickOnId(serviceTaskStartDiagramId)
        canvas.paintComponent(graphics)
        canvas.startSelectionAndDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, elementCenter(subprocessDiagramId))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[serviceTaskStartBpmnId]!!.parentIdForXml.shouldBeEqualTo(subprocessBpmnId)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[serviceTaskStartBpmnId]!!.parent.shouldBeEqualTo(subprocessBpmnId)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[optionalBoundaryErrorEventBpmnId]!!.parentIdForXml.shouldBeEqualTo(subprocessBpmnId)
        lastRenderedState(project)!!.state.currentState.elementByBpmnId[optionalBoundaryErrorEventBpmnId]!!.parent.shouldBeEqualTo(serviceTaskStartBpmnId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedToServiceTask = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSize(2)[0]
            val draggedBoundaryEvent = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSize(2)[1]
            val xmlParentChangedServiceTask = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSize(2)[0]
            val xmlParentChangedBoundaryEvent = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSize(2)[1]
            val uiParentChangedBoundaryEvent = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldBeEmpty()
            firstValue.indexOf(xmlParentChangedServiceTask).shouldBeLessThan(firstValue.indexOf(uiParentChangedBoundaryEvent))
            firstValue.indexOf(xmlParentChangedBoundaryEvent).shouldBeLessThan(firstValue.indexOf(uiParentChangedBoundaryEvent))
            firstValue.shouldHaveSize(5)

            draggedToServiceTask.diagramElementId.shouldBeEqualTo(serviceTaskStartDiagramId)
            draggedBoundaryEvent.diagramElementId.shouldBeEqualTo(optionalBoundaryErrorEventDiagramId)
            xmlParentChangedServiceTask.bpmnElementId.shouldBeEqualTo(serviceTaskStartBpmnId)
            xmlParentChangedServiceTask.newParentId.shouldBeEqualTo(subprocessBpmnId)
            xmlParentChangedBoundaryEvent.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            xmlParentChangedBoundaryEvent.newParentId.shouldBeEqualTo(subprocessBpmnId)
            uiParentChangedBoundaryEvent.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId) // Compensates xml change of parent
            uiParentChangedBoundaryEvent.newParentId.shouldBeEqualTo(serviceTaskStartBpmnId)
        }
    }

    @Test
    fun `New edge element should be addable out of boundary element on plain process`() {
        prepareServiceTaskWithBoundaryEventOnRootView()

        clickOnId(optionalBoundaryErrorEventDiagramId)
        val newLink = findExactlyOneNewLinkElem().shouldNotBeNull()
        val newLinkLocation = clickOnId(newLink)
        dragToButDontStop(newLinkLocation, elementCenter(serviceTaskStartDiagramId))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val propUpdate = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSize(2).toTypedArray()
            propUpdate.map { it.property }.shouldContainAll(arrayOf(PropertyType.BPMN_INCOMING, PropertyType.BPMN_OUTGOING))
            lastValue.shouldContainSame(listOf(edgeBpmn, *propUpdate))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)

            sequence.sourceRef.shouldBe(optionalBoundaryErrorEventBpmnId.id)
            sequence.targetRef.shouldBe("")
            edgeBpmn.props[PropertyType.SOURCE_REF]!!.value.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId.id)
            edgeBpmn.props[PropertyType.TARGET_REF]!!.value.shouldBeEqualTo(serviceTaskStartBpmnId.id)
        }
    }

    @Test
    fun `New edge element should be addable out of boundary element on service task`() {
        prepareServiceTaskWithAttachedBoundaryEventView()

        clickOnId(optionalBoundaryErrorEventDiagramId)
        val newLink = findExactlyOneNewLinkElem().shouldNotBeNull()
        val newLinkLocation = clickOnId(newLink)
        dragToButDontStop(newLinkLocation, elementCenter(serviceTaskStartDiagramId))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val propUpdate = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSize(2).toTypedArray()
            propUpdate.map { it.property }.shouldContainAll(arrayOf(PropertyType.BPMN_INCOMING, PropertyType.BPMN_OUTGOING))
            lastValue.shouldContainSame(listOf(edgeBpmn, *propUpdate))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)

            sequence.sourceRef.shouldBe(optionalBoundaryErrorEventBpmnId.id)
            sequence.targetRef.shouldBe("")
            edgeBpmn.props[PropertyType.SOURCE_REF]!!.value.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId.id)
            edgeBpmn.props[PropertyType.TARGET_REF]!!.value.shouldBeEqualTo(serviceTaskStartBpmnId.id)
        }
    }
}
