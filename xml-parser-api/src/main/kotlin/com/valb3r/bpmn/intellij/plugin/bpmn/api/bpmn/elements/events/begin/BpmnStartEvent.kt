package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionFormProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnStartEventAlike

data class BpmnStartEvent(
    override val id: BpmnElementId,
    val name: String? = null,
    val documentation: String? = null,
    val asyncBefore: Boolean? = null,
    val asyncAfter: Boolean? = null,
    val formKey: String? = null,
    val formFieldValidation: Boolean? = null,
    val timerEventDefinition: BpmnTimerEventDefinition? = null,
    val signalEventDefinition: BpmnSignalEventDefinition? = null,
    val messageEventDefinition: BpmnMessageEventDefinition? = null,
    val errorEventDefinition: BpmnErrorEventDefinition? = null,
    val escalationEventDefinition: BpmnEscalationEventDefinition? = null,
    val conditionalEventDefinition: BpmnConditionalEventDefinition? = null,
    val incoming: List<String>? = null,
    val outgoing: List<String>? = null,
    /* BPMN engine specific extensions (intermediate storage) */
    val extensionElements: List<ExtensionElement>? = null,
    /* Flattened extensionElements, for explicitness - these are the target of binding */
    val formPropertiesExtension: List<ExtensionFormProperty>? = null,
    val executionListener: List<ExeсutionListener>? = null
) : WithBpmnId, BpmnStartEventAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}