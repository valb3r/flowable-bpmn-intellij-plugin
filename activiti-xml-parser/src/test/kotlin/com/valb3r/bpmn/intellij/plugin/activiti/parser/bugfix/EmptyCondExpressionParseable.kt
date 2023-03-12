package com.valb3r.bpmn.intellij.plugin.flowable.parser.bugfix

import com.valb3r.bpmn.intellij.plugin.activiti.parser.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ConditionExpression
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

private const val FILE = "bugfix/fbp-267-empty-cond-expr.bpmn20.xml"

internal class EmptyCondExpressionParseable {

    private val parser = ActivitiParser()
    private val sequenceFlowElem = BpmnElementId("sequenceFlow")

    @Test
    fun `Sequence flow with empty conditional flow element parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val sequenceFlow = processObject.processes[0].body!!.sequenceFlow!![0]
        sequenceFlow.id.shouldBeEqualTo(sequenceFlowElem)
        sequenceFlow.conditionExpression.shouldBeEqualTo(ConditionExpression(null, ""))

        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(ActivitiObjectFactory()).processes[0].processElemPropertiesByElementId[sequenceFlow.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(sequenceFlowElem.id)
        props[PropertyType.CONDITION_EXPR_TYPE]!!.value.shouldBeNull()
        props[PropertyType.CONDITION_EXPR_VALUE]!!.value.shouldBeEqualTo("")
    }

    @Test
    fun `Sequence flow with empty conditional flow element parseable (Activiti 7)`() {
        val processObject = Activiti7Parser().parse(FILE.asResource()!!)

        val sequenceFlow = processObject.processes[0].body!!.sequenceFlow!![0]
        sequenceFlow.id.shouldBeEqualTo(sequenceFlowElem)
        sequenceFlow.conditionExpression.shouldBeEqualTo(ConditionExpression(null, ""))

        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(Activiti7ObjectFactory()).processes[0].processElemPropertiesByElementId[sequenceFlow.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(sequenceFlowElem.id)
        props[PropertyType.CONDITION_EXPR_TYPE]!!.value.shouldBeNull()
        props[PropertyType.CONDITION_EXPR_VALUE]!!.value.shouldBeEqualTo("")
    }
}
