package com.valb3r.bpmn.intellij.activiti.plugin.advertisement

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.valb3r.bpmn.intellij.plugin.core.advertisement.BaseAdvertisementState

@State(name = "ActivitiIntellijPluginAdvertisementState", storages = [(Storage("valb3r-activiti-opensource-polybpmn-advertisement.xml"))], defaultStateAsResource = true)
class ActivitiAdvertisementState: BaseAdvertisementState()