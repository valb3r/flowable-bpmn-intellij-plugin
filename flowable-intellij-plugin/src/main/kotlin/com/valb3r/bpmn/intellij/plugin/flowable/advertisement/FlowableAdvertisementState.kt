package com.valb3r.bpmn.intellij.plugin.flowable.advertisement

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.valb3r.bpmn.intellij.plugin.core.advertisement.BaseAdvertisementState

@State(name = "FlowableIntellijPluginAdvertisementState", storages = [(Storage("valb3r-flowable-opensource-polybpmn-advertisement.xml"))], defaultStateAsResource = true)
class FlowableAdvertisementState: BaseAdvertisementState()