package com.valb3r.bpmn.intellij.plugin.core.advertisment

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import java.time.LocalDate

@State(name = "AdvertisementCache", storages = [(Storage("advertisement.iml.xml"))])
class AdvertisementCache: PersistentStateComponent<AdvertisementCache.State> {
    class State {
        var lastShowEpochDay: Long = LocalDate.MIN.toEpochDay()
    }

    private var myState = State()
    var lastShow: LocalDate
        get() = LocalDate.ofEpochDay(myState.lastShowEpochDay)
        set(lastShow){
            myState.lastShowEpochDay = lastShow.toEpochDay()
        }
    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project) = project.getService(AdvertisementCache::class.java)
    }
}