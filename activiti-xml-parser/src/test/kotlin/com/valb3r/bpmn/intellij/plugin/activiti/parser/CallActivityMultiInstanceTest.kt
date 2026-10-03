package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class CallActivityMultiInstanceTest {

    @Test
    fun `call activity multi-instance properties are parsed`() {
        val processObject = ActivitiParser().parse("popurri.bpmn20.xml".asResource()!!)
        val callActivity = processObject.process.body!!.callActivity!!.single { it.id.id == "callActivitiId" }
        val loop = callActivity.multiInstanceLoopCharacteristics!!

        loop.isSequential.shouldBeEqualTo(false)
        loop.collection.shouldBeEqualTo("M1")
        loop.elementVariable.shouldBeEqualTo("M1")
        loop.loopCardinality.shouldBeEqualTo("C1")
        loop.completionCondition.shouldBeEqualTo("C1")

        val properties = processObject.propsOf("callActivitiId")
        properties[PropertyType.MULTI_INSTANCE_IS_SEQUENTIAL].shouldBeEqualTo(Property(false))
        properties[PropertyType.MULTI_INSTANCE_COLLECTION].shouldBeEqualTo(Property("M1"))
        properties[PropertyType.MULTI_INSTANCE_ELEMENT_VARIABLE].shouldBeEqualTo(Property("M1"))
        properties[PropertyType.MULTI_INSTANCE_LOOP_CARDINALITY].shouldBeEqualTo(Property("C1"))
        properties[PropertyType.MULTI_INSTANCE_COMPLETION_CONDITION].shouldBeEqualTo(Property("C1"))
    }
}
