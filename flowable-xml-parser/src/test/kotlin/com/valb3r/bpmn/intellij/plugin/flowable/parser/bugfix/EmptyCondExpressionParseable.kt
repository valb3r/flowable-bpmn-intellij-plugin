package com.valb3r.bpmn.intellij.plugin.flowable.parser.bugfix

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ConditionExpression
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

private const val FILE = "bugfix/fbp-267-empty-cond-expr.bpmn20.xml"

internal class EmptyCondExpressionParseable {

    private val parser = FlowableParser()
    private val sequenceFlowElem = BpmnElementId("sequenceFlow")

    @Test
    fun `Sequence flow with empty conditional flow element parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val sequenceFlow = processObject.process.body!!.sequenceFlow!![0]
        sequenceFlow.id.shouldBeEqualTo(sequenceFlowElem)
        sequenceFlow.conditionExpression.shouldBeEqualTo(ConditionExpression(null, null))

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[sequenceFlow.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(sequenceFlowElem.id)
        props[PropertyType.CONDITION_EXPR_TYPE]!!.value.shouldBeNull()
        props[PropertyType.CONDITION_EXPR_VALUE]!!.value.shouldBeNull()
    }
}