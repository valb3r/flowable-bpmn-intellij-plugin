package com.valb3r.bpmn.intellij.plugin.camunda.meta

import com.intellij.ide.highlighter.XmlLikeFileType
import com.intellij.lang.Language
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class CamundaFileType private constructor() : XmlLikeFileType(CamundaBpmnLanguage.INSTANCE) {

    override fun getName(): String {
        return "Camunda BPMN 2.0 Engine XML process"
    }

    override fun getDescription(): String {
        return "Camunda BPMN 2.0 Engine XML process"
    }

    override fun getDefaultExtension(): String {
        return "bpmn"
    }

    override fun getIcon(): Icon {
        return IconLoader.getIcon("META-INF/pluginIcon.svg")
    }

    companion object Instance {
        val INSTANCE = CamundaFileType()
    }
}

class CamundaBpmnLanguage : XMLLanguage(XMLLanguage.INSTANCE, "Camunda BPMN Engine XML process definition") {

    companion object Language {
        val INSTANCE: CamundaBpmnLanguage = CamundaBpmnLanguage()
    }
}