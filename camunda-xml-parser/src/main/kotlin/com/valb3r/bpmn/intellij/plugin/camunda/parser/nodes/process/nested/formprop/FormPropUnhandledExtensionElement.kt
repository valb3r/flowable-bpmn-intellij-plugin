package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(`as` = FormPropUnhandledExtensionElement::class)
class FormPropUnhandledExtensionElement : FormPropExtensionElement()