package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnReceiveTask
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
data class ReceiveTask(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        @XmlAttribute val documentation: String?,
        @XmlAttribute val async: Boolean?,
        @XmlAttribute val isForCompensation: Boolean?
): BpmnMappable<BpmnReceiveTask> {

    override fun toElement(): BpmnReceiveTask {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: ReceiveTask) : BpmnReceiveTask
    }
}