package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import net.thoughtmachine.please.plugin.PLEASE_ICON
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class PleaseRunConfigurationType : ConfigurationTypeBase("PleaseRunConfigurationType", "Please", "Run a please action on a target", PLEASE_ICON) {
    class Factory(type : PleaseRunConfigurationType) : ConfigurationFactory(type) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return PleaseRunConfiguration(project, this, "//some:target", "", "", "")
        }

        override fun getId(): String {
            return "PleaseRunConfigurationType.Factory"
        }
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(Factory(this))
    }
}

class PleaseRunConfigurationSettings(private val showWorkingDirField: Boolean) : SettingsEditor<PleaseRunConfigurationBase>() {
    private val target = JTextField("//some:target")
    private val programArgs = JTextField()
    private val pleaseArgs = JTextField()
    private val workingDir = JTextField()

    private fun (GridBagConstraints).addField(panel: JPanel, row: Int, name: String, field: JTextField) {
        this.fill = GridBagConstraints.HORIZONTAL
        this.gridy = row
        this.gridheight = 1

        this.gridx = 0
        this.gridwidth = 1
        panel.add(JLabel(name), this)

        this.gridx = 1
        this.gridwidth = 2
        field.columns = 30
        panel.add(field, this)
    }

    override fun createEditor(): JComponent {
        val panel = JPanel(GridBagLayout())
        val constraint = GridBagConstraints()

        constraint.addField(panel, 1, "Target", target)
        constraint.addField(panel, 2, "Program args", programArgs)
        constraint.addField(panel, 3, "Please args", pleaseArgs)
        if (showWorkingDirField) {
            constraint.addField(panel, 4, "Working dir", workingDir)
        }

        return panel
    }

    override fun applyEditorTo(s: PleaseRunConfigurationBase) {
        s.target = target.text
        s.pleaseArgs = pleaseArgs.text
        s.programArgs = programArgs.text
        s.workingDir = workingDir.text
    }

    override fun resetEditorFrom(s: PleaseRunConfigurationBase) {
        target.text = s.target
        pleaseArgs.text = s.pleaseArgs
        programArgs.text = s.programArgs
        workingDir.text = s.workingDir
    }
}

interface PleaseRunConfigurationBase : RunConfiguration {
    var target : String
    var pleaseArgs : String
    var programArgs : String
    var workingDir : String

    fun stateFor(executor: Executor) : RunProfileState {
        if(executor is PleaseBuildExecutor) {
            return PleaseProfileState(target, this.project, "build", pleaseArgs, "")
        }
        if(executor is PleaseTestExecutor) {
            // TODO(jpoole): we could parse the test output here and present it in a nice way with a custom console view
            return PleaseProfileState(target, this.project, "test", pleaseArgs, programArgs)
        }
        return PleaseProfileState(target, this.project, "run", pleaseArgs, programArgs)
    }
}

class PleaseRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    override var target: String,
    override var pleaseArgs: String,
    override var programArgs: String,
    override var workingDir: String
) : LocatableConfigurationBase<RunProfileState>(project, factory, "Please"), PleaseRunConfigurationBase {
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PleaseRunConfigurationSettings(false)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return stateFor(executor)
    }
}

object PreloadRunConfig : PreloadingActivity() {
    override fun preload(indicator: ProgressIndicator) {
        PleaseLineMarkerProvider.actionProducers.add { project, target -> listOf(
            PleaseAction(project, PleaseBuildExecutor, target, "build"),
            PleaseAction(project, PleaseTestExecutor, target, "test"),
            PleaseAction(project, DefaultRunExecutor.getRunExecutorInstance(), target, "run")
        )}
    }
}
