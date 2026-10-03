package com.valb3r.bpmn.intellij.plugin.camunda.advertisement

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.valb3r.bpmn.intellij.plugin.core.advertisement.BaseAdvertisementState

@State(name = "CamundaIntellijPluginAdvertisementState", storages = [(Storage("valb3r-camunda-opensource-polybpmn-advertisement.xml"))], defaultStateAsResource = true)
class CamundaAdvertisementState: BaseAdvertisementState()