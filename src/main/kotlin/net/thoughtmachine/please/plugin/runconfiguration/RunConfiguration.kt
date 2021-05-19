package net.thoughtmachine.please.plugin.runconfiguration

import com.goide.dlv.DlvDisconnectOption
import com.goide.execution.GoRunUtil
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import net.thoughtmachine.please.plugin.PLEASE_ICON
import org.jetbrains.debugger.DebuggableRunConfiguration
import java.net.InetSocketAddress
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTextField

object PleaseBuildExecutor : DefaultRunExecutor() {
    override fun getIcon(): Icon {
        return AllIcons.Actions.Compile
    }
}

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

class PleaseRunConfiguration(project: Project, factory: ConfigurationFactory, var target: String) : LocatableConfigurationBase<RunProfileState>(project, factory, "Please"), DebuggableRunConfiguration{
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PleaseRunConfigurationSettings()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        if(executor is DefaultDebugExecutor) {
            return PleaseDebugState(target, this.project, super.computeDebugAddress(null))
        }

        if(executor is PleaseBuildExecutor) {
            return PleaseBuildState(target, this.project)
        }

        return PleaseRunState(target,  project)
    }

    override fun computeDebugAddress(state: RunProfileState): InetSocketAddress {
        return (state as PleaseDebugState).address
    }

    override fun createDebugProcess(
        socketAddress: InetSocketAddress,
        session: XDebugSession,
        executionResult: ExecutionResult?,
        environment: ExecutionEnvironment
    ): XDebugProcess {
        return GoRunUtil.createDlvDebugProcess(session, executionResult, socketAddress, true, DlvDisconnectOption.LEAVE_RUNNING)
    }
}

