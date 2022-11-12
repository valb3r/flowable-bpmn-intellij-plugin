package com.valb3r.bpmn.intellij.plugin.bpmn.api.info

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnSendEventTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import kotlin.reflect.KClass
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.EXPRESSION as T_EXPRESSION

enum class PropertyType(
    val id: String,
    val caption: String,
    val valueType: PropertyValueType,
    val path: String = id,
    val cascades: Boolean = false,
    val updatedBy: PropertyType? = null,
    val updateOrder: Int = 0,
    val elementUpdateChangesClass: Boolean = false,
    val defaultValueIfNull: Any? = null,
    val group: List<FunctionalGroupType>? = null,
    val indexInGroupArrayName: String? = null,
    val indexCascades: CascadeGroup = CascadeGroup.NONE,
    val explicitIndexCascades: List<String>? = null,
    val removeEnclosingNodeIfNullOrEmpty: Boolean = false,
    val hideIfNullOrEmpty: Boolean = false,
    val visible: Boolean = true,
    val multiline: Boolean = false,
    val positionInGroup: Int = 65534, // Explicit order indicator - where to place control in UI
    val setForSelect: Set<String>? = null,
    val isUsedOnlyBy: Set<KClass<out WithBpmnId>> = setOf() // In case of empty list us used by any class
) {
    ID("id", "ID", STRING, "id.id", true, null, 1000, explicitIndexCascades = listOf("BPMN_INCOMING", "BPMN_OUTGOING")), // ID should fire last
    NAME("name", "Name", STRING),
    DOCUMENTATION("documentation", "Documentation", STRING, multiline = true),
    IS_FOR_COMPENSATION("forCompensation", "Is for compensation", BOOLEAN),
    ASYNC("async", "Asynchronous", BOOLEAN),
    ASYNC_BEFORE("asyncBefore", "Asynchronous Before", BOOLEAN),
    ASYNC_AFTER("asyncAfter", "Asynchronous After", BOOLEAN),
    ASSIGNEE("assignee", "Assignee", STRING),
    CANDIDATE_USERS("candidateUsers", "Candidate Users", STRING),
    CANDIDATE_GROUPS("candidateGroups", "Candidate Groups", STRING),
    JOB_TOPIC("jobTopic", "Job topic", STRING),
    CALLED_ELEM("calledElement", "Called element", T_EXPRESSION),
    CALLED_ELEM_TYPE("calledElementType", "Called element type", STRING),
    INHERIT_VARS("inheritVariables", "Inherit parent variables", BOOLEAN),
    FALLBACK_TO_DEF_TENANT("fallbackToDefaultTenant", "Fallback to default tenant", BOOLEAN),
    EXCLUSIVE("exclusive", "Exclusive", BOOLEAN, defaultValueIfNull = true),
    EXPRESSION("expression", "Expression", T_EXPRESSION),
    DELEGATE_EXPRESSION("delegateExpression", "Delegate expression", T_EXPRESSION),
    CLASS("clazz", "Class", PropertyValueType.CLASS),
    SKIP_EXPRESSION("skipExpression", "Skip expression", T_EXPRESSION),
    IS_TRIGGERABLE("triggerable", "Is activity triggerable?", BOOLEAN),
    DUE_DATE("dueDate", "Due date", STRING),
    CATEGORY("category", "Category", STRING),
    FORM_KEY("formKey", "Form key", STRING),
    FORM_FIELD_VALIDATION("formFieldValidation", "Form field validation", BOOLEAN),
    PRIORITY("priority", "Priority", STRING),
    TASK_PRIORITY("taskPriority", "Task priority", STRING),
    SCRIPT("scriptBody", "Script body", STRING, multiline = true),
    SCRIPT_FORMAT("scriptFormat", "Script format", STRING),
    AUTO_STORE_VARIABLES("autoStoreVariables", "Auto store variables", BOOLEAN),
    RULE_VARIABLES_INPUT("ruleVariablesInput", "Rule variables input", STRING),
    RULES("rules", "Rules", STRING),
    RESULT_VARIABLE("resultVariable", "Result variable", STRING),
    RESULT_VARIABLE_NAME("resultVariableName", "Result variable name", STRING),
    EXCLUDE("exclude", "Exclude", BOOLEAN),
    SOURCE_REF("sourceRef", "Source reference", STRING, "sourceRef", false, ID),
    TARGET_REF("targetRef", "Target reference", STRING, "targetRef", false, ID),
    BPMN_INCOMING("incoming", "Incoming reference", STRING, "incoming", false, ID, visible = false, indexInGroupArrayName = "@", removeEnclosingNodeIfNullOrEmpty = true),
    BPMN_OUTGOING("outgoing", "Outgoing reference", STRING, "outgoing", false, ID, visible = false, indexInGroupArrayName = "@", removeEnclosingNodeIfNullOrEmpty = true),
    ATTACHED_TO_REF("attachedToRef", "Attached to", STRING, "attachedToRef.id", false, ID),
    CONDITION_EXPR_VALUE("conditionExpression.text", "Condition expression", T_EXPRESSION, "conditionExpression.text"),
    CONDITION_EXPR_TYPE("conditionExpression.type", "Condition expression type", STRING, "conditionExpression.type"),
    COMPLETION_CONDITION("completionCondition.condition", "Completion condition", T_EXPRESSION, "completionCondition.condition"),
    DEFAULT_FLOW("defaultElement", "Default flow element", ATTACHED_SEQUENCE_SELECT, "defaultElement", false, ID),
    IS_TRANSACTIONAL_SUBPROCESS("transactionalSubprocess", "Is transactional subprocess", BOOLEAN, "transactionalSubprocess", elementUpdateChangesClass = true),
    IS_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE("useLocalScopeForResultVariable", "Use local scope for result varaible", BOOLEAN),
    CAMEL_CONTEXT("camelContext", "Camel context", STRING),
    DECISION_TABLE_REFERENCE_KEY("decisionTableReferenceKey", "Decision table reference key", STRING),
    DECISION_TASK_THROW_ERROR_ON_NO_HITS("decisionTaskThrowErrorOnNoHits", "Throw error if no rule hit", BOOLEAN),
    FALLBACK_TO_DEF_TENANT_CDATA("fallbackToDefaultTenantCdata", "Fallback to default tenant", BOOLEAN),
    REQUEST_METHOD("requestMethod", "Request method", STRING),
    REQUEST_URL("requestUrl", "Request URL", STRING),
    REQUEST_HEADERS("requestHeaders", "Request headers", STRING),
    REQUEST_BODY("requestBody", "Request body", STRING, multiline = true),
    REQUEST_BODY_ENCODING("requestBodyEncoding", "Request encoding", STRING),
    REQUEST_TIMEOUT("requestTimeout", "Request timeout", STRING),
    DISALLOW_REDIRECTS("disallowRedirects", "Disallow redirects", BOOLEAN),
    FAIL_STATUS_CODES("failStatusCodes", "Fail status codes", STRING),
    HANDLE_STATUS_CODES("handleStatusCodes", "Handle status codes", STRING),
    RESPONSE_VARIABLE_NAME("responseVariableName", "Response variable name", STRING),
    IGNORE_EXCEPTION("ignoreException", "Ignore exception", STRING),
    SAVE_REQUEST_VARIABLES("saveRequestVariables", "Save request variables to", STRING),
    SAVE_RESPONSE_PARAMETERS("saveResponseParameters", "Save response,status,headers to", STRING),
    RESULT_VARIABLE_PREFIX("resultVariablePrefix", "Result variable prefix", STRING),
    SAVE_RESPONSE_PARAMETERS_TRANSIENT("saveResponseParametersTransient", "Save response as transient variable", STRING),
    SAVE_RESPONSE_VARIABLE_AS_JSON("saveResponseVariableAsJson", "Save response as json", STRING),
    HEADERS("headers", "Headers", STRING, multiline = true),
    TO("to", "To", STRING),
    FROM("from", "From", STRING),
    SUBJECT("subject", "Subject", STRING),
    CC("cc", "CC", STRING),
    BCC("bcc", "BCC", STRING),
    TEXT("text", "Text", STRING, multiline = true),
    HTML("html", "Html", STRING, multiline = true),
    CHARSET("charset", "Charset", STRING),
    ENDPOINT_URL("endpointUrl", "Endpoint url", STRING),
    LANGUAGE("language", "Language", STRING),
    PAYLOAD_EXPRESSION("payloadExpression", "Payload expression", STRING),
    RESULT_VARIABLE_CDATA("resultVariableCdata", "Result variable", STRING),
    COMMAND("command", "Command to run", STRING),
    ARG_1("arg1", "Argument 1", STRING),
    ARG_2("arg2", "Argument 2", STRING),
    ARG_3("arg3", "Argument 3", STRING),
    ARG_4("arg4", "Argument 4", STRING),
    ARG_5("arg5", "Argument 5", STRING),
    WAIT("wait", "Wait", STRING),
    CLEAN_ENV("cleanEnv", "Clean environment", STRING),
    ERROR_CODE_VARIABLE("errorCodeVariable", "Error code variable", STRING),
    OUTPUT_VARIABLE("outputVariable", "Output variable", STRING),
    DIRECTORY("directory", "Working directory", STRING),
    FAILED_JOB_RETRY_CYCLE("failedJobRetryTimeCycle", "Failed job retry cycle", STRING),
    FIELD_NAME("fieldsExtension.@name", "Field name", STRING, group = listOf(FunctionalGroupType.ADD_FIELD), indexInGroupArrayName = "name", updateOrder = 100, indexCascades = CascadeGroup.PARENTS_CASCADE, removeEnclosingNodeIfNullOrEmpty = true, hideIfNullOrEmpty = true), // Is sub-id
    FIELD_EXPRESSION("fieldsExtension.@expression", "Expression", T_EXPRESSION, group = listOf(FunctionalGroupType.ADD_FIELD), indexInGroupArrayName = "name", removeEnclosingNodeIfNullOrEmpty = true),
    FIELD_STRING("fieldsExtension.@string", "String value", STRING, group = listOf(FunctionalGroupType.ADD_FIELD), indexInGroupArrayName = "name", removeEnclosingNodeIfNullOrEmpty = true),
    FORM_PROPERTY_ID("formPropertiesExtension.@id", "Form property ID", STRING, group = listOf(FunctionalGroupType.ADD_FORM_PROPERTY), indexInGroupArrayName = "id", updateOrder = 100, indexCascades = CascadeGroup.PARENTS_CASCADE, removeEnclosingNodeIfNullOrEmpty = true, hideIfNullOrEmpty = true), // Is sub-id
    FORM_PROPERTY_NAME("formPropertiesExtension.@name", "Property name", STRING, group = listOf(FunctionalGroupType.ADD_FORM_PROPERTY), indexInGroupArrayName = "id"),
    FORM_PROPERTY_TYPE("formPropertiesExtension.@type", "Type", STRING, group = listOf(FunctionalGroupType.ADD_FORM_PROPERTY), indexInGroupArrayName = "id"),
    FORM_PROPERTY_VARIABLE("formPropertiesExtension.@variable", "Variable", STRING, group = listOf(FunctionalGroupType.ADD_FORM_PROPERTY), indexInGroupArrayName = "id"),
    FORM_PROPERTY_DEFAULT("formPropertiesExtension.@default", "Default value", STRING, group = listOf(FunctionalGroupType.ADD_FORM_PROPERTY), indexInGroupArrayName = "id"),
    FORM_PROPERTY_EXPRESSION("formPropertiesExtension.@expression", "Expression", T_EXPRESSION, group = listOf(FunctionalGroupType.ADD_FORM_PROPERTY), indexInGroupArrayName = "id"),
    FORM_PROPERTY_DATE_PATTERN("formPropertiesExtension.@datePattern", "Date pattern", STRING, group = listOf(FunctionalGroupType.ADD_FORM_PROPERTY), indexInGroupArrayName = "id"),
    FORM_PROPERTY_VALUE_ID("formPropertiesExtension.@value.@id", "Value ID", STRING, group = listOf(FunctionalGroupType.ADD_FORM_PROPERTY, FunctionalGroupType.ADD_FORM_PROPERTY_VALUE), indexInGroupArrayName = "id.id", updateOrder = 100, indexCascades = CascadeGroup.PARENTS_CASCADE, removeEnclosingNodeIfNullOrEmpty = true, hideIfNullOrEmpty = true),  // Is sub-id
    FORM_PROPERTY_VALUE_NAME("formPropertiesExtension.@value.@name", "Value name", STRING, group = listOf(FunctionalGroupType.ADD_FORM_PROPERTY, FunctionalGroupType.ADD_FORM_PROPERTY_VALUE), indexInGroupArrayName = "id.id"),
    EVENT_TYPE("eventExtensionElements.@eventType", "Event type", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), updateOrder = 100, removeEnclosingNodeIfNullOrEmpty = true, hideIfNullOrEmpty = false, indexInGroupArrayName = "eventType", indexCascades = CascadeGroup.FLAT, positionInGroup = 1),
    EVENT_NAME("eventExtensionElements.@eventName", "Event name", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    TRIGGER_EVENT_TYPE("eventExtensionElements.@triggerEventType", "Trigger event key", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    CHANNEL_KEY("eventExtensionElements.@channelKey" , "Channel key", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    CHANNEL_NAME("eventExtensionElements.@channelName" , "Channel name", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    CHANNEL_DESTINATION("eventExtensionElements.@channelDestination" , "Channel destination", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    CHANNEL_TYPE("eventExtensionElements.@channelType" ,"Channel type", LIST_SELECT, setForSelect = setOf("", "jms", "kafka", "rabbitmq"), group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    TRIGGER_EVENT_NAME("eventExtensionElements.@triggerEventName" , "Trigger event name", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    TRIGGER_CHANNEL_KEY("eventExtensionElements.@triggerChannelKey" , "Trigger channel key", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    TRIGGER_CHANNEL_NAME("eventExtensionElements.@triggerChannelName" , "Trigger channel name", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    TRIGGER_CHANNEL_DESTINATION("eventExtensionElements.@triggerChannelDestination" , "Trigger channel destination", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    TRIGGER_CHANNEL_TYPE("eventExtensionElements.@triggerChannelType" ,"Trigger channel type", LIST_SELECT, setForSelect = setOf("", "jms", "kafka", "rabbitmq"), group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    EVENT_KEY_FIXED_VALUE("eventExtensionElements.@keyDetectionValue" , "Event key fixed value", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    FIXED_VALUE("eventExtensionElements.@keyDetectionType" ,"", STRING, group = listOf(FunctionalGroupType.ADD_EVENT), indexInGroupArrayName = "eventType", removeEnclosingNodeIfNullOrEmpty = true),
    MAPPING_PAYLOAD_TO_EVENT_VARIABLE_NAME("extensionElementsMappingPayloadToEvent.@source", "Variable name", STRING, isUsedOnlyBy = setOf(BpmnSendEventTask::class), group = listOf(FunctionalGroupType.MAPPING_PAYLOAD_TO), indexInGroupArrayName = "source",  updateOrder = 100, removeEnclosingNodeIfNullOrEmpty = true, hideIfNullOrEmpty = true, indexCascades = CascadeGroup.PARENTS_CASCADE, positionInGroup = 1),
    MAPPING_PAYLOAD_TO_EVENT_PROPERTY_NAME("extensionElementsMappingPayloadToEvent.@target", "Event property name", STRING, isUsedOnlyBy = setOf(BpmnSendEventTask::class), group = listOf(FunctionalGroupType.MAPPING_PAYLOAD_TO), indexInGroupArrayName = "source"),
    MAPPING_PAYLOAD_TO_EVENT_TYPE("extensionElementsMappingPayloadToEvent.@type", "Type", LIST_SELECT, setForSelect = setOf("","string", "integer", "double", "boolean"), isUsedOnlyBy = setOf(BpmnSendEventTask::class), group = listOf(FunctionalGroupType.MAPPING_PAYLOAD_TO), indexInGroupArrayName = "source"),
    MAPPING_PAYLOAD_FROM_EVENT_VARIABLE_NAME("extensionElementsMappingPayloadFromEvent.@source", "Variable name", STRING, group = listOf(FunctionalGroupType.MAPPING_PAYLOAD_FROM), isUsedOnlyBy = setOf(BpmnSendEventTask::class), indexInGroupArrayName = "source",  updateOrder = 100, indexCascades = CascadeGroup.PARENTS_CASCADE, removeEnclosingNodeIfNullOrEmpty = true, hideIfNullOrEmpty = true, positionInGroup = 1),
    MAPPING_PAYLOAD_FROM_EVENT_PROPERTY_NAME("extensionElementsMappingPayloadFromEvent.@target", "Event property name", STRING, group = listOf(FunctionalGroupType.MAPPING_PAYLOAD_FROM), isUsedOnlyBy = setOf(BpmnSendEventTask::class), indexInGroupArrayName = "source"),
    MAPPING_PAYLOAD_FROM_EVENT_TYPE("extensionElementsMappingPayloadFromEvent.@type", "Type", LIST_SELECT, setForSelect = setOf("","string", "integer", "double", "boolean"), isUsedOnlyBy = setOf(BpmnSendEventTask::class), group = listOf(FunctionalGroupType.MAPPING_PAYLOAD_FROM), indexInGroupArrayName = "source"),
    EXECUTION_LISTENER_CLASS("executionListener.@clazz", "Class", STRING, group = listOf(FunctionalGroupType.EXECUTION_LISTENER), indexInGroupArrayName = "clazz", updateOrder = 100, indexCascades = CascadeGroup.PARENTS_CASCADE, removeEnclosingNodeIfNullOrEmpty = true, hideIfNullOrEmpty = true),
    EXECUTION_LISTENER_EVENT("executionListener.@event", "Event", LIST_SELECT, setForSelect = setOf("","start", "end", "take"), group = listOf(FunctionalGroupType.EXECUTION_LISTENER), indexInGroupArrayName = "clazz"),
    EXECUTION_LISTENER_FIELD_NAME("executionListener.@fields.@name", "Name", STRING, group = listOf(FunctionalGroupType.EXECUTION_LISTENER, FunctionalGroupType.EXECUTION_LISTENER_FILED), indexInGroupArrayName = "clazz.name", indexCascades = CascadeGroup.PARENTS_CASCADE, removeEnclosingNodeIfNullOrEmpty = true, updateOrder = 95),
    EXECUTION_LISTENER_FIELD_STRING("executionListener.@fields.@string", "String", STRING, group = listOf(FunctionalGroupType.EXECUTION_LISTENER, FunctionalGroupType.EXECUTION_LISTENER_FILED), indexInGroupArrayName = "clazz.name", updateOrder = 90);

    fun isNestedProperty(): Boolean {
        return (group?.size ?: 0) > 1
    }
}

val defaultXmlNestedValues: List<DefaultXmlNestedValue> = listOf(
    DefaultXmlNestedValue(PropertyType.EVENT_KEY_FIXED_VALUE, PropertyType.FIXED_VALUE, "fixedValue")
)
data class DefaultXmlNestedValue(val headProp: PropertyType, val dependProp: PropertyType, val valueDependProp: String)
data class NewElem(val propertyType: String, val valuePattern: String = "", val uiOnlyaddedIndex: List<String> = emptyList())

enum class CascadeGroup { 
    PARENTS_CASCADE, NONE, FLAT
}
enum class FunctionalGroupType(val groupCaption: String, val actionResult: NewElem, val actionUiOnlyResult: List<NewElem> = listOf(), val createExpansionButton: Boolean = true, val actionCaption: String = "",) {
    ADD_FIELD("Fields", actionCaption = "Add field", actionResult = NewElem("FIELD_NAME", "Field %d"), actionUiOnlyResult = listOf(NewElem("FIELD_EXPRESSION", ""), NewElem("FIELD_STRING", ""))),
    ADD_FORM_PROPERTY("Form properties", actionCaption = "Add property", actionResult = NewElem("FORM_PROPERTY_ID", "Property %d"),
        actionUiOnlyResult = listOf(
            NewElem("FORM_PROPERTY_NAME", ""),
            NewElem("FORM_PROPERTY_TYPE", ""),
            NewElem("FORM_PROPERTY_VARIABLE", ""),
            NewElem("FORM_PROPERTY_DEFAULT", ""),
            NewElem("FORM_PROPERTY_EXPRESSION", ""),
            NewElem("FORM_PROPERTY_DATE_PATTERN", ""),
            NewElem("FORM_PROPERTY_VALUE_ID", "", uiOnlyaddedIndex = listOf("")),
            NewElem("FORM_PROPERTY_VALUE_NAME", "", uiOnlyaddedIndex = listOf(""))
        )
    ),
    ADD_FORM_PROPERTY_VALUE("Form property value", actionCaption = "Add value", actionResult = NewElem("FORM_PROPERTY_VALUE_ID", "Property value %d"),
        actionUiOnlyResult = listOf(
            NewElem("FORM_PROPERTY_VALUE_NAME", "")
        )
    ),
    ADD_EVENT("Event fields", createExpansionButton = false, actionResult = NewElem("EVENT_TYPE", ""),
        actionUiOnlyResult = listOf(
            NewElem("EVENT_NAME", ""),
            NewElem("TRIGGER_EVENT_TYPE", ""),
            NewElem("CHANEL_KEY", ""),
            NewElem("CHANNEL_NAME", ""),
            NewElem("CHANNEL_DESTINATION", ""),
            NewElem("TRIGGER_EVENT_NAME", ""),
            NewElem("TRIGGER_CHANNEL_KEY", ""),
            NewElem("TRIGGER_CHANNEL_NAME", ""),
            NewElem("TRIGGER_CHANNEL_DESTINATION", ""),
            NewElem("EVENT_KEY_FIXED_VALUE", ""),
        )
    ),
    MAPPING_PAYLOAD_TO("Mapping to event payload", actionCaption = "Add mapping payload", actionResult = NewElem("MAPPING_PAYLOAD_TO_EVENT_VARIABLE_NAME", "Name %d"),
        actionUiOnlyResult = listOf(
            NewElem("MAPPING_PAYLOAD_TO_EVENT_PROPERTY_NAME", ""),
            NewElem("MAPPING_PAYLOAD_TO_EVENT_TYPE", ""),
        )
    ),
    MAPPING_PAYLOAD_FROM("Mapping from event payload", actionCaption = "Add mapping payload", actionResult = NewElem("MAPPING_PAYLOAD_FROM_EVENT_VARIABLE_NAME", "Name %d"),
        actionUiOnlyResult = listOf(
            NewElem("MAPPING_PAYLOAD_FROM_EVENT_PROPERTY_NAME", ""),
            NewElem("MAPPING_PAYLOAD_FROM_EVENT_TYPE", ""),
        )
    ),
    EXECUTION_LISTENER("Execution listeners", actionCaption = "Add execution listeners", actionResult = NewElem("EXECUTION_LISTENER_CLASS", "Class %d"),
        actionUiOnlyResult = listOf(
            NewElem("EXECUTION_LISTENER_EVENT", ""),
            NewElem("EXECUTION_LISTENER_FIELD_NAME", "", uiOnlyaddedIndex = listOf("")),
            NewElem("EXECUTION_LISTENER_FIELD_STRING", "", uiOnlyaddedIndex = listOf(""))
        )
    ),
    EXECUTION_LISTENER_FILED("Fields", actionCaption = "Add fields listener", actionResult = NewElem("EXECUTION_LISTENER_FIELD_NAME", "Name %d"),
        actionUiOnlyResult = listOf(
            NewElem("EXECUTION_LISTENER_FIELD_STRING", ""),
        )
    )
}
