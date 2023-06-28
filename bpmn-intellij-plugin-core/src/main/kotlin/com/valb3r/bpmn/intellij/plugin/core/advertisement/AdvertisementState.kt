package com.valb3r.bpmn.intellij.plugin.core.advertisement

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.time.LocalDate

@State(name = "AdvertisementState", storages = [(Storage("opensource-polybpmn-advertisement.xml"))], defaultStateAsResource = true)
class AdvertisementState: PersistentStateComponent<AdvertisementState.State> {
    class State {
        var advertisementCommonShownAtDay: Long = LocalDate.MIN.toEpochDay()
        var advertisementSwimpoolShownAtDay: Long = LocalDate.MIN.toEpochDay()
    }

    private var myState = State()
    var lastShowCommon: LocalDate
        get() = LocalDate.ofEpochDay(myState.advertisementCommonShownAtDay)
        set(lastShow){
            myState.advertisementCommonShownAtDay = lastShow.toEpochDay()
        }
    var lastShowSwimpoolAd: LocalDate
        get() = LocalDate.ofEpochDay(myState.advertisementSwimpoolShownAtDay)
        set(lastShow){
            myState.advertisementSwimpoolShownAtDay = lastShow.toEpochDay()
        }
    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance() = ApplicationManager.getApplication().getService(AdvertisementState::class.java)
    }
}