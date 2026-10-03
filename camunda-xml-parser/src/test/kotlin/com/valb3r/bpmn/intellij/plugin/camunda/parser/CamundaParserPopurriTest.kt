package com.valb3r.bpmn.intellij.plugin.camunda.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.*


internal class CamundaParserPopurriTest {
    
    @Test
    fun `XML process with all Camunda elements is parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = CamundaParser().parse("popurri.bpmn".asResource()!!)

        processObject.shouldNotBeNull()
    }

    @Test
    fun `call activity multi-instance properties are parsed`() {
        val processObject = CamundaParser().parse("popurri.bpmn".asResource()!!)
        val callActivity = processObject.process.body!!.callActivity!!.single { it.id.id == "callActiviti" }
        val loop = callActivity.multiInstanceLoopCharacteristics!!

        loop.isSequential shouldBeEqualTo false
        loop.collection shouldBeEqualTo "COLLECTION"
        loop.elementVariable shouldBeEqualTo "ELEMENT"
        loop.loopCardinality shouldBeEqualTo "12"
        loop.completionCondition shouldBeEqualTo "\${completion}"

        val properties = processObject.propsOf("callActiviti")
        properties[PropertyType.MULTI_INSTANCE_IS_SEQUENTIAL] shouldBeEqualTo Property(false)
        properties[PropertyType.MULTI_INSTANCE_COLLECTION] shouldBeEqualTo Property("COLLECTION")
        properties[PropertyType.MULTI_INSTANCE_ELEMENT_VARIABLE] shouldBeEqualTo Property("ELEMENT")
        properties[PropertyType.MULTI_INSTANCE_LOOP_CARDINALITY] shouldBeEqualTo Property("12")
        properties[PropertyType.MULTI_INSTANCE_COMPLETION_CONDITION] shouldBeEqualTo Property("\${completion}")
    }
}
