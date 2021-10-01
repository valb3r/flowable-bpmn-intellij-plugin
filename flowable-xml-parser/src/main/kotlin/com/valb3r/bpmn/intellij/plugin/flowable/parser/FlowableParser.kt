package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.BaseBpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.NS
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.PropertyTypeDetails
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.XmlType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnFile
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.DiagramNode
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.ProcessNode
import org.dom4j.Element


enum class FlowablePropertyTypeDetails(val details: PropertyTypeDetails) {
    ID(PropertyTypeDetails(PropertyType.ID, "id", XmlType.ATTRIBUTE)),
    NAME(PropertyTypeDetails(PropertyType.NAME,"name", XmlType.ATTRIBUTE)),
    DOCUMENTATION(PropertyTypeDetails(PropertyType.DOCUMENTATION, "documentation.text", XmlType.CDATA)),
    IS_FOR_COMPENSATION(PropertyTypeDetails(PropertyType.IS_FOR_COMPENSATION, "isForCompensation", XmlType.ATTRIBUTE)),
    ASYNC(PropertyTypeDetails(PropertyType.ASYNC, "flowable:async", XmlType.ATTRIBUTE)),
    ASSIGNEE(PropertyTypeDetails(PropertyType.ASSIGNEE, "flowable:assignee", XmlType.ATTRIBUTE)),
    CALLED_ELEM(PropertyTypeDetails(PropertyType.CALLED_ELEM, "calledElement", XmlType.ATTRIBUTE)),
    CALLED_ELEM_TYPE(PropertyTypeDetails(PropertyType.CALLED_ELEM_TYPE, "flowable:calledElementType", XmlType.ATTRIBUTE)),
    INHERIT_VARS(PropertyTypeDetails(PropertyType.INHERIT_VARS, "flowable:inheritVariables", XmlType.ATTRIBUTE)),
    FALLBACK_TO_DEF_TENANT(PropertyTypeDetails(PropertyType.FALLBACK_TO_DEF_TENANT, "flowable:fallbackToDefaultTenant", XmlType.ATTRIBUTE)),
    EXCLUSIVE(PropertyTypeDetails(PropertyType.EXCLUSIVE,"flowable:exclusive", XmlType.ATTRIBUTE)),
    EXPRESSION(PropertyTypeDetails(PropertyType.EXPRESSION, "flowable:expression", XmlType.ATTRIBUTE)),
    DELEGATE_EXPRESSION(PropertyTypeDetails(PropertyType.DELEGATE_EXPRESSION, "flowable:delegateExpression", XmlType.ATTRIBUTE)),
    CLASS(PropertyTypeDetails(PropertyType.CLASS, "flowable:class", XmlType.ATTRIBUTE)),
    SKIP_EXPRESSION(PropertyTypeDetails(PropertyType.SKIP_EXPRESSION, "flowable:skipExpression", XmlType.ATTRIBUTE)),
    IS_TRIGGERABLE(PropertyTypeDetails(PropertyType.IS_TRIGGERABLE, "flowable:triggerable", XmlType.ATTRIBUTE)),
    DUE_DATE(PropertyTypeDetails(PropertyType.DUE_DATE, "flowable:dueDate", XmlType.ATTRIBUTE)),
    CATEGORY(PropertyTypeDetails(PropertyType.CATEGORY, "flowable:category", XmlType.ATTRIBUTE)),
    FORM_KEY(PropertyTypeDetails(PropertyType.FORM_KEY, "flowable:formKey", XmlType.ATTRIBUTE)),
    FORM_FIELD_VALIDATION(PropertyTypeDetails(PropertyType.FORM_FIELD_VALIDATION, "flowable:formFieldValidation", XmlType.ATTRIBUTE)),
    PRIORITY(PropertyTypeDetails(PropertyType.PRIORITY, "flowable:priority", XmlType.ATTRIBUTE)),
    SCRIPT(PropertyTypeDetails(PropertyType.SCRIPT, "script.text", XmlType.CDATA)),
    SCRIPT_FORMAT(PropertyTypeDetails(PropertyType.SCRIPT_FORMAT, "scriptFormat", XmlType.ATTRIBUTE)),
    AUTO_STORE_VARIABLES(PropertyTypeDetails(PropertyType.AUTO_STORE_VARIABLES, "flowable:autoStoreVariables", XmlType.ATTRIBUTE)),
    RULE_VARIABLES_INPUT(PropertyTypeDetails(PropertyType.RULE_VARIABLES_INPUT, "flowable:ruleVariablesInput", XmlType.ATTRIBUTE)),
    RULES(PropertyTypeDetails(PropertyType.RULES, "flowable:rules", XmlType.ATTRIBUTE)),
    RESULT_VARIABLE(PropertyTypeDetails(PropertyType.RESULT_VARIABLE, "flowable:resultVariable", XmlType.ATTRIBUTE)),
    RESULT_VARIABLE_NAME(PropertyTypeDetails(PropertyType.RESULT_VARIABLE_NAME, "flowable:resultVariableName", XmlType.ATTRIBUTE)),
    EXCLUDE(PropertyTypeDetails(PropertyType.EXCLUDE, "flowable:exclude", XmlType.ATTRIBUTE)),
    SOURCE_REF(PropertyTypeDetails(PropertyType.SOURCE_REF,"sourceRef", XmlType.ATTRIBUTE)),
    TARGET_REF(PropertyTypeDetails(PropertyType.TARGET_REF, "targetRef", XmlType.ATTRIBUTE)),
    ATTACHED_TO_REF(PropertyTypeDetails(PropertyType.ATTACHED_TO_REF, "attachedToRef", XmlType.ATTRIBUTE)),
    CONDITION_EXPR_VALUE(PropertyTypeDetails(PropertyType.CONDITION_EXPR_VALUE, "conditionExpression.text", XmlType.CDATA)),
    CONDITION_EXPR_TYPE(PropertyTypeDetails(PropertyType.CONDITION_EXPR_TYPE, "conditionExpression.xsi:type", XmlType.ATTRIBUTE)),
    COMPLETION_CONDITION(PropertyTypeDetails(PropertyType.COMPLETION_CONDITION, "completionCondition.text", XmlType.CDATA)),
    DEFAULT_FLOW(PropertyTypeDetails(PropertyType.DEFAULT_FLOW, "default", XmlType.ATTRIBUTE)),
    IS_TRANSACTIONAL_SUBPROCESS(PropertyTypeDetails(PropertyType.IS_TRANSACTIONAL_SUBPROCESS, "transactionalSubprocess", XmlType.ELEMENT)),
    IS_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE(PropertyTypeDetails(PropertyType.IS_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE, "flowable:useLocalScopeForResultVariable", XmlType.ATTRIBUTE)),
    CAMEL_CONTEXT(PropertyTypeDetails(PropertyType.CAMEL_CONTEXT, "extensionElements.flowable:field?name=camelContext.flowable:string.text", XmlType.CDATA)),
    DECISION_TABLE_REFERENCE_KEY(PropertyTypeDetails(PropertyType.DECISION_TABLE_REFERENCE_KEY, "extensionElements.flowable:field?name=decisionTableReferenceKey.flowable:string.text", XmlType.CDATA)),
    DECISION_TASK_THROW_ERROR_ON_NO_HITS(PropertyTypeDetails(PropertyType.DECISION_TASK_THROW_ERROR_ON_NO_HITS, "extensionElements.flowable:field?name=decisionTaskThrowErrorOnNoHits.flowable:string.text", XmlType.CDATA)),
    FALLBACK_TO_DEF_TENANT_CDATA(PropertyTypeDetails(PropertyType.FALLBACK_TO_DEF_TENANT_CDATA, "extensionElements.flowable:field?name=fallbackToDefaultTenant.flowable:string.text", XmlType.CDATA)),
    REQUEST_METHOD(PropertyTypeDetails(PropertyType.REQUEST_METHOD, "extensionElements.flowable:field?name=requestMethod.flowable:string.text", XmlType.CDATA)),
    REQUEST_URL(PropertyTypeDetails(PropertyType.REQUEST_URL, "extensionElements.flowable:field?name=requestUrl.flowable:string.text", XmlType.CDATA)),
    REQUEST_HEADERS(PropertyTypeDetails(PropertyType.REQUEST_HEADERS, "extensionElements.flowable:field?name=requestHeaders.flowable:string.text", XmlType.CDATA)),
    REQUEST_BODY(PropertyTypeDetails(PropertyType.REQUEST_BODY, "extensionElements.flowable:field?name=requestBody.flowable:string.text", XmlType.CDATA)),
    REQUEST_BODY_ENCODING(PropertyTypeDetails(PropertyType.REQUEST_BODY_ENCODING, "extensionElements.flowable:field?name=requestBodyEncoding.flowable:string.text", XmlType.CDATA)),
    REQUEST_TIMEOUT(PropertyTypeDetails(PropertyType.REQUEST_TIMEOUT, "extensionElements.flowable:field?name=requestTimeout.flowable:string.text", XmlType.CDATA)),
    DISALLOW_REDIRECTS(PropertyTypeDetails(PropertyType.DISALLOW_REDIRECTS, "extensionElements.flowable:field?name=disallowRedirects.flowable:string.text", XmlType.CDATA)),
    FAIL_STATUS_CODES(PropertyTypeDetails(PropertyType.FAIL_STATUS_CODES, "extensionElements.flowable:field?name=failStatusCodes.flowable:string.text", XmlType.CDATA)),
    HANDLE_STATUS_CODES(PropertyTypeDetails(PropertyType.HANDLE_STATUS_CODES, "extensionElements.flowable:field?name=handleStatusCodes.flowable:string.text", XmlType.CDATA)),
    RESPONSE_VARIABLE_NAME(PropertyTypeDetails(PropertyType.RESPONSE_VARIABLE_NAME, "extensionElements.flowable:field?name=responseVariableName.flowable:string.text", XmlType.CDATA)),
    IGNORE_EXCEPTION(PropertyTypeDetails(PropertyType.IGNORE_EXCEPTION, "extensionElements.flowable:field?name=ignoreException.flowable:string.text", XmlType.CDATA)),
    SAVE_REQUEST_VARIABLES(PropertyTypeDetails(PropertyType.SAVE_REQUEST_VARIABLES, "extensionElements.flowable:field?name=saveRequestVariables.flowable:string.text", XmlType.CDATA)),
    SAVE_RESPONSE_PARAMETERS(PropertyTypeDetails(PropertyType.SAVE_RESPONSE_PARAMETERS, "extensionElements.flowable:field?name=saveResponseParameters.flowable:string.text", XmlType.CDATA)),
    RESULT_VARIABLE_PREFIX(PropertyTypeDetails(PropertyType.RESULT_VARIABLE_PREFIX, "extensionElements.flowable:field?name=resultVariablePrefix.flowable:string.text", XmlType.CDATA)),
    SAVE_RESPONSE_PARAMETERS_TRANSIENT(PropertyTypeDetails(PropertyType.SAVE_RESPONSE_PARAMETERS_TRANSIENT, "extensionElements.flowable:field?name=saveResponseParametersTransient.flowable:string.text", XmlType.CDATA)),
    SAVE_RESPONSE_VARIABLE_AS_JSON(PropertyTypeDetails(PropertyType.SAVE_RESPONSE_VARIABLE_AS_JSON, "extensionElements.flowable:field?name=saveResponseVariableAsJson.flowable:string.text", XmlType.CDATA)),
    HEADERS(PropertyTypeDetails(PropertyType.HEADERS, "extensionElements.flowable:field?name=headers.flowable:string.text", XmlType.CDATA)),
    TO(PropertyTypeDetails(PropertyType.TO, "extensionElements.flowable:field?name=to.flowable:string.text", XmlType.CDATA)),
    FROM(PropertyTypeDetails(PropertyType.FROM, "extensionElements.flowable:field?name=from.flowable:string.text", XmlType.CDATA)),
    SUBJECT(PropertyTypeDetails(PropertyType.SUBJECT, "extensionElements.flowable:field?name=subject.flowable:string.text", XmlType.CDATA)),
    CC(PropertyTypeDetails(PropertyType.CC, "extensionElements.flowable:field?name=cc.flowable:string.text", XmlType.CDATA)),
    BCC(PropertyTypeDetails(PropertyType.BCC, "extensionElements.flowable:field?name=bcc.flowable:string.text", XmlType.CDATA)),
    TEXT(PropertyTypeDetails(PropertyType.TEXT, "extensionElements.flowable:field?name=text.flowable:string.text", XmlType.CDATA)),
    HTML(PropertyTypeDetails(PropertyType.HTML, "extensionElements.flowable:field?name=html.flowable:string.text", XmlType.CDATA)),
    CHARSET(PropertyTypeDetails(PropertyType.CHARSET, "extensionElements.flowable:field?name=charset.flowable:string.text", XmlType.CDATA)),
    ENDPOINT_URL(PropertyTypeDetails(PropertyType.ENDPOINT_URL, "extensionElements.flowable:field?name=endpointUrl.flowable:string.text", XmlType.CDATA)),
    LANGUAGE(PropertyTypeDetails(PropertyType.LANGUAGE, "extensionElements.flowable:field?name=language.flowable:string.text", XmlType.CDATA)),
    PAYLOAD_EXPRESSION(PropertyTypeDetails(PropertyType.PAYLOAD_EXPRESSION, "extensionElements.flowable:field?name=payloadExpression.flowable:expression.text", XmlType.CDATA)),
    RESULT_VARIABLE_CDATA(PropertyTypeDetails(PropertyType.RESULT_VARIABLE_CDATA, "extensionElements.flowable:field?name=resultVariable.flowable:string.text", XmlType.CDATA)),
    COMMAND(PropertyTypeDetails(PropertyType.COMMAND, "extensionElements.flowable:field?name=command.flowable:string.text", XmlType.CDATA)),
    ARG_1(PropertyTypeDetails(PropertyType.ARG_1, "extensionElements.flowable:field?name=arg1.flowable:string.text", XmlType.CDATA)),
    ARG_2(PropertyTypeDetails(PropertyType.ARG_2, "extensionElements.flowable:field?name=arg2.flowable:string.text", XmlType.CDATA)),
    ARG_3(PropertyTypeDetails(PropertyType.ARG_3, "extensionElements.flowable:field?name=arg3.flowable:string.text", XmlType.CDATA)),
    ARG_4(PropertyTypeDetails(PropertyType.ARG_4, "extensionElements.flowable:field?name=arg4.flowable:string.text", XmlType.CDATA)),
    ARG_5(PropertyTypeDetails(PropertyType.ARG_5, "extensionElements.flowable:field?name=arg5.flowable:string.text", XmlType.CDATA)),
    WAIT(PropertyTypeDetails(PropertyType.WAIT, "extensionElements.flowable:field?name=wait.flowable:string.text", XmlType.CDATA)),
    CLEAN_ENV(PropertyTypeDetails(PropertyType.CLEAN_ENV, "extensionElements.flowable:field?name=cleanEnv.flowable:string.text", XmlType.CDATA)),
    ERROR_CODE_VARIABLE(PropertyTypeDetails(PropertyType.ERROR_CODE_VARIABLE, "extensionElements.flowable:field?name=errorCodeVariable.flowable:string.text", XmlType.CDATA)),
    OUTPUT_VARIABLE(PropertyTypeDetails(PropertyType.OUTPUT_VARIABLE, "extensionElements.flowable:field?name=outputVariable.flowable:string.text", XmlType.CDATA)),
    DIRECTORY(PropertyTypeDetails(PropertyType.DIRECTORY, "extensionElements.flowable:field?name=directory.flowable:string.text", XmlType.CDATA)),
    FAILED_JOB_RETRY_CYCLE(PropertyTypeDetails(PropertyType.FAILED_JOB_RETRY_CYCLE, "extensionElements.flowable:failedJobRetryTimeCycle.text", XmlType.CDATA))
}

class FlowableParser : BaseBpmnParser() {

    private val mapper: XmlMapper = mapper()

    override fun parse(input: String): BpmnProcessObject {
        val dto = mapper.readValue<BpmnFile>(input)
        return toProcessObject(dto)
    }

    private fun toProcessObject(dto: BpmnFile): BpmnProcessObject {
        // TODO - Multi process support?
        markSubprocessesAndTransactionsThatHaveExternalDiagramAsCollapsed(dto.processes[0], dto.diagrams!!)
        val process = dto.processes[0].toElement()
        val diagrams = dto.diagrams!!.map { it.toElement() }

        return BpmnProcessObject(process, diagrams)
    }

    override fun modelNs(): NS {
        return NS("", "http://www.omg.org/spec/BPMN/20100524/MODEL")
    }

    override fun bpmndiNs(): NS {
        return NS("bpmdi", "http://www.omg.org/spec/BPMN/20100524/DI")
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
        return NS("flowable", "http://flowable.org/bpmn")
    }

    override fun changeElementType(node: Element, name: String, details: PropertyTypeDetails, value: String?) {
        if (FlowablePropertyTypeDetails.IS_TRANSACTIONAL_SUBPROCESS.details != details) {
            throw IllegalArgumentException("Can't change type for: ${details.javaClass.canonicalName}")
        }

        if (null == value || !value.toBoolean()) {
            node.qName = modelNs().named("subProcess")
        } else {
            node.qName = modelNs().named("transaction")
        }
    }

    override fun propertyTypeDetails(): List<PropertyTypeDetails> {
        return FlowablePropertyTypeDetails.values().map { it.details }
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
