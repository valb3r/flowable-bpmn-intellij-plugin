package com.valb3r.bpmn.intellij.plugin

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.BpmnParentChangedEvent
import com.valb3r.bpmn.intellij.plugin.events.DraggedToEvent
import com.valb3r.bpmn.intellij.plugin.events.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D

internal class BoundaryEventAttachTest: BaseUiTest() {

    private val onProcessDragEnd = Point2D.Float(-1000.0f, -1000.0f)

    @Test
    fun `Boundary event attaches to service task`() {
        prepareServiceTaskWithBoundaryEventOnRootView()
        val target = clickOnId(serviceTaskStartDiagramId)

        val start = clickOnId(optionalBoundaryErrorEventDiagramId)
        canvas.paintComponent(graphics)
        canvas.startSelectionOrDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, target)
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedTo = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val xmlParentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSingleItem()
            val parentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            val attachedRef = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
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
        prepareServiceTaskInSubprocesskWithBoundaryEventOnRootView()
        val target = clickOnId(serviceTaskStartDiagramId)

        val start = clickOnId(optionalBoundaryErrorEventDiagramId)
        canvas.paintComponent(graphics)
        canvas.startSelectionOrDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, target)
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedTo = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val xmlParentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSingleItem()
            val parentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            val attachedRef = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
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
    fun `Boundary event detaches from service task (plain) to subprocess`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()
        val target = clickOnId(subprocessDiagramId)

        val start = clickOnId(optionalBoundaryErrorEventDiagramId)
        canvas.paintComponent(graphics)
        canvas.startSelectionOrDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, target)
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedTo = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val xmlParentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSingleItem()
            val parentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            val attachedRef = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            firstValue.shouldHaveSize(4)

            draggedTo.diagramElementId.shouldBeEqualTo(optionalBoundaryErrorEventDiagramId)
            xmlParentChanged.bpmnElementId.shouldBeEqualTo(optionalBoundaryErrorEventBpmnId)
            xmlParentChanged.newParentId.shouldBeEqualTo(subprocessBpmnId)
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
        canvas.startSelectionOrDrag(start)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(start, onProcessDragEnd)
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            val draggedTo = firstValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val xmlParentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.propagateToXml }.shouldHaveSingleItem()
            val parentChanged = firstValue.filterIsInstance<BpmnParentChangedEvent>().filter { !it.propagateToXml }.shouldHaveSingleItem()
            val attachedRef = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
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
}