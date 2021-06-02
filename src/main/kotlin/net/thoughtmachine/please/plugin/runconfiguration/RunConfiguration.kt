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
import javax.swing.JComponent
import javax.swing.JTextField

class PleaseRunConfigurationType : ConfigurationTypeBase("PleaseRunConfigurationType", "Please", "Run a please action on a target", PLEASE_ICON) {
    class Factory(type : PleaseRunConfigurationType) : ConfigurationFactory(type) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return PleaseRunConfiguration(project, this, "//some:target")
        }

        override fun getId(): String {
            return "PleaseRunConfigurationType.Factory"
        }
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(Factory(this))
    }
}

class PleaseRunConfigurationSettings : SettingsEditor<PleaseRunConfiguration>() {
    private val target = JTextField("//some:target")

    override fun resetEditorFrom(s: PleaseRunConfiguration) {
        target.text = s.target
    }

    override fun createEditor(): JComponent {
        return target
    }

    override fun applyEditorTo(s: PleaseRunConfiguration) {
        s.target = target.text
    }
}

class PleaseRunConfiguration(project: Project, factory: ConfigurationFactory, var target: String) : LocatableConfigurationBase<RunProfileState>(project, factory, "Please") {
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PleaseRunConfigurationSettings()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        if(executor is PleaseBuildExecutor) {
            return PleaseProfileState(target, this.project, "build")
        }
        if(executor is PleaseTestExecutor) {
            // TODO(jpoole): we could parse the test output here and present it in a nice way with a custom console view
            return PleaseProfileState(target, this.project, "test")
        }
        return PleaseProfileState(target, this.project, "run")
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
