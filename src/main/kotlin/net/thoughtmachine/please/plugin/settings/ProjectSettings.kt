package net.thoughtmachine.please.plugin.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel


class AppSettingsComponent(plz : String) {
    val panel: JPanel
    private val pleasePathField = JBTextField()

    var pleasePath: String
        get() = pleasePathField.text
        set(v) {
            pleasePathField.text = v
        }

    init {
        pleasePath = plz
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Please path: "), pleasePathField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}

class PleaseProjectConfigurable(val project: Project) : Configurable {
    private val persistenceService = PropertiesComponent.getInstance(project)
    private val settingsComponent = AppSettingsComponent(getPersistedPleasePath())

    private fun getPersistedPleasePath() = persistenceService.getValue(pleasePathPersistenceKey, "plz")
    private fun setPersistedPleasePath(path: String) = persistenceService.setValue(pleasePathPersistenceKey, path)

    override fun createComponent() = settingsComponent.panel

    override fun isModified(): Boolean {
        return getPersistedPleasePath() != settingsComponent.pleasePath
    }

    override fun apply() {
        setPersistedPleasePath(settingsComponent.pleasePath)
    }

    override fun getDisplayName() = "Please Project Settings"

    companion object {
        const val pleasePathPersistenceKey = "net.thoughtmachine.please.plugin.settings.pleasePath"
        fun getPleasePath(project: Project) = PropertiesComponent.getInstance(project).getValue(pleasePathPersistenceKey, "plz")
    }
}