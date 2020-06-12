package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
data class UserTask(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        val documentation: String?,
        @XmlAttribute val async: Boolean?,
        @XmlAttribute val isForCompensation: Boolean?,
        @XmlAttribute val assignee: String?,
        @XmlAttribute val dueDate: String?,
        @XmlAttribute val category: String?,
        @XmlAttribute val formKey: String?,
        @XmlAttribute val formFieldValidation: Boolean?,
        @XmlAttribute val priority: String?,
        @XmlAttribute val skipExpression: String?
): BpmnMappable<BpmnUserTask> {

    override fun toElement(): BpmnUserTask {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: UserTask) : BpmnUserTask
    }
}