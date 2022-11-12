package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnSendEventTask
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

internal class FlowableSendEventTaskTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("sendEventId")

    @Test
    fun `Send event task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readSendEventTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("sendEventName")
        task.documentation.shouldBeEqualTo("Documentation for script send event task")
        task.async!!.shouldBeTrue()
        task.triggerable!!.shouldBeTrue()
        // TODO 'exclusive' ?

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.EVENT_TYPE]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].eventType)
        props[PropertyType.EVENT_NAME]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].eventName)
        props[PropertyType.TRIGGER_EVENT_TYPE]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].triggerEventType)
        props[PropertyType.TRIGGER_EVENT_NAME]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].triggerEventName)
        props[PropertyType.CHANNEL_KEY]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].channelKey)
        props[PropertyType.CHANNEL_NAME]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].channelName)
        props[PropertyType.CHANNEL_DESTINATION]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].channelDestination)
        props[PropertyType.TRIGGER_CHANNEL_KEY]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].triggerChannelKey)
        props[PropertyType.TRIGGER_CHANNEL_NAME]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].triggerChannelName)
        props[PropertyType.TRIGGER_CHANNEL_DESTINATION]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].triggerChannelDestination)
        props[PropertyType.EVENT_KEY_FIXED_VALUE]!!.value.shouldBeEqualTo(task.eventExtensionElements!![0].keyDetectionValue)
    }

    @Test
    fun `Send event task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.EVENT_TYPE, value).eventExtensionElements!![0].eventType.shouldBeEqualTo(value)} ("new event type");
        {value: String -> readAndUpdate(PropertyType.EVENT_NAME, value).eventExtensionElements!![0].eventName.shouldBeEqualTo(value)} ("new event name");
        {value: String -> readAndUpdate(PropertyType.TRIGGER_EVENT_TYPE, value).eventExtensionElements!![0].triggerEventType.shouldBeEqualTo(value)} ("new trigger event type");
        {value: String -> readAndUpdate(PropertyType.TRIGGER_EVENT_NAME, value).eventExtensionElements!![0].triggerEventName.shouldBeEqualTo(value)} ("new trigger event name");
        {value: String -> readAndUpdate(PropertyType.CHANNEL_KEY, value).eventExtensionElements!![0].channelKey.shouldBeEqualTo(value)} ("new channel key");
        {value: String -> readAndUpdate(PropertyType.CHANNEL_NAME, value).eventExtensionElements!![0].channelName.shouldBeEqualTo(value)} ("new channel name");
        {value: String -> readAndUpdate(PropertyType.CHANNEL_DESTINATION, value).eventExtensionElements!![0].channelDestination.shouldBeEqualTo(value)} ("new channel destination");
        {value: String -> readAndUpdate(PropertyType.TRIGGER_CHANNEL_KEY, value).eventExtensionElements!![0].triggerChannelKey.shouldBeEqualTo(value)} ("new trigger channel key");
        {value: String -> readAndUpdate(PropertyType.TRIGGER_CHANNEL_NAME, value).eventExtensionElements!![0].triggerChannelName.shouldBeEqualTo(value)} ("new trigger channel name");
        {value: String -> readAndUpdate(PropertyType.TRIGGER_CHANNEL_DESTINATION, value).eventExtensionElements!![0].triggerChannelDestination.shouldBeEqualTo(value)} ("new trigger channel destination");
        {value: String -> readAndUpdate(PropertyType.EVENT_KEY_FIXED_VALUE, value).eventExtensionElements!![0].keyDetectionValue.shouldBeEqualTo(value)} ("new event key fixed value");
    }

    @Test
    fun `Send event task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.EVENT_TYPE).eventExtensionElements!![0].eventType.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.EVENT_NAME).eventExtensionElements!![0].eventName.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.TRIGGER_EVENT_TYPE).eventExtensionElements!![0].triggerEventType.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.TRIGGER_EVENT_NAME).eventExtensionElements!![0].triggerEventName.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CHANNEL_KEY).eventExtensionElements!![0].channelKey.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CHANNEL_NAME).eventExtensionElements!![0].channelName.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CHANNEL_DESTINATION).eventExtensionElements!![0].channelDestination.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.TRIGGER_CHANNEL_KEY).eventExtensionElements!![0].triggerChannelKey.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.TRIGGER_CHANNEL_NAME).eventExtensionElements!![0].triggerChannelName.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.TRIGGER_CHANNEL_DESTINATION).eventExtensionElements!![0].triggerChannelDestination.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.EVENT_KEY_FIXED_VALUE).eventExtensionElements!![0].keyDetectionValue.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnSendEventTask {
        return readSendEventTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnSendEventTask {
        return readSendEventTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnSendEventTask {
        return readSendEventTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readSendEventTask(processObject: BpmnProcessObject): BpmnSendEventTask {
        return processObject.process.body!!.sendEventTask!!.shouldHaveSingleItem()
    }
}
private const val FILE = "custom-service-tasks/send-event-task.bpmn20.xml"

