package com.valb3r.bpmn.intellij.plugin.flowable.parser.bugfix

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ConditionExpression
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

private const val FILE = "bugfix/fbp-287-cond-expr-without-type.bpmn20.xml"

internal class CondExpressionWithoutTypeParseable {

    private val parser = FlowableParser()
    private val sequenceFlowElem = BpmnElementId("sid-s3")

    @Test
    fun `Sequence flow with empty conditional flow element parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val sequenceFlow = processObject.processes[0].body!!.sequenceFlow!![2]
        sequenceFlow.id.shouldBeEqualTo(sequenceFlowElem)
        sequenceFlow.conditionExpression.shouldBeEqualTo(ConditionExpression(null, "\${evection.num<3} "))

        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(FlowableObjectFactory()).processes[0].processElemPropertiesByElementId[sequenceFlow.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(sequenceFlowElem.id)
        props[PropertyType.CONDITION_EXPR_TYPE]!!.value.shouldBeNull()
        props[PropertyType.CONDITION_EXPR_VALUE]!!.value.shouldBeEqualTo("\${evection.num<3} ")
    }
}
