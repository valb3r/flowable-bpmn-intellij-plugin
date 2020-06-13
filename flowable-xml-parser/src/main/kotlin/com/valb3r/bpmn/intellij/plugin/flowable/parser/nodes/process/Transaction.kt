package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionalSubProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.ProcessBody
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
class Transaction: BpmnMappable<BpmnTransactionalSubProcess>, ProcessBody() {

    @Attribute var id: String? = null
    @Attribute var name: String? = null
    @PropertyElement var documentation: String? = null
    @Attribute var async: Boolean? = null
    @Attribute var exclusive: Boolean? = null

    override fun toElement(): BpmnTransactionalSubProcess {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Transaction) : BpmnTransactionalSubProcess
    }
}