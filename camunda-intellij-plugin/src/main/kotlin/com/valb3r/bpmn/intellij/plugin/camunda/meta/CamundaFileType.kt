package com.valb3r.bpmn.intellij.plugin.camunda.meta

import com.intellij.ide.highlighter.XmlLikeFileType
import com.intellij.lang.xml.XMLLanguage
import com.intellij.ui.IconManager
import javax.swing.Icon

class CamundaFileType private constructor() : XmlLikeFileType(XMLLanguage.INSTANCE) {

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
        return IconManager.getInstance().getIcon("META-INF/pluginIcon.svg", CamundaFileType::class.java.classLoader)
    }

    companion object Instance {
        val INSTANCE = CamundaFileType()
    }
}
