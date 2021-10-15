package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.formprop

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(`as` = FormPropUnhandledExtensionElement::class)
class FormPropUnhandledExtensionElement : FormPropExtensionElement()