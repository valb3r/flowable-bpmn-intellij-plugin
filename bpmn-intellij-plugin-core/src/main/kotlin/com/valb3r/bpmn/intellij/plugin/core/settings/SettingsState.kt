package com.valb3r.bpmn.intellij.plugin.core.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.util.xmlb.XmlSerializerUtil

class SettingsState: PersistentStateComponent<SettingsState> {

    var ideaStatus = false

 companion object {
     fun getInstance(): SettingsState? {
         return ServiceManager.getService(SettingsState::class.java)
     }
 }

    override fun getState(): SettingsState {
        return this
    }

    override fun loadState(state: SettingsState) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
