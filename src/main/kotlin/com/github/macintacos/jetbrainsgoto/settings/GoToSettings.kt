package com.github.macintacos.jetbrainsgoto.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(
    name = "GoToSettings",
    storages = [Storage("GoToSettings.xml")],
)
class GoToSettings : PersistentStateComponent<GoToSettings.State> {
    data class State(
        var previewDebounceMs: Int = 250,
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var previewDebounceMs: Int
        get() = myState.previewDebounceMs
        set(value) {
            myState.previewDebounceMs = value
        }

    companion object {
        fun getInstance(): GoToSettings =
            ApplicationManager.getApplication().getService(GoToSettings::class.java)
    }
}
