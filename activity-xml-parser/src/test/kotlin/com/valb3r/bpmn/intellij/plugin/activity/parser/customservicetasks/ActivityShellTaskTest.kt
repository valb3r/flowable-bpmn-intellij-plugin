package com.valb3r.bpmn.intellij.plugin.activity.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnShellTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.activity.parser.ActivityObjectFactory
import com.valb3r.bpmn.intellij.plugin.activity.parser.ActivityParser
import com.valb3r.bpmn.intellij.plugin.activity.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activity.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.activity.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.activity.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/shell-task.bpmn20.xml"

internal class ActivityShellTaskTest {

    private val parser = ActivityParser()
    private val elementId = BpmnElementId("shellTaskId")

    @Test
    fun `Shell task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readShellTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Shell task name")
        task.documentation.shouldBeEqualTo("Docs for shell task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.command.shouldBeEqualTo("echo \"Hello\" > /tmp/foo.txt")
        task.arg1.shouldBeEqualTo("Arg1")
        task.arg2.shouldBeEqualTo("Arg2")
        task.arg3.shouldBeEqualTo("Arg3")
        task.arg4.shouldBeEqualTo("Arg4")
        task.arg5.shouldBeEqualTo("Arg5")
        task.wait.shouldBeEqualTo("WAIT")
        task.cleanEnv.shouldBeEqualTo("clear")
        task.errorCodeVariable.shouldBeEqualTo("ERR_CODE")
        task.outputVariable.shouldBeEqualTo("OUTPUT_VAR")
        task.directory.shouldBeEqualTo("/tmp")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(ActivityObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.COMMAND]!!.value.shouldBeEqualTo(task.command)
        props[PropertyType.ARG_1]!!.value.shouldBeEqualTo(task.arg1)
        props[PropertyType.ARG_2]!!.value.shouldBeEqualTo(task.arg2)
        props[PropertyType.ARG_3]!!.value.shouldBeEqualTo(task.arg3)
        props[PropertyType.ARG_4]!!.value.shouldBeEqualTo(task.arg4)
        props[PropertyType.ARG_5]!!.value.shouldBeEqualTo(task.arg5)
        props[PropertyType.WAIT]!!.value.shouldBeEqualTo(task.wait)
        props[PropertyType.CLEAN_ENV]!!.value.shouldBeEqualTo(task.cleanEnv)
        props[PropertyType.ERROR_CODE_VARIABLE]!!.value.shouldBeEqualTo(task.errorCodeVariable)
        props[PropertyType.OUTPUT_VARIABLE]!!.value.shouldBeEqualTo(task.outputVariable)
        props[PropertyType.DIRECTORY]!!.value.shouldBeEqualTo(task.directory)
    }

    @Test
    fun `Shell task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.COMMAND, value).command.shouldBeEqualTo(value)} ("echo '123' > /data/temp");
        {value: String -> readAndUpdate(PropertyType.ARG_1, value).arg1.shouldBeEqualTo(value)} ("'test1'");
        {value: String -> readAndUpdate(PropertyType.ARG_2, value).arg2.shouldBeEqualTo(value)} ("'test2'");
        {value: String -> readAndUpdate(PropertyType.ARG_3, value).arg3.shouldBeEqualTo(value)} ("'test3'");
        {value: String -> readAndUpdate(PropertyType.ARG_4, value).arg4.shouldBeEqualTo(value)} ("'test4'");
        {value: String -> readAndUpdate(PropertyType.ARG_5, value).arg5.shouldBeEqualTo(value)} ("'test5'");
        {value: String -> readAndUpdate(PropertyType.WAIT, value).wait.shouldBeEqualTo(value)} ("sleep 1000;");
        {value: String -> readAndUpdate(PropertyType.CLEAN_ENV, value).cleanEnv.shouldBeEqualTo(value)} ("rm -rf *");
        {value: String -> readAndUpdate(PropertyType.ERROR_CODE_VARIABLE, value).errorCodeVariable.shouldBeEqualTo(value)} ("ERR_CODE_1");
        {value: String -> readAndUpdate(PropertyType.OUTPUT_VARIABLE, value).outputVariable.shouldBeEqualTo(value)} ("OUTPUT_1");
        {value: String -> readAndUpdate(PropertyType.DIRECTORY, value).directory.shouldBeEqualTo(value)} ("/tmp/work/result")
    }

    @Test
    fun `Shell task is fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.COMMAND).command.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.ARG_1).arg1.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.ARG_2).arg2.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.ARG_3).arg3.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.ARG_4).arg4.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.ARG_5).arg5.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.WAIT).wait.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CLEAN_ENV).cleanEnv.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.ERROR_CODE_VARIABLE).errorCodeVariable.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.OUTPUT_VARIABLE).outputVariable.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DIRECTORY).directory.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnShellTask {
        return readShellTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnShellTask {
        return readShellTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnShellTask {
        return readShellTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readShellTask(processObject: BpmnProcessObject): BpmnShellTask {
        return processObject.process.body!!.shellTask!!.shouldHaveSingleItem()
    }
}