package com.valb3r.bpmn.intellij.plugin.camunda.parser

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateLinkCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnComplexGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnExternalTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnSendTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.BpmnShapeObjectAdded
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.BaseBpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.NS
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.PropertyTypeDetails
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.XmlType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnFile
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.DiagramNode
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.ProcessNode
import org.dom4j.Element


enum class CamundaPropertyTypeDetails(val details: PropertyTypeDetails) {
    ID(PropertyTypeDetails(PropertyType.ID, "id", XmlType.ATTRIBUTE)),
    NAME(PropertyTypeDetails(PropertyType.NAME,"name", XmlType.ATTRIBUTE)),
    DOCUMENTATION(PropertyTypeDetails(PropertyType.DOCUMENTATION, "documentation.text", XmlType.CDATA, forceFirst = true)),
    IS_FOR_COMPENSATION(PropertyTypeDetails(PropertyType.IS_FOR_COMPENSATION, "isForCompensation", XmlType.ATTRIBUTE)),
    ASYNC_BEFORE(PropertyTypeDetails(PropertyType.ASYNC_BEFORE, "camunda:asyncBefore", XmlType.ATTRIBUTE)),
    ASYNC_AFTER(PropertyTypeDetails(PropertyType.ASYNC_AFTER, "camunda:asyncAfter", XmlType.ATTRIBUTE)),
    ASSIGNEE(PropertyTypeDetails(PropertyType.ASSIGNEE, "camunda:assignee", XmlType.ATTRIBUTE)),
    CANDIDATE_USERS(PropertyTypeDetails(PropertyType.CANDIDATE_USERS, "camunda:candidateUsers", XmlType.ATTRIBUTE)),
    CANDIDATE_GROUPS(PropertyTypeDetails(PropertyType.CANDIDATE_GROUPS, "camunda:candidateGroups", XmlType.ATTRIBUTE)),
    JOB_TOPIC(PropertyTypeDetails(PropertyType.JOB_TOPIC, "camunda:topic", XmlType.ATTRIBUTE)),
    TASK_PRIORITY(PropertyTypeDetails(PropertyType.TASK_PRIORITY, "camunda:taskPriority", XmlType.ATTRIBUTE)),
    CALLED_ELEM(PropertyTypeDetails(PropertyType.CALLED_ELEM, "calledElement", XmlType.ATTRIBUTE)),
    // Unsupported? CALLED_ELEM_TYPE(PropertyTypeDetails(PropertyType.CALLED_ELEM_TYPE, "camunda:calledElementType", XmlType.ATTRIBUTE)),
    // Unsupported? INHERIT_VARS(PropertyTypeDetails(PropertyType.INHERIT_VARS, "camunda:inheritVariables", XmlType.ATTRIBUTE)),
    // Unsupported? FALLBACK_TO_DEF_TENANT(PropertyTypeDetails(PropertyType.FALLBACK_TO_DEF_TENANT, "camunda:fallbackToDefaultTenant", XmlType.ATTRIBUTE)),
    EXCLUSIVE(PropertyTypeDetails(PropertyType.EXCLUSIVE,"camunda:exclusive", XmlType.ATTRIBUTE)),
    EXPRESSION(PropertyTypeDetails(PropertyType.EXPRESSION, "camunda:expression", XmlType.ATTRIBUTE)),
    DELEGATE_EXPRESSION(PropertyTypeDetails(PropertyType.DELEGATE_EXPRESSION, "camunda:delegateExpression", XmlType.ATTRIBUTE)),
    CLASS(PropertyTypeDetails(PropertyType.CLASS, "camunda:class", XmlType.ATTRIBUTE)),
    SKIP_EXPRESSION(PropertyTypeDetails(PropertyType.SKIP_EXPRESSION, "camunda:skipExpression", XmlType.ATTRIBUTE)),
    // Unsupported IS_TRIGGERABLE(PropertyTypeDetails(PropertyType.IS_TRIGGERABLE, "camunda:triggerable", XmlType.ATTRIBUTE)),
    DUE_DATE(PropertyTypeDetails(PropertyType.DUE_DATE, "camunda:dueDate", XmlType.ATTRIBUTE)),
    // Unsupported? CATEGORY(PropertyTypeDetails(PropertyType.CATEGORY, "camunda:category", XmlType.ATTRIBUTE)),
    FORM_KEY(PropertyTypeDetails(PropertyType.FORM_KEY, "camunda:formKey", XmlType.ATTRIBUTE)),
    // Unsupported? FORM_FIELD_VALIDATION(PropertyTypeDetails(PropertyType.FORM_FIELD_VALIDATION, "camunda:formFieldValidation", XmlType.ATTRIBUTE)),
    PRIORITY(PropertyTypeDetails(PropertyType.PRIORITY, "camunda:priority", XmlType.ATTRIBUTE)),
    SCRIPT(PropertyTypeDetails(PropertyType.SCRIPT, "script.text", XmlType.CDATA)),
    // Unsupported? SCRIPT_FORMAT(PropertyTypeDetails(PropertyType.SCRIPT_FORMAT, "scriptFormat", XmlType.ATTRIBUTE)),
    // Unsupported? AUTO_STORE_VARIABLES(PropertyTypeDetails(PropertyType.AUTO_STORE_VARIABLES, "camunda:autoStoreVariables", XmlType.ATTRIBUTE)),
    // Unsupported? RULE_VARIABLES_INPUT(PropertyTypeDetails(PropertyType.RULE_VARIABLES_INPUT, "camunda:ruleVariablesInput", XmlType.ATTRIBUTE)),
    // Unsupported? RULES(PropertyTypeDetails(PropertyType.RULES, "camunda:rules", XmlType.ATTRIBUTE)),
    RESULT_VARIABLE(PropertyTypeDetails(PropertyType.RESULT_VARIABLE, "camunda:resultVariable", XmlType.ATTRIBUTE)),
    // Unsupported? RESULT_VARIABLE_NAME(PropertyTypeDetails(PropertyType.RESULT_VARIABLE_NAME, "camunda:resultVariableName", XmlType.ATTRIBUTE)),
    // Unsupported? EXCLUDE(PropertyTypeDetails(PropertyType.EXCLUDE, "camunda:exclude", XmlType.ATTRIBUTE)),
    SOURCE_REF(PropertyTypeDetails(PropertyType.SOURCE_REF,"sourceRef", XmlType.ATTRIBUTE)),
    TARGET_REF(PropertyTypeDetails(PropertyType.TARGET_REF, "targetRef", XmlType.ATTRIBUTE)),
    BPMN_INCOMING(PropertyTypeDetails(PropertyType.BPMN_INCOMING, "incoming?\$=@.text", XmlType.CDATA)),
    BPMN_OUTGOING(PropertyTypeDetails(PropertyType.BPMN_OUTGOING, "outgoing?\$=@.text", XmlType.CDATA)),
    // Unsupported? ATTACHED_TO_REF(PropertyTypeDetails(PropertyType.ATTACHED_TO_REF, "attachedToRef", XmlType.ATTRIBUTE)),
    CONDITION_EXPR_VALUE(PropertyTypeDetails(PropertyType.CONDITION_EXPR_VALUE, "conditionExpression.text", XmlType.CDATA)),
    CONDITION_EXPR_TYPE(PropertyTypeDetails(PropertyType.CONDITION_EXPR_TYPE, "conditionExpression.xsi:type", XmlType.ATTRIBUTE)),
    COMPLETION_CONDITION(PropertyTypeDetails(PropertyType.COMPLETION_CONDITION, "completionCondition.text", XmlType.CDATA)),
    DEFAULT_FLOW(PropertyTypeDetails(PropertyType.DEFAULT_FLOW, "default", XmlType.ATTRIBUTE)),
    // Unsupported? IS_TRANSACTIONAL_SUBPROCESS(PropertyTypeDetails(PropertyType.IS_TRANSACTIONAL_SUBPROCESS, "transactionalSubprocess", XmlType.ELEMENT)),
    // Unsupported? IS_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE(PropertyTypeDetails(PropertyType.IS_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE, "camunda:useLocalScopeForResultVariable", XmlType.ATTRIBUTE)),
    // Unsupported? CAMEL_CONTEXT(PropertyTypeDetails(PropertyType.CAMEL_CONTEXT, "extensionElements.camunda:field?name=camelContext.camunda:string.text", XmlType.CDATA)),
    // Unsupported? DECISION_TABLE_REFERENCE_KEY(PropertyTypeDetails(PropertyType.DECISION_TABLE_REFERENCE_KEY, "extensionElements.camunda:field?name=decisionTableReferenceKey.camunda:string.text", XmlType.CDATA)),
    // Unsupported? DECISION_TASK_THROW_ERROR_ON_NO_HITS(PropertyTypeDetails(PropertyType.DECISION_TASK_THROW_ERROR_ON_NO_HITS, "extensionElements.camunda:field?name=decisionTaskThrowErrorOnNoHits.camunda:string.text", XmlType.CDATA)),
    // Unsupported? FALLBACK_TO_DEF_TENANT_CDATA(PropertyTypeDetails(PropertyType.FALLBACK_TO_DEF_TENANT_CDATA, "extensionElements.camunda:field?name=fallbackToDefaultTenant.camunda:string.text", XmlType.CDATA)),
    // Unsupported? REQUEST_METHOD(PropertyTypeDetails(PropertyType.REQUEST_METHOD, "extensionElements.camunda:field?name=requestMethod.camunda:string.text", XmlType.CDATA)),
    // Unsupported? REQUEST_URL(PropertyTypeDetails(PropertyType.REQUEST_URL, "extensionElements.camunda:field?name=requestUrl.camunda:string.text", XmlType.CDATA)),
    // Unsupported? REQUEST_HEADERS(PropertyTypeDetails(PropertyType.REQUEST_HEADERS, "extensionElements.camunda:field?name=requestHeaders.camunda:string.text", XmlType.CDATA)),
    // Unsupported? REQUEST_BODY(PropertyTypeDetails(PropertyType.REQUEST_BODY, "extensionElements.camunda:field?name=requestBody.camunda:string.text", XmlType.CDATA)),
    // Unsupported? REQUEST_BODY_ENCODING(PropertyTypeDetails(PropertyType.REQUEST_BODY_ENCODING, "extensionElements.camunda:field?name=requestBodyEncoding.camunda:string.text", XmlType.CDATA)),
    // Unsupported? REQUEST_TIMEOUT(PropertyTypeDetails(PropertyType.REQUEST_TIMEOUT, "extensionElements.camunda:field?name=requestTimeout.camunda:string.text", XmlType.CDATA)),
    // Unsupported? DISALLOW_REDIRECTS(PropertyTypeDetails(PropertyType.DISALLOW_REDIRECTS, "extensionElements.camunda:field?name=disallowRedirects.camunda:string.text", XmlType.CDATA)),
    // Unsupported? FAIL_STATUS_CODES(PropertyTypeDetails(PropertyType.FAIL_STATUS_CODES, "extensionElements.camunda:field?name=failStatusCodes.camunda:string.text", XmlType.CDATA)),
    // Unsupported? HANDLE_STATUS_CODES(PropertyTypeDetails(PropertyType.HANDLE_STATUS_CODES, "extensionElements.camunda:field?name=handleStatusCodes.camunda:string.text", XmlType.CDATA)),
    // Unsupported? RESPONSE_VARIABLE_NAME(PropertyTypeDetails(PropertyType.RESPONSE_VARIABLE_NAME, "extensionElements.camunda:field?name=responseVariableName.camunda:string.text", XmlType.CDATA)),
    // Unsupported? IGNORE_EXCEPTION(PropertyTypeDetails(PropertyType.IGNORE_EXCEPTION, "extensionElements.camunda:field?name=ignoreException.camunda:string.text", XmlType.CDATA)),
    // Unsupported? SAVE_REQUEST_VARIABLES(PropertyTypeDetails(PropertyType.SAVE_REQUEST_VARIABLES, "extensionElements.camunda:field?name=saveRequestVariables.camunda:string.text", XmlType.CDATA)),
    // Unsupported? SAVE_RESPONSE_PARAMETERS(PropertyTypeDetails(PropertyType.SAVE_RESPONSE_PARAMETERS, "extensionElements.camunda:field?name=saveResponseParameters.camunda:string.text", XmlType.CDATA)),
    // Unsupported? RESULT_VARIABLE_PREFIX(PropertyTypeDetails(PropertyType.RESULT_VARIABLE_PREFIX, "extensionElements.camunda:field?name=resultVariablePrefix.camunda:string.text", XmlType.CDATA)),
    // Unsupported? SAVE_RESPONSE_PARAMETERS_TRANSIENT(PropertyTypeDetails(PropertyType.SAVE_RESPONSE_PARAMETERS_TRANSIENT, "extensionElements.camunda:field?name=saveResponseParametersTransient.camunda:string.text", XmlType.CDATA)),
    // Unsupported? SAVE_RESPONSE_VARIABLE_AS_JSON(PropertyTypeDetails(PropertyType.SAVE_RESPONSE_VARIABLE_AS_JSON, "extensionElements.camunda:field?name=saveResponseVariableAsJson.camunda:string.text", XmlType.CDATA)),
    // Unsupported? HEADERS(PropertyTypeDetails(PropertyType.HEADERS, "extensionElements.camunda:field?name=headers.camunda:string.text", XmlType.CDATA)),
    // Unsupported? TO(PropertyTypeDetails(PropertyType.TO, "extensionElements.camunda:field?name=to.camunda:string.text", XmlType.CDATA)),
    // Unsupported? FROM(PropertyTypeDetails(PropertyType.FROM, "extensionElements.camunda:field?name=from.camunda:string.text", XmlType.CDATA)),
    // Unsupported? SUBJECT(PropertyTypeDetails(PropertyType.SUBJECT, "extensionElements.camunda:field?name=subject.camunda:string.text", XmlType.CDATA)),
    // Unsupported? CC(PropertyTypeDetails(PropertyType.CC, "extensionElements.camunda:field?name=cc.camunda:string.text", XmlType.CDATA)),
    // Unsupported? BCC(PropertyTypeDetails(PropertyType.BCC, "extensionElements.camunda:field?name=bcc.camunda:string.text", XmlType.CDATA)),
    // Unsupported? TEXT(PropertyTypeDetails(PropertyType.TEXT, "extensionElements.camunda:field?name=text.camunda:string.text", XmlType.CDATA)),
    // Unsupported? HTML(PropertyTypeDetails(PropertyType.HTML, "extensionElements.camunda:field?name=html.camunda:string.text", XmlType.CDATA)),
    // Unsupported? CHARSET(PropertyTypeDetails(PropertyType.CHARSET, "extensionElements.camunda:field?name=charset.camunda:string.text", XmlType.CDATA)),
    // Unsupported? ENDPOINT_URL(PropertyTypeDetails(PropertyType.ENDPOINT_URL, "extensionElements.camunda:field?name=endpointUrl.camunda:string.text", XmlType.CDATA)),
    // Unsupported? LANGUAGE(PropertyTypeDetails(PropertyType.LANGUAGE, "extensionElements.camunda:field?name=language.camunda:string.text", XmlType.CDATA)),
    // Unsupported? PAYLOAD_EXPRESSION(PropertyTypeDetails(PropertyType.PAYLOAD_EXPRESSION, "extensionElements.camunda:field?name=payloadExpression.camunda:expression.text", XmlType.CDATA)),
    // Unsupported? RESULT_VARIABLE_CDATA(PropertyTypeDetails(PropertyType.RESULT_VARIABLE_CDATA, "extensionElements.camunda:field?name=resultVariable.camunda:string.text", XmlType.CDATA)),
    // Unsupported? COMMAND(PropertyTypeDetails(PropertyType.COMMAND, "extensionElements.camunda:field?name=command.camunda:string.text", XmlType.CDATA)),
    // Unsupported? ARG_1(PropertyTypeDetails(PropertyType.ARG_1, "extensionElements.camunda:field?name=arg1.camunda:string.text", XmlType.CDATA)),
    // Unsupported? ARG_2(PropertyTypeDetails(PropertyType.ARG_2, "extensionElements.camunda:field?name=arg2.camunda:string.text", XmlType.CDATA)),
    // Unsupported? ARG_3(PropertyTypeDetails(PropertyType.ARG_3, "extensionElements.camunda:field?name=arg3.camunda:string.text", XmlType.CDATA)),
    // Unsupported? ARG_4(PropertyTypeDetails(PropertyType.ARG_4, "extensionElements.camunda:field?name=arg4.camunda:string.text", XmlType.CDATA)),
    // Unsupported? ARG_5(PropertyTypeDetails(PropertyType.ARG_5, "extensionElements.camunda:field?name=arg5.camunda:string.text", XmlType.CDATA)),
    // Unsupported? WAIT(PropertyTypeDetails(PropertyType.WAIT, "extensionElements.camunda:field?name=wait.camunda:string.text", XmlType.CDATA)),
    // Unsupported? CLEAN_ENV(PropertyTypeDetails(PropertyType.CLEAN_ENV, "extensionElements.camunda:field?name=cleanEnv.camunda:string.text", XmlType.CDATA)),
    // Unsupported? ERROR_CODE_VARIABLE(PropertyTypeDetails(PropertyType.ERROR_CODE_VARIABLE, "extensionElements.camunda:field?name=errorCodeVariable.camunda:string.text", XmlType.CDATA)),
    // Unsupported? OUTPUT_VARIABLE(PropertyTypeDetails(PropertyType.OUTPUT_VARIABLE, "extensionElements.camunda:field?name=outputVariable.camunda:string.text", XmlType.CDATA)),
    // Unsupported? DIRECTORY(PropertyTypeDetails(PropertyType.DIRECTORY, "extensionElements.camunda:field?name=directory.camunda:string.text", XmlType.CDATA)),
    FAILED_JOB_RETRY_CYCLE(PropertyTypeDetails(PropertyType.FAILED_JOB_RETRY_CYCLE, "extensionElements.camunda:failedJobRetryTimeCycle.text", XmlType.CDATA)),
    FIELD_NAME(PropertyTypeDetails(PropertyType.FIELD_NAME, "extensionElements.camunda:field?name=@.name", XmlType.ATTRIBUTE)),
    FIELD_EXPRESSION(PropertyTypeDetails(PropertyType.FIELD_EXPRESSION, "extensionElements.camunda:field?name=@.camunda:expression.text", XmlType.CDATA)),
    FIELD_STRING(PropertyTypeDetails(PropertyType.FIELD_STRING, "extensionElements.camunda:field?name=@.camunda:string.text", XmlType.CDATA)),
    FORM_PROPERTY_ID(PropertyTypeDetails(PropertyType.FORM_PROPERTY_ID, "extensionElements.camunda:formData.camunda:formField?id=@.id", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_NAME(PropertyTypeDetails(PropertyType.FORM_PROPERTY_NAME, "extensionElements.camunda:formData.camunda:formField?id=@.label", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_TYPE(PropertyTypeDetails(PropertyType.FORM_PROPERTY_TYPE, "extensionElements.camunda:formData.camunda:formField?id=@.type", XmlType.ATTRIBUTE)),
    // Unsupported FORM_PROPERTY_VARIABLE(PropertyTypeDetails(PropertyType.FORM_PROPERTY_VARIABLE, "extensionElements.camunda:formProperty?id=@.variable", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_DEFAULT(PropertyTypeDetails(PropertyType.FORM_PROPERTY_DEFAULT, "extensionElements.camunda:formData.camunda:formField?id=@.defaultValue", XmlType.ATTRIBUTE)),
    // Unsupported FORM_PROPERTY_EXPRESSION(PropertyTypeDetails(PropertyType.FORM_PROPERTY_EXPRESSION, "extensionElements.camunda:formProperty?id=@.expression", XmlType.ATTRIBUTE)),
    // Unsupported FORM_PROPERTY_DATE_PATTERN(PropertyTypeDetails(PropertyType.FORM_PROPERTY_DATE_PATTERN, "extensionElements.camunda:formProperty?id=@.datePattern", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_VALUE_ID(PropertyTypeDetails(PropertyType.FORM_PROPERTY_VALUE_ID, "extensionElements.camunda:formData.camunda:formField?id=@.camunda:properties.camunda:property?id=@.id", XmlType.ATTRIBUTE)),
    FORM_PROPERTY_VALUE_NAME(PropertyTypeDetails(PropertyType.FORM_PROPERTY_VALUE_NAME, "extensionElements.camunda:formData.camunda:formField?id=@.camunda:properties.camunda:property?id=@.value", XmlType.ATTRIBUTE))
}

class CamundaParser : BaseBpmnParser() {

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
        return NS("bpmn", "http://www.omg.org/spec/BPMN/20100524/MODEL")
    }

    override fun bpmndiNs(): NS {
        return NS("bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI")
    }

    override fun omgdcNs(): NS {
        return NS("dc", "http://www.omg.org/spec/DD/20100524/DC")
    }

    override fun omgdiNs(): NS {
        return NS("di", "http://www.omg.org/spec/DD/20100524/DI")
    }

    override fun xsiNs(): NS {
        return NS("xsi", "http://www.w3.org/2001/XMLSchema-instance")
    }

    override fun engineNs(): NS {
        return NS("camunda", "http://camunda.org/schema/1.0/bpmn")
    }

    override fun changeElementType(node: Element, name: String, details: PropertyTypeDetails, value: String?) {
//        if (CamundaPropertyTypeDetails.IS_TRANSACTIONAL_SUBPROCESS.details != details) {
//            throw IllegalArgumentException("Can't change type for: ${details.javaClass.canonicalName}")
//        }

        if (null == value || !value.toBoolean()) {
            node.qName = modelNs().named("subProcess")
        } else {
            node.qName = modelNs().named("transaction")
        }
    }

    override fun propertyTypeDetails(): List<PropertyTypeDetails> {
        return CamundaPropertyTypeDetails.values().map { it.details }
    }

    override fun createBpmnObject(update: BpmnShapeObjectAdded, diagramParent: Element): Element? {
        return when (update.bpmnObject.element) {
            is BpmnTask -> diagramParent.addElement(modelNs().named("task"))
            is BpmnSendTask -> diagramParent.addElement(modelNs().named("sendTask"))
            is BpmnComplexGateway -> diagramParent.addElement(modelNs().named("complexGateway"))
            is BpmnIntermediateLinkCatchingEvent -> createIntermediateCatchEventWithType(diagramParent, "linkEventDefinition")
            else -> super.createBpmnObject(update, diagramParent)
        }
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
