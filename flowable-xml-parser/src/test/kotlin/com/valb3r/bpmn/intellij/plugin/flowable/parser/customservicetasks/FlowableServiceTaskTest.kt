package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import com.valb3r.bpmn.intellij.plugin.flowable.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/service-task.bpmn20.xml"

internal class FlowableServiceTaskTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("serviceTaskId")

    @Test
    fun `Service task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readServiceTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Service task name")
        task.documentation.shouldBeEqualTo("Docs for service task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.delegateExpression.shouldBeEqualTo("\${do.delegateTo()}")
        task.clazz.shouldBeEqualTo("com.foo.bar")
        task.expression.shouldBeEqualTo("#{some.expr()}")
        task.resultVariableName.shouldBeEqualTo("RESULT_VAR")
        task.skipExpression.shouldBeEqualTo("\${do.skip}")
        task.triggerable!!.shouldBeTrue()
        task.useLocalScopeForResultVariable!!.shouldBeTrue()
        // TODO handle deep extension elements - field

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.DELEGATE_EXPRESSION]!!.value.shouldBeEqualTo(task.delegateExpression)
        props[PropertyType.CLASS]!!.value.shouldBeEqualTo(task.clazz)
        props[PropertyType.EXPRESSION]!!.value.shouldBeEqualTo(task.expression)
        props[PropertyType.RESULT_VARIABLE_NAME]!!.value.shouldBeEqualTo(task.resultVariableName)
        props[PropertyType.SKIP_EXPRESSION]!!.value.shouldBeEqualTo(task.skipExpression)
        props[PropertyType.IS_TRIGGERABLE]!!.value.shouldBeEqualTo(task.triggerable)
        props[PropertyType.IS_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE]!!.value.shouldBeEqualTo(task.useLocalScopeForResultVariable)
    }

    @Test
    fun `Service task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.DELEGATE_EXPRESSION, value).delegateExpression.shouldBeEqualTo(value)} ("\${>Delegate expr}");
        {value: String -> readAndUpdate(PropertyType.CLASS, value).clazz.shouldBeEqualTo(value)} ("com.foo.bar.another.class");
        {value: String -> readAndUpdate(PropertyType.EXPRESSION, value).expression.shouldBeEqualTo(value)} ("#{some.expr()}");
        {value: String -> readAndUpdate(PropertyType.RESULT_VARIABLE_NAME, value).resultVariableName.shouldBeEqualTo(value)} ("NEW_RESULT_TO");
        {value: String -> readAndUpdate(PropertyType.SKIP_EXPRESSION, value).skipExpression.shouldBeEqualTo(value)} ("#{skipStuff.yes()}");
        {value: Boolean -> readAndUpdate(PropertyType.IS_TRIGGERABLE, value).triggerable.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE, value).useLocalScopeForResultVariable.shouldBeEqualTo(value)} (false);
    }

    @Test
    fun `Service task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DELEGATE_EXPRESSION).delegateExpression.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CLASS).clazz.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.EXPRESSION).expression.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.RESULT_VARIABLE_NAME).resultVariableName.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.SKIP_EXPRESSION).skipExpression.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnServiceTask {
        return readServiceTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnServiceTask {
        return readServiceTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnServiceTask {
        return readServiceTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readServiceTask(processObject: BpmnProcessObject): BpmnServiceTask {
        val task = processObject.process.body!!.serviceTask!!.shouldHaveSingleItem()
        return task
    }
}