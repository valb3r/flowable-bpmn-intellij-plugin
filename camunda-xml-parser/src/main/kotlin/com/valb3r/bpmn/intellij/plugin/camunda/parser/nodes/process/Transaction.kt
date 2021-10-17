package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionCollapsedSubprocess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionalSubProcess
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.ProcessBody
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

class Transaction: BpmnMappable<BpmnTransactionalSubProcess>, ProcessBody() {

    @JacksonXmlProperty(isAttribute = true) var id: String? = null
    @JacksonXmlProperty(isAttribute = true) var name: String? = null
    var documentation: String? = null
    @JacksonXmlProperty(isAttribute = true) val asyncBefore: Boolean? = null
    @JacksonXmlProperty(isAttribute = true) val asyncAfter: Boolean? = null
    @JacksonXmlProperty(isAttribute = true) var exclusive: Boolean? = null
    @JsonIgnore var hasExternalDiagram: Boolean = false

    override fun toElement(): BpmnTransactionalSubProcess {
        return Mappers.getMapper(TransactionMapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    abstract class TransactionMapping {  // Default methods of Kotlin interfaces are not understood by MapStruct

        fun mapNonCollapsed(input: List<Transaction>?) : List<BpmnTransactionalSubProcess>? {
            return input?.filter { !it.hasExternalDiagram }?.map { convertToDto(it) }
        }

        fun mapCollapsed(input: List<Transaction>?) : List<BpmnTransactionCollapsedSubprocess>? {
            return input?.filter { it.hasExternalDiagram }?.map { convertToCollapsedDto(it) }
        }

        @Mapping(target = "transactionalSubprocess", constant = "true")
        abstract fun convertToDto(input: Transaction) : BpmnTransactionalSubProcess

        @Mapping(target = "transactionalSubprocess", constant = "true")
        abstract fun convertToCollapsedDto(input: Transaction) : BpmnTransactionCollapsedSubprocess
    }
}