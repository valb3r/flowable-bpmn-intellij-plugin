package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnScriptTask
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

private const val FILE = "custom-service-tasks/script-task.bpmn20.xml"

internal class FlowableScriptTaskTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("scriptServiceTask")

    @Test
    fun `Script task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readScriptTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("echoesFooBar")
        task.documentation.shouldBeEqualTo("Documentation for script service task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.scriptFormat.shouldBeEqualTo("bash")
        task.autoStoreVariables.shouldBeEqualTo(true)
        task.scriptBody.shouldBeEqualTo("echo \"Foo Bar!\" > /tmp/foo.txt")

        val props = BpmnFileObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.SCRIPT_FORMAT]!!.value.shouldBeEqualTo(task.scriptFormat)
        props[PropertyType.AUTO_STORE_VARIABLES]!!.value.shouldBeEqualTo(task.autoStoreVariables)
        props[PropertyType.SCRIPT]!!.value.shouldBeEqualTo(task.scriptBody)
    }

    @Test
    fun `Script task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.SCRIPT_FORMAT, value).scriptFormat.shouldBeEqualTo(value)} ("groovy");
        {value: Boolean -> readAndUpdate(PropertyType.AUTO_STORE_VARIABLES, value).autoStoreVariables.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.SCRIPT, value).scriptBody.shouldBeEqualTo(value)} ("{def aa = 1}")
    }

    @Test
    fun `Script task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.SCRIPT_FORMAT).scriptFormat.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.SCRIPT).scriptBody.shouldBeNullOrEmpty()
    }
    
    private fun readAndSetNullString(property: PropertyType): BpmnScriptTask {
        return readScriptTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }
    
    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnScriptTask {
        return readScriptTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnScriptTask {
        return readScriptTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readScriptTask(processObject: BpmnFileObject): BpmnScriptTask {
        return processObject.process.body!!.scriptTask!!.shouldHaveSingleItem()
    }
}
