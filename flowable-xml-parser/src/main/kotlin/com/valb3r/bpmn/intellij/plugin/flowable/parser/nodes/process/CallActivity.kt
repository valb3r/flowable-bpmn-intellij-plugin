package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElementWrapper

@XmlAccessorType(XmlAccessType.FIELD)
data class CallActivity(
        @XmlAttribute val id: String,
        @XmlAttribute val name: String?,
        @XmlAttribute val documentation: String?,
        @XmlAttribute val async: Boolean?,
        @XmlAttribute val calledElement: String?,
        @XmlAttribute val calledElementType: String?,
        @XmlAttribute val inheritVariables: Boolean?,
        @XmlAttribute val fallbackToDefaultTenant: Boolean?,
        val extensionElements: ExtensionElements?
): BpmnMappable<BpmnCallActivity> {

    override fun toElement(): BpmnCallActivity {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: CallActivity) : BpmnCallActivity
    }
}

data class ExtensionElements(
        @XmlElementWrapper val out: List<OutExtensionElement>?
)

data class OutExtensionElement(val source: String?, val target: String?)