package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnScriptTask
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class ScriptTask(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        val documentation: String?,
        @XmlAttribute val async: Boolean?,
        @XmlAttribute val isForCompensation: Boolean?,
        @XmlElement(name = "script") val scriptBody: String?,
        @XmlAttribute val scriptFormat: String?,
        @XmlAttribute val autoStoreVariables: Boolean?
): BpmnMappable<BpmnScriptTask> {

    override fun toElement(): BpmnScriptTask {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: ScriptTask) : BpmnScriptTask
    }
}