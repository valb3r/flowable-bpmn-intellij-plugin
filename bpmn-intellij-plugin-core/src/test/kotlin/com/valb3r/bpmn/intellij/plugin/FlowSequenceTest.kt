package com.valb3r.bpmn.intellij.plugin

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.events.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.properties.propertiesVisualizer
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.JCheckBox
import javax.swing.table.DefaultTableModel

internal class FlowSequenceTest: BaseUiTest() {

    @BeforeEach
    fun prepare() {
        registerNewElementsFactory(project, FlowableObjectFactory())
    }

    @Test
    fun `No Default flow selection on flow sequence element without gateway`() {
        prepareOneSubProcessWithServiceTaskAndAttachedBoundaryEventOneNestedSubprocessAndServiceTaskWithSequence()
        clickOnId(sequenceFlowDiagramId)
        val propertiesVisible = currentVisibleProperties()
        propertiesVisible.shouldNotContain(PropertyType.DEFAULT_FLOW_ON_SEQUENCE.caption)
    }

    @Test
    fun `Default flow selection present on flow sequence element with gateway and is updateable`() {
        prepareExclusiveGatewayAttachedToServiceTaskWithFlowSequence()
        clickOnId(sequenceFlowDiagramId)

        val propertiesVisible = currentVisibleProperties()
        propertiesVisible.shouldContain(PropertyType.DEFAULT_FLOW_ON_SEQUENCE.caption)

        whenever(boolFieldsConstructed[Pair(sequenceFlowBpmnId, PropertyType.DEFAULT_FLOW_ON_SEQUENCE)]!!.isSelected).thenReturn(true)
        propertiesVisualizer(project).clear()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldHaveSize(2)
            val xmlOnlyUpdate = firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            val uiOnlyUpdate = firstValue.filterIsInstance<BooleanValueUpdatedEvent>().shouldHaveSingleItem()
            xmlOnlyUpdate.bpmnElementId.shouldBeEqualTo(exclusiveGatewayBpmnId)
            xmlOnlyUpdate.property.shouldBeEqualTo(PropertyType.DEFAULT_FLOW)
            uiOnlyUpdate.bpmnElementId.shouldBeEqualTo(sequenceFlowBpmnId)
            uiOnlyUpdate.property.shouldBeEqualTo(PropertyType.DEFAULT_FLOW_ON_SEQUENCE)
        }
    }

    @Test
    fun `Default flow selection present on flow sequence element with gateway when sequence was created in UI and is updateable`() {
        prepareExclusiveGatewayAndServiceTaskDetached()
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce(exclusiveGatewayDiagramId)
        clickOnId(addedEdge.edge.id)

        val propertiesVisible = currentVisibleProperties()
        propertiesVisible.shouldContain(PropertyType.DEFAULT_FLOW_ON_SEQUENCE.caption)

        whenever(boolFieldsConstructed[Pair(addedEdge.edge.bpmnElement, PropertyType.DEFAULT_FLOW_ON_SEQUENCE)]!!.isSelected).thenReturn(true)
        propertiesVisualizer(project).clear()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(5)
            val xmlOnlyUpdate = lastValue.filterIsInstance<StringValueUpdatedEvent>().filter { it.property == PropertyType.DEFAULT_FLOW }.shouldHaveSingleItem()
            val uiOnlyUpdate = lastValue.filterIsInstance<BooleanValueUpdatedEvent>().filter { it.property == PropertyType.DEFAULT_FLOW_ON_SEQUENCE }.shouldHaveSingleItem()
            xmlOnlyUpdate.bpmnElementId.shouldBeEqualTo(exclusiveGatewayBpmnId)
            uiOnlyUpdate.bpmnElementId.shouldBeEqualTo(addedEdge.edge.bpmnElement)
        }
    }
}