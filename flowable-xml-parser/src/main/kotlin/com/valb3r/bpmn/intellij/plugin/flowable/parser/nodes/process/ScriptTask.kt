package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnScriptTask
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

@Xml
data class ScriptTask(
        @Attribute val id: String,
        @Attribute val name: String?,
        @PropertyElement val documentation: String?,
        @Attribute val async: Boolean?,
        @Attribute val isForCompensation: Boolean?,
        @Attribute(name = "script") val scriptBody: String?,
        @Attribute val scriptFormat: String?,
        @Attribute val autoStoreVariables: Boolean?
): BpmnMappable<BpmnScriptTask> {

    override fun toElement(): BpmnScriptTask {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: ScriptTask) : BpmnScriptTask
    }
}