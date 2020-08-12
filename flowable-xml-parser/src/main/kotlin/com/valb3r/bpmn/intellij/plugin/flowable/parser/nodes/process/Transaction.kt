package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionalSubProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.ProcessBody
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

class Transaction: BpmnMappable<BpmnTransactionalSubProcess>, ProcessBody() {

    @JacksonXmlProperty(isAttribute = true) var id: String? = null
    @JacksonXmlProperty(isAttribute = true) var name: String? = null
    var documentation: String? = null
    @JacksonXmlProperty(isAttribute = true) var async: Boolean? = null
    @JacksonXmlProperty(isAttribute = true) var exclusive: Boolean? = null
    @JsonIgnore var hasExternalDiagram: Boolean = false

    override fun toElement(): BpmnTransactionalSubProcess {
        return Mappers.getMapper(TransactionMapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface TransactionMapping {

        @Mapping(target = "transactionalSubprocess", constant = "true")
        fun convertToDto(input: Transaction) : BpmnTransactionalSubProcess
    }
}