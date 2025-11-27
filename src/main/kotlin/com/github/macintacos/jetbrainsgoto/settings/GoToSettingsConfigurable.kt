package com.github.macintacos.jetbrainsgoto.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class GoToSettingsConfigurable : Configurable {
    private var debounceField: JBTextField? = null
    private var mainPanel: JPanel? = null

    override fun getDisplayName(): String = "Go to..."

    override fun createComponent(): JComponent {
        debounceField = JBTextField()

        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Preview debounce (ms):"), debounceField!!, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val settings = GoToSettings.getInstance()
        return debounceField?.text?.toIntOrNull() != settings.previewDebounceMs
    }

    override fun apply() {
        val settings = GoToSettings.getInstance()
        debounceField?.text?.toIntOrNull()?.let {
            settings.previewDebounceMs = it.coerceIn(0, 2000)
        }
    }

    override fun reset() {
        val settings = GoToSettings.getInstance()
        debounceField?.text = settings.previewDebounceMs.toString()
    }

    override fun disposeUIResources() {
        debounceField = null
        mainPanel = null
    }
}
