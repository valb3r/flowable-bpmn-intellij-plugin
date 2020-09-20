package com.valb3r.bpmn.intellij.plugin

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.events.DiagramElementRemovedEvent
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ElementRemovalTest: BaseUiTest() {

    @BeforeEach
    fun prepare() {
        prepareOneSubProcessWithServiceTaskAndAttachedBoundaryEventOneNestedSubprocessAndServiceTaskWithSequence()
    }

    @Test
    fun `Remove boundary event within subprocess`() {
        clickOnId(optionalBoundaryErrorEventDiagramId)
        val boundaryEventRemove = findExactlyOneDeleteElem()
        clickOnId(boundaryEventRemove!!)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(optionalBoundaryErrorEventDiagramId),
                    BpmnElementRemovedEvent(optionalBoundaryErrorEventBpmnId))
            )
        }
    }

    @Test
    fun `Remove service task with boundary event within subprocess`() {
        clickOnId(serviceTaskStartDiagramId)
        val serviceTaskRemove = findExactlyOneDeleteElem()
        clickOnId(serviceTaskRemove!!)

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

    @Test
    fun `Remove sequence element within subprocess`() {
        clickOnId(sequenceFlowDiagramId)
        val sequenceRemove = findExactlyOneDeleteElem()
        clickOnId(sequenceRemove!!)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(sequenceFlowDiagramId),
                    BpmnElementRemovedEvent(sequenceFlowBpmnId)
            ))
        }
    }

    @Test
    fun `Remove subprocess element within subprocess`() {
        clickOnId(subprocessInSubProcessDiagramId)
        val nestedSubprocessRemove = findExactlyOneDeleteElem()
        clickOnId(nestedSubprocessRemove!!)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(sequenceFlowDiagramId),
                    BpmnElementRemovedEvent(sequenceFlowBpmnId),
                    DiagramElementRemovedEvent(serviceTaskEndDiagramId),
                    BpmnElementRemovedEvent(serviceTaskEndBpmnId),
                    DiagramElementRemovedEvent(subprocessInSubProcessDiagramId),
                    BpmnElementRemovedEvent(subprocessInSubProcessBpmnId)
            ))
        }
    }

    @Test
    fun `Remove root subprocess element`() {
        clickOnId(subprocessDiagramId)
        val subprocessRemove = findExactlyOneDeleteElem()
        clickOnId(subprocessRemove!!)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(sequenceFlowDiagramId),
                    BpmnElementRemovedEvent(sequenceFlowBpmnId),
                    DiagramElementRemovedEvent(serviceTaskEndDiagramId),
                    BpmnElementRemovedEvent(serviceTaskEndBpmnId),
                    DiagramElementRemovedEvent(subprocessInSubProcessDiagramId),
                    BpmnElementRemovedEvent(subprocessInSubProcessBpmnId),
                    DiagramElementRemovedEvent(optionalBoundaryErrorEventDiagramId),
                    DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                    BpmnElementRemovedEvent(optionalBoundaryErrorEventBpmnId),
                    BpmnElementRemovedEvent(serviceTaskStartBpmnId),
                    DiagramElementRemovedEvent(subprocessDiagramId),
                    BpmnElementRemovedEvent(subprocessBpmnId)
            ))
        }
    }
}