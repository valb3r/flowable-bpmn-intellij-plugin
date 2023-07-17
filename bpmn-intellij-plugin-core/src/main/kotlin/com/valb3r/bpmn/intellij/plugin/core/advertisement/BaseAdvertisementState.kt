package com.valb3r.bpmn.intellij.plugin.core.advertisement

import com.intellij.openapi.components.PersistentStateComponent
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

val currentAdvertisementStateProvider = AtomicReference<() -> BaseAdvertisementState>() // Not map as is global

// Due to class name collision each plugin implementation should reference its own class
open class BaseAdvertisementState: PersistentStateComponent<BaseAdvertisementState.State> {
    class State {
        var advertisementCommonShownAtDay: Long = LocalDate.MIN.toEpochDay()
        var advertisementSwimpoolShownAtDay: Long = LocalDate.MIN.toEpochDay()
    }

    private var myState = State()
    var lastDisplayDateGlobal: LocalDate
        get() = LocalDate.ofEpochDay(myState.advertisementCommonShownAtDay)
        set(lastShow){
            myState.advertisementCommonShownAtDay = lastShow.toEpochDay()
        }
    var lastDisplayDateSwimpoolAd: LocalDate
        get() = LocalDate.ofEpochDay(myState.advertisementSwimpoolShownAtDay)
        set(lastShow){
            myState.advertisementSwimpoolShownAtDay = lastShow.toEpochDay()
        }
    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }
}

fun currentAdvertisementState(): BaseAdvertisementState {
    // This is required to access state this way, because ServiceManagerImpl.getComponentInstance -> ComponentStoreImpl.initComponent are
    // responsible for loading from XML
    // TODO this null-replacing dummy is is a hack for 'Searchable options index builder failed' java.lang.NullPointerException of build
    val current = currentAdvertisementStateProvider.get() ?: return object : BaseAdvertisementState() {}
    return current()
}