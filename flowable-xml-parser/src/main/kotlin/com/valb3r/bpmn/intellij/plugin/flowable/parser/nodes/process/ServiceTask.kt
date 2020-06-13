package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
data class ServiceTask(
        @Attribute val id: String,
        @Attribute val name: String?,
        @PropertyElement val documentation: String?,
        @Attribute val async: Boolean?,
        @Attribute val expression: String?,
        @Attribute val delegateExpression: String?,
        @Attribute(name = "class") val clazz: String?,
        @Attribute val skipExpression: String?,
        @Attribute val triggerable: Boolean?,
        @Attribute val isForCompensation: Boolean?,
        @Attribute val type: String?
): BpmnMappable<BpmnServiceTask> {

    override fun toElement(): BpmnServiceTask {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: ServiceTask) : BpmnServiceTask
    }
}