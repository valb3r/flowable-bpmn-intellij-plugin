package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.BpmnFile
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.DiagramNode
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.ProcessNode
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.BaseBpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.NS
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.PropertyTypeDetails
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.XmlType
import org.dom4j.Element


const val CDATA_FIELD = "CDATA"

enum class ActivitiPropertyTypeDetails(val details: PropertyTypeDetails) {
    ID(PropertyTypeDetails(PropertyType.ID, "id", XmlType.ATTRIBUTE)),
    NAME(PropertyTypeDetails(PropertyType.NAME,"name", XmlType.ATTRIBUTE)),
    DOCUMENTATION(PropertyTypeDetails(PropertyType.DOCUMENTATION, "documentation.text", XmlType.CDATA, forceFirst = true)),
    IS_FOR_COMPENSATION(PropertyTypeDetails(PropertyType.IS_FOR_COMPENSATION, "isForCompensation", XmlType.ATTRIBUTE)),
    ASYNC(PropertyTypeDetails(PropertyType.ASYNC, "activiti:async", XmlType.ATTRIBUTE)),
    ASSIGNEE(PropertyTypeDetails(PropertyType.ASSIGNEE, "activiti:assignee", XmlType.ATTRIBUTE)),
    CANDIDATE_USERS(PropertyTypeDetails(PropertyType.CANDIDATE_USERS, "activiti:candidateUsers", XmlType.ATTRIBUTE)),
    CANDIDATE_GROUPS(PropertyTypeDetails(PropertyType.CANDIDATE_GROUPS, "activiti:candidateGroups", XmlType.ATTRIBUTE)),
    CALLED_ELEM(PropertyTypeDetails(PropertyType.CALLED_ELEM, "calledElement", XmlType.ATTRIBUTE)),
    CALLED_ELEM_TYPE(PropertyTypeDetails(PropertyType.CALLED_ELEM_TYPE, "activiti:calledElementType", XmlType.ATTRIBUTE)),
    INHERIT_VARS(PropertyTypeDetails(PropertyType.INHERIT_VARS, "activiti:inheritVariables", XmlType.ATTRIBUTE)),
    FALLBACK_TO_DEF_TENANT(PropertyTypeDetails(PropertyType.FALLBACK_TO_DEF_TENANT, "activiti:fallbackToDefaultTenant", XmlType.ATTRIBUTE)),
    EXCLUSIVE(PropertyTypeDetails(PropertyType.EXCLUSIVE,"activiti:exclusive", XmlType.ATTRIBUTE)),
    EXPRESSION(PropertyTypeDetails(PropertyType.EXPRESSION, "activiti:expression", XmlType.ATTRIBUTE)),
    DELEGATE_EXPRESSION(PropertyTypeDetails(PropertyType.DELEGATE_EXPRESSION, "activiti:delegateExpression", XmlType.ATTRIBUTE)),
    CLASS(PropertyTypeDetails(PropertyType.CLASS, "activiti:class", XmlType.ATTRIBUTE)),
    DUE_DATE(PropertyTypeDetails(PropertyType.DUE_DATE, "activiti:dueDate", XmlType.ATTRIBUTE)),
    FORM_KEY(PropertyTypeDetails(PropertyType.FORM_KEY, "activiti:formKey", XmlType.ATTRIBUTE)),
    PRIORITY(PropertyTypeDetails(PropertyType.PRIORITY, "activiti:priority", XmlType.ATTRIBUTE)),
    SCRIPT(PropertyTypeDetails(PropertyType.SCRIPT, "script.text", XmlType.CDATA)),
    SCRIPT_FORMAT(PropertyTypeDetails(PropertyType.SCRIPT_FORMAT, "scriptFormat", XmlType.ATTRIBUTE)),
    AUTO_STORE_VARIABLES(PropertyTypeDetails(PropertyType.AUTO_STORE_VARIABLES, "activiti:autoStoreVariables", XmlType.ATTRIBUTE)), // Doesn't seem that we are able to change this from Activiti UI, but is stored
    RULE_VARIABLES_INPUT(PropertyTypeDetails(PropertyType.RULE_VARIABLES_INPUT, "activiti:ruleVariablesInput", XmlType.ATTRIBUTE)),
    RULES(PropertyTypeDetails(PropertyType.RULES, "activiti:rules", XmlType.ATTRIBUTE)),
    RESULT_VARIABLE(PropertyTypeDetails(PropertyType.RESULT_VARIABLE, "activiti:resultVariable", XmlType.ATTRIBUTE)),
    RESULT_VARIABLE_NAME(PropertyTypeDetails(PropertyType.RESULT_VARIABLE_NAME, "activiti:resultVariableName", XmlType.ATTRIBUTE)),
    EXCLUDE(PropertyTypeDetails(PropertyType.EXCLUDE, "activiti:exclude", XmlType.ATTRIBUTE)),
    SOURCE_REF(PropertyTypeDetails(PropertyType.SOURCE_REF,"sourceRef", XmlType.ATTRIBUTE)),
    TARGET_REF(PropertyTypeDetails(PropertyType.TARGET_REF, "targetRef", XmlType.ATTRIBUTE)),
    ATTACHED_TO_REF(PropertyTypeDetails(PropertyType.ATTACHED_TO_REF, "attachedToRef", XmlType.ATTRIBUTE)),
    CONDITION_EXPR_VALUE(PropertyTypeDetails(PropertyType.CONDITION_EXPR_VALUE, "conditionExpression.text", XmlType.CDATA)),
    CONDITION_EXPR_TYPE(PropertyTypeDetails(PropertyType.CONDITION_EXPR_TYPE, "conditionExpression.xsi:type", XmlType.ATTRIBUTE)),
    COMPLETION_CONDITION(PropertyTypeDetails(PropertyType.COMPLETION_CONDITION, "completionCondition.text", XmlType.CDATA)),
    DEFAULT_FLOW(PropertyTypeDetails(PropertyType.DEFAULT_FLOW, "default", XmlType.ATTRIBUTE)),
    IS_TRANSACTIONAL_SUBPROCESS(PropertyTypeDetails(PropertyType.IS_TRANSACTIONAL_SUBPROCESS, "transactionalSubprocess", XmlType.ELEMENT)),
    CAMEL_CONTEXT(PropertyTypeDetails(PropertyType.CAMEL_CONTEXT, "extensionElements.activiti:field?name=camelContext.activiti:string.text", XmlType.CDATA)),
    DECISION_TABLE_REFERENCE_KEY(PropertyTypeDetails(PropertyType.DECISION_TABLE_REFERENCE_KEY, "extensionElements.activiti:field?name=decisionTableReferenceKey.activiti:string.text", XmlType.CDATA)),
    REQUEST_METHOD(PropertyTypeDetails(PropertyType.REQUEST_METHOD, "extensionElements.activiti:field?name=requestMethod.activiti:string.text", XmlType.CDATA)),
    REQUEST_URL(PropertyTypeDetails(PropertyType.REQUEST_URL, "extensionElements.activiti:field?name=requestUrl.activiti:string.text", XmlType.CDATA)),
    REQUEST_HEADERS(PropertyTypeDetails(PropertyType.REQUEST_HEADERS, "extensionElements.activiti:field?name=requestHeaders.activiti:string.text", XmlType.CDATA)),
    REQUEST_BODY(PropertyTypeDetails(PropertyType.REQUEST_BODY, "extensionElements.activiti:field?name=requestBody.activiti:string.text", XmlType.CDATA)),
    REQUEST_BODY_ENCODING(PropertyTypeDetails(PropertyType.REQUEST_BODY_ENCODING, "extensionElements.activiti:field?name=requestBodyEncoding.activiti:string.text", XmlType.CDATA)),
    REQUEST_TIMEOUT(PropertyTypeDetails(PropertyType.REQUEST_TIMEOUT, "extensionElements.activiti:field?name=requestTimeout.activiti:string.text", XmlType.CDATA)),
    DISALLOW_REDIRECTS(PropertyTypeDetails(PropertyType.DISALLOW_REDIRECTS, "extensionElements.activiti:field?name=disallowRedirects.activiti:string.text", XmlType.CDATA)),
    FAIL_STATUS_CODES(PropertyTypeDetails(PropertyType.FAIL_STATUS_CODES, "extensionElements.activiti:field?name=failStatusCodes.activiti:string.text", XmlType.CDATA)),
    HANDLE_STATUS_CODES(PropertyTypeDetails(PropertyType.HANDLE_STATUS_CODES, "extensionElements.activiti:field?name=handleStatusCodes.activiti:string.text", XmlType.CDATA)),
    RESPONSE_VARIABLE_NAME(PropertyTypeDetails(PropertyType.RESPONSE_VARIABLE_NAME, "extensionElements.activiti:field?name=responseVariableName.activiti:string.text", XmlType.CDATA)),
    IGNORE_EXCEPTION(PropertyTypeDetails(PropertyType.IGNORE_EXCEPTION, "extensionElements.activiti:field?name=ignoreException.activiti:string.text", XmlType.CDATA)),
    SAVE_REQUEST_VARIABLES(PropertyTypeDetails(PropertyType.SAVE_REQUEST_VARIABLES, "extensionElements.activiti:field?name=saveRequestVariables.activiti:string.text", XmlType.CDATA)),
    SAVE_RESPONSE_PARAMETERS(PropertyTypeDetails(PropertyType.SAVE_RESPONSE_PARAMETERS, "extensionElements.activiti:field?name=saveResponseParameters.activiti:string.text", XmlType.CDATA)),
    RESULT_VARIABLE_PREFIX(PropertyTypeDetails(PropertyType.RESULT_VARIABLE_PREFIX, "extensionElements.activiti:field?name=resultVariablePrefix.activiti:string.text", XmlType.CDATA)),
    SAVE_RESPONSE_PARAMETERS_TRANSIENT(PropertyTypeDetails(PropertyType.SAVE_RESPONSE_PARAMETERS_TRANSIENT, "extensionElements.activiti:field?name=saveResponseParametersTransient.activiti:string.text", XmlType.CDATA)),
    SAVE_RESPONSE_VARIABLE_AS_JSON(PropertyTypeDetails(PropertyType.SAVE_RESPONSE_VARIABLE_AS_JSON, "extensionElements.activiti:field?name=saveResponseVariableAsJson.activiti:string.text", XmlType.CDATA)),
    TO(PropertyTypeDetails(PropertyType.TO, "extensionElements.activiti:field?name=to.activiti:string.text", XmlType.CDATA)),
    FROM(PropertyTypeDetails(PropertyType.FROM, "extensionElements.activiti:field?name=from.activiti:string.text", XmlType.CDATA)),
    SUBJECT(PropertyTypeDetails(PropertyType.SUBJECT, "extensionElements.activiti:field?name=subject.activiti:string.text", XmlType.CDATA)),
    CC(PropertyTypeDetails(PropertyType.CC, "extensionElements.activiti:field?name=cc.activiti:string.text", XmlType.CDATA)),
    BCC(PropertyTypeDetails(PropertyType.BCC, "extensionElements.activiti:field?name=bcc.activiti:string.text", XmlType.CDATA)),
    TEXT(PropertyTypeDetails(PropertyType.TEXT, "extensionElements.activiti:field?name=text.activiti:string.text", XmlType.CDATA)),
    HTML(PropertyTypeDetails(PropertyType.HTML, "extensionElements.activiti:field?name=html.activiti:string.text", XmlType.CDATA)),
    CHARSET(PropertyTypeDetails(PropertyType.CHARSET, "extensionElements.activiti:field?name=charset.activiti:string.text", XmlType.CDATA)),
    ENDPOINT_URL(PropertyTypeDetails(PropertyType.ENDPOINT_URL, "extensionElements.activiti:field?name=endpointUrl.activiti:string.text", XmlType.CDATA)),
    LANGUAGE(PropertyTypeDetails(PropertyType.LANGUAGE, "extensionElements.activiti:field?name=language.activiti:string.text", XmlType.CDATA)),
    PAYLOAD_EXPRESSION(PropertyTypeDetails(PropertyType.PAYLOAD_EXPRESSION, "extensionElements.activiti:field?name=payloadExpression.activiti:expression.text", XmlType.CDATA)),
    RESULT_VARIABLE_CDATA(PropertyTypeDetails(PropertyType.RESULT_VARIABLE_CDATA, "extensionElements.activiti:field?name=resultVariable.activiti:string.text", XmlType.CDATA)),
    COMMAND(PropertyTypeDetails(PropertyType.COMMAND, "extensionElements.activiti:field?name=command.activiti:string.text", XmlType.CDATA)),
    ARG_1(PropertyTypeDetails(PropertyType.ARG_1, "extensionElements.activiti:field?name=arg1.activiti:string.text", XmlType.CDATA)),
    ARG_2(PropertyTypeDetails(PropertyType.ARG_2, "extensionElements.activiti:field?name=arg2.activiti:string.text", XmlType.CDATA)),
    ARG_3(PropertyTypeDetails(PropertyType.ARG_3, "extensionElements.activiti:field?name=arg3.activiti:string.text", XmlType.CDATA)),
    ARG_4(PropertyTypeDetails(PropertyType.ARG_4, "extensionElements.activiti:field?name=arg4.activiti:string.text", XmlType.CDATA)),
    ARG_5(PropertyTypeDetails(PropertyType.ARG_5, "extensionElements.activiti:field?name=arg5.activiti:string.text", XmlType.CDATA)),
    WAIT(PropertyTypeDetails(PropertyType.WAIT, "extensionElements.activiti:field?name=wait.activiti:string.text", XmlType.CDATA)),
    CLEAN_ENV(PropertyTypeDetails(PropertyType.CLEAN_ENV, "extensionElements.activiti:field?name=cleanEnv.activiti:string.text", XmlType.CDATA)),
    ERROR_CODE_VARIABLE(PropertyTypeDetails(PropertyType.ERROR_CODE_VARIABLE, "extensionElements.activiti:field?name=errorCodeVariable.activiti:string.text", XmlType.CDATA)),
    OUTPUT_VARIABLE(PropertyTypeDetails(PropertyType.OUTPUT_VARIABLE, "extensionElements.activiti:field?name=outputVariable.activiti:string.text", XmlType.CDATA)),
    DIRECTORY(PropertyTypeDetails(PropertyType.DIRECTORY, "extensionElements.activiti:field?name=directory.activiti:string.text", XmlType.CDATA)),
    FAILED_JOB_RETRY_CYCLE(PropertyTypeDetails(PropertyType.FAILED_JOB_RETRY_CYCLE, "extensionElements.activiti:failedJobRetryTimeCycle.text", XmlType.CDATA)),
    FIELD_NAME(PropertyTypeDetails(PropertyType.FIELD_NAME, "extensionElements.activiti:field?name=@.name", XmlType.ATTRIBUTE)),
    FIELD_EXPRESSION(PropertyTypeDetails(PropertyType.FIELD_EXPRESSION, "extensionElements.activiti:field?name=@.activiti:expression.text", XmlType.CDATA)),
    FIELD_STRING(PropertyTypeDetails(PropertyType.FIELD_STRING, "extensionElements.activiti:field?name=@.activiti:string.text", XmlType.CDATA)),
    FORM_PROPERTY_ID(PropertyTypeDetails(PropertyType.FORM_PROPERTY_ID, "extensionElements.activiti:formProperty?id=@.id", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_NAME(PropertyTypeDetails(PropertyType.FORM_PROPERTY_NAME, "extensionElements.activiti:formProperty?id=@.name", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_TYPE(PropertyTypeDetails(PropertyType.FORM_PROPERTY_TYPE, "extensionElements.activiti:formProperty?id=@.type", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_VARIABLE(PropertyTypeDetails(PropertyType.FORM_PROPERTY_VARIABLE, "extensionElements.activiti:formProperty?id=@.variable", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_DEFAULT(PropertyTypeDetails(PropertyType.FORM_PROPERTY_DEFAULT, "extensionElements.activiti:formProperty?id=@.default", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_EXPRESSION(PropertyTypeDetails(PropertyType.FORM_PROPERTY_EXPRESSION, "extensionElements.activiti:formProperty?id=@.expression", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_DATE_PATTERN(PropertyTypeDetails(PropertyType.FORM_PROPERTY_DATE_PATTERN, "extensionElements.activiti:formProperty?id=@.datePattern", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_VALUE_ID(PropertyTypeDetails(PropertyType.FORM_PROPERTY_VALUE_ID, "extensionElements.activiti:formProperty?id=@.activiti:value?id=@.id", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_VALUE_NAME(PropertyTypeDetails(PropertyType.FORM_PROPERTY_VALUE_NAME, "extensionElements.activiti:formProperty?id=@.activiti:value?id=@.name", XmlType.ATTRIBUTE)),
    EXECUTION_LISTENER_CLASS(PropertyTypeDetails(PropertyType.EXECUTION_LISTENER_CLASS, "extensionElements.activiti:executionListener?class=@.class", XmlType.ATTRIBUTE)),
    EXECUTION_LISTENER_EVENT(PropertyTypeDetails(PropertyType.EXECUTION_LISTENER_EVENT, "extensionElements.activiti:executionListener?class=@.event", XmlType.ATTRIBUTE)),
    EXECUTION_LISTENER_FIELD_NAME(PropertyTypeDetails(PropertyType.EXECUTION_LISTENER_FIELD_NAME, "extensionElements.activiti:executionListener?class=@.activiti:field?name=@.name", XmlType.ATTRIBUTE)),
    EXECUTION_LISTENER_FIELD_STRING(PropertyTypeDetails(PropertyType.EXECUTION_LISTENER_FIELD_STRING, "extensionElements.activiti:executionListener?class=@.activiti:field?name=@.activiti:string.text", XmlType.CDATA)),
}

open class ActivitiParser : BaseBpmnParser() {

    private val mapper: XmlMapper = mapper()

    override fun parse(input: String): BpmnFileObject {
        val dto = mapper.readValue<BpmnFile>(input)
        return toProcessObject(dto)
    }

    private fun toProcessObject(dto: BpmnFile): BpmnFileObject {
        markSubprocessesAndTransactionsThatHaveExternalDiagramAsCollapsed(dto.processes[0], dto.diagrams!!)
        val processes = dto.processes.map { it.toElement() }
        val collaborations = dto.collaborations?.map { it.toElement() } ?: emptyList()
        val diagrams = dto.diagrams!!.map { it.toElement() }

        return BpmnFileObject(processes, collaborations, diagrams)
    }

    override fun modelNs(): NS {
        return NS("", "http://www.omg.org/spec/BPMN/20100524/MODEL")
    }

    override fun bpmndiNs(): NS {
        return NS("bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI")
    }

    override fun omgdcNs(): NS {
        return NS("omgdc", "http://www.omg.org/spec/DD/20100524/DC")
    }

    override fun omgdiNs(): NS {
        return NS("omgdi", "http://www.omg.org/spec/DD/20100524/DI")
    }

    override fun xsiNs(): NS {
        return NS("xsi", "http://www.w3.org/2001/XMLSchema-instance")
    }

    override fun engineNs(): NS {
        return NS("activiti", "http://activiti.org/bpmn")
    }

    override fun changeElementType(node: Element, name: String, details: PropertyTypeDetails, value: String?) {
        if (ActivitiPropertyTypeDetails.IS_TRANSACTIONAL_SUBPROCESS.details != details) {
            throw IllegalArgumentException("Can't change type for: ${details.javaClass.canonicalName}")
        }

        if (null == value || !value.toBoolean()) {
            node.qName = modelNs().named("subProcess")
        } else {
            node.qName = modelNs().named("transaction")
        }
    }

    override fun propertyTypeDetails(): List<PropertyTypeDetails> {
        return ActivitiPropertyTypeDetails.values().map { it.details }
    }

    // Mark 'collapsed' subprocesses where diagram is different from 1st one
    private fun markSubprocessesAndTransactionsThatHaveExternalDiagramAsCollapsed(
        process: ProcessNode,
        diagrams: List<DiagramNode>
    ) {
        if (diagrams.size <= 1) {
            return
        }

        val externalDiagrams = diagrams.subList(1, diagrams.size).map { it.bpmnPlane.bpmnElement }.toSet()

        fun elementIsInExternalDiagram(id: String?): Boolean {
            return externalDiagrams.contains(id)
        }

        process.subProcess?.forEach { it.hasExternalDiagram = elementIsInExternalDiagram(it.id) }
        process.transaction?.forEach { it.hasExternalDiagram = elementIsInExternalDiagram(it.id) }
    }
}
