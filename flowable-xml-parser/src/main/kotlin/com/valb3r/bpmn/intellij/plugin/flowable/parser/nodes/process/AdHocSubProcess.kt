package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnAdHocSubProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.ProcessBody
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlValue

@XmlAccessorType(XmlAccessType.FIELD)
data class AdHocSubProcess(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        @XmlAttribute val ordering: String?,
        val documentation: String?,
        val completionCondition: CompletionCondition?
): BpmnMappable<BpmnAdHocSubProcess>, ProcessBody() {

    override fun toElement(): BpmnAdHocSubProcess {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: AdHocSubProcess) : BpmnAdHocSubProcess
    }
}

@XmlAccessorType(XmlAccessType.FIELD)
data class CompletionCondition(
        @XmlValue val condition: String? = null
)