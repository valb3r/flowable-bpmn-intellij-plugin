package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class MultiInstanceLoopCharacteristicsTest {

    @Test
    fun `call activity multi-instance properties are parsed and exposed`() {
        val processObject = FlowableParser().parse("popurri.bpmn20.xml".asResource()!!)
        val callActivity = processObject.process.body!!.callActivity!!
            .single { it.id.id == "callActivityId" }
        val loop = callActivity.multiInstanceLoopCharacteristics!!

        loop.isSequential.shouldBeEqualTo(true)
        loop.collection.shouldBeEqualTo("COLLECTION")
        loop.elementVariable.shouldBeEqualTo("ELEM_VAR")
        loop.loopCardinality.shouldBeEqualTo("12")
        loop.completionCondition.shouldBeEqualTo("completionCond")
        loop.elementIndexVariable.shouldBeEqualTo("ELEM_INDEX")
        loop.noWaitStatesAsyncLeave.shouldBeEqualTo(true)
        loop.loopDataInputRef.shouldBeEqualTo("INPUT_COLLECTION")
        loop.inputDataItem.shouldBeEqualTo("INPUT_ITEM")
        loop.variableAggregations!!.single().apply {
            target.shouldBeEqualTo("aggregated")
            delegateExpression.shouldBeEqualTo("${'$'}{variableAggregator}")
            createOverviewVariable.shouldBeEqualTo(true)
            storeAsTransientVariable.shouldBeEqualTo(true)
            definitions!!.single().apply {
                source.shouldBeEqualTo("sourceVar")
                target.shouldBeEqualTo("targetVar")
            }
        }

        val properties = processObject.propsOf("callActivityId")
        properties[PropertyType.MULTI_INSTANCE_IS_SEQUENTIAL]
            .shouldBeEqualTo(Property(true))
        properties[PropertyType.MULTI_INSTANCE_COLLECTION]
            .shouldBeEqualTo(Property("COLLECTION"))
        properties[PropertyType.MULTI_INSTANCE_ELEMENT_VARIABLE]
            .shouldBeEqualTo(Property("ELEM_VAR"))
        properties[PropertyType.MULTI_INSTANCE_LOOP_CARDINALITY]
            .shouldBeEqualTo(Property("12"))
        properties[PropertyType.MULTI_INSTANCE_COMPLETION_CONDITION]
            .shouldBeEqualTo(Property("completionCond"))
        properties[PropertyType.MULTI_INSTANCE_ELEMENT_INDEX_VARIABLE]
            .shouldBeEqualTo(Property("ELEM_INDEX"))
        properties[PropertyType.MULTI_INSTANCE_NO_WAIT_STATES_ASYNC_LEAVE]
            .shouldBeEqualTo(Property(true))
        properties[PropertyType.MULTI_INSTANCE_LOOP_DATA_INPUT_REF]
            .shouldBeEqualTo(Property("INPUT_COLLECTION"))
        properties[PropertyType.MULTI_INSTANCE_INPUT_DATA_ITEM]
            .shouldBeEqualTo(Property("INPUT_ITEM"))
        properties[PropertyType.VARIABLE_AGGREGATION_TARGET]
            .shouldBeEqualTo(Property("aggregated", listOf("aggregated")))
        properties[PropertyType.VARIABLE_AGGREGATION_VARIABLE_TARGET]
            .shouldBeEqualTo(Property("targetVar", listOf("aggregated", "sourceVar")))
    }

    @Test
    fun `multi-instance variable aggregation mappings are updated`() {
        val updated = readAndUpdateProcess(
            FlowableParser(),
            "popurri.bpmn20.xml",
            StringValueUpdatedEvent(
                BpmnElementId("callActivityId"),
                PropertyType.VARIABLE_AGGREGATION_VARIABLE_TARGET,
                "updatedTarget",
                propertyIndex = listOf("aggregated", "sourceVar"),
            ),
        )

        updated.propsOf("callActivityId")[PropertyType.VARIABLE_AGGREGATION_VARIABLE_TARGET]
            .shouldBeEqualTo(Property("updatedTarget", listOf("aggregated", "sourceVar")))
    }
}
