package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
data class ServiceTask(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        val documentation: String?,
        @XmlAttribute val async: Boolean?,
        @XmlAttribute val expression: String?,
        @XmlAttribute val delegateExpression: String?,
        @XmlAttribute(name ="class") val clazz: String?,
        @XmlAttribute val skipExpression: String?,
        @XmlAttribute val triggerable: Boolean?,
        @XmlAttribute val isForCompensation: Boolean?,
        @XmlAttribute val type: String?
): BpmnMappable<BpmnServiceTask> {

    override fun toElement(): BpmnServiceTask {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: ServiceTask) : BpmnServiceTask
    }
}