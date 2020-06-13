package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnAdHocSubProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.ProcessBody
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements.CompletionCondition
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
data class AdHocSubProcess(
        @Attribute val id: String,
        @Attribute val name: String?,
        @Attribute val ordering: String?,
        @PropertyElement val documentation: String?,
        @Element val completionCondition: CompletionCondition?
): BpmnMappable<BpmnAdHocSubProcess>, ProcessBody() {

    override fun toElement(): BpmnAdHocSubProcess {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: AdHocSubProcess) : BpmnAdHocSubProcess
    }
}