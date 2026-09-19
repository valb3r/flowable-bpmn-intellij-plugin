package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
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
    }
}
