package com.valb3r.bpmn.intellij.plugin.flowable.meta

import com.intellij.ide.highlighter.XmlLikeFileType
import com.intellij.lang.Language
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class FlowableFileType private constructor() : XmlLikeFileType(FlowableBpmnLanguage.INSTANCE) {

  override fun getName(): String {
    return "Flowable BPMN 2.0 Engine XML process"
  }

  override fun getDescription(): String {
    return "Flowable BPMN 2.0 Engine XML process"
  }

  override fun getDefaultExtension(): String {
    return "bpmn20.xml"
  }

  override fun getIcon(): Icon {
    return IconLoader.getIcon("META-INF/pluginIcon.svg")
  }

  companion object Instance {
    val INSTANCE = FlowableFileType()
  }
}

class FlowableBpmnLanguage : Language("Flowable BPMN Engine XML process definition") {

  companion object Language {
    val INSTANCE: FlowableBpmnLanguage = FlowableBpmnLanguage()
  }
}