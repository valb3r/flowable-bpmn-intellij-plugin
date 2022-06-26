package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnBusinessRuleTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/business-rule-task.bpmn20.xml"

internal class ActivityBusinessRuleTaskTest {

    private val parser = ActivitiParser()
    private val elementId = BpmnElementId("businessRuleTask")

    @Test
    fun `Business rule task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readBusinessRuleTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Business rule task")
        task.documentation.shouldBeEqualTo("Docs for business rule task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.ruleVariablesInput.shouldBeEqualTo("INPUT_VAR")
        task.rules.shouldBeEqualTo("Rule1,Rule2")
        task.resultVariable.shouldBeEqualTo("RESULT_VAR")
        task.exclude!!.shouldBeTrue()

        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(ActivitiObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.RULE_VARIABLES_INPUT]!!.value.shouldBeEqualTo(task.ruleVariablesInput)
        props[PropertyType.RULES]!!.value.shouldBeEqualTo(task.rules)
        props[PropertyType.RESULT_VARIABLE]!!.value.shouldBeEqualTo(task.resultVariable)
        props[PropertyType.EXCLUDE]!!.value.shouldBeEqualTo(task.exclude)
    }

    @Test
    fun `Business rule task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.RULE_VARIABLES_INPUT, value).ruleVariablesInput.shouldBeEqualTo(value)} ("New inputs");
        {value: String -> readAndUpdate(PropertyType.RULES, value).rules.shouldBeEqualTo(value)} ("Rules");
        {value: String -> readAndUpdate(PropertyType.RESULT_VARIABLE, value).resultVariable.shouldBeEqualTo(value)} ("New result variable");
        {value: Boolean -> readAndUpdate(PropertyType.EXCLUDE, value).exclude.shouldBeEqualTo(value)} (false)
    }

    @Test
    fun `Business rule task fields are emptyable or removable if null`() {
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.IS_FOR_COMPENSATION).isForCompensation.shouldBeNull()
        readAndSetNullString(PropertyType.RULE_VARIABLES_INPUT).ruleVariablesInput.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.RULES).rules.shouldBeNull()
        readAndSetNullString(PropertyType.RESULT_VARIABLE).resultVariable.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnBusinessRuleTask {
        return readBusinessRuleTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnBusinessRuleTask {
        return readBusinessRuleTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnBusinessRuleTask {
        return readBusinessRuleTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readBusinessRuleTask(processObject: BpmnFileObject): BpmnBusinessRuleTask {
        return processObject.processes[0].body!!.businessRuleTask!!.shouldHaveSingleItem()
    }
}
