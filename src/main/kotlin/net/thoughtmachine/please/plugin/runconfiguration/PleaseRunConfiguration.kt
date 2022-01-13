package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import net.thoughtmachine.please.plugin.PLEASE_ICON
import net.thoughtmachine.please.plugin.graph.BuildTarget
import net.thoughtmachine.please.plugin.graph.resolveTarget
import net.thoughtmachine.please.plugin.pleasecommandline.Please
import org.apache.tools.ant.types.Commandline
import org.jdom.Element
import javax.swing.JComponent
import javax.swing.JPanel

data class PleaseRunConfigArgs(
    var target: BuildTarget,
    var pleaseArgs: String = "",
    var programArgs: String = "",
    var workingDir: String = ""
)

class PleaseRunConfigurationType : ConfigurationTypeBase("PleaseRunConfigurationType", "plz run", "Run a build target in a please project", PLEASE_ICON) {
    class Factory(type : PleaseRunConfigurationType) : ConfigurationFactory(type) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return PleaseRunConfiguration(project, this, PleaseRunConfigArgs(BuildTarget.of("//some:target")))
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
    private val target = JBTextField("//some:target")
    private val programArgs = JBTextField()
    private val pleaseArgs = JBTextField()
    private val workingDir = JBTextField()


    override fun createEditor(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Target: "), target, 1, false)
            .addLabeledComponent(JBLabel("Program args: "), programArgs, 2, false)
            .addLabeledComponent(JBLabel("Please args: "), pleaseArgs, 3, false)
            .addLabeledComponent(JBLabel("Working dir: "), workingDir, 4, false)
            .addComponentFillVertically(JPanel(), 0).panel
    }

    override fun applyEditorTo(s: PleaseRunConfiguration) {
        s.args.target = resolveTarget(s.project, target.text)
        s.args.pleaseArgs = pleaseArgs.text
        s.args.programArgs = programArgs.text
        s.args.workingDir = workingDir.text
    }

    override fun resetEditorFrom(s: PleaseRunConfiguration) {
        target.text = s.args.target.label.toString()
        pleaseArgs.text = s.args.pleaseArgs
        programArgs.text = s.args.programArgs
        workingDir.text = s.args.workingDir
    }
}

/**
 * A run configuration to `plz run //some:target`
 */
class PleaseRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    var args: PleaseRunConfigArgs
) : LocatableConfigurationBase<RunProfileState>(project, factory, "plz run"), PleaseRunConfigurationBase {
    // TODO(jpoole): Don't just assume we want to use the first one (configurable? What happens when there's none?)
    var runStateProvider : PleaseDebugRunStateProvider? = null

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PleaseRunConfigurationSettings()
    }

    override fun getActiveRunStateProvider(): PleaseDebugRunStateProvider {
        if (runStateProvider == null) {
            runStateProvider = getDebugRunStateProviders().first()
        }
        return runStateProvider!!
    }

    override fun target() = args.target
    override fun pleaseArgs() = args.pleaseArgs

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val plzArgs = Commandline.translateCommandline(args.pleaseArgs).toList()

        if (executor == PleaseBuildExecutor) {
            return PleaseBuildConfiguration.getBuildProfileState(project, args.target.toString(), plzArgs)
        }

        if (executor == DefaultDebugExecutor.getDebugExecutorInstance()) {
           return getActiveRunStateProvider().getRunState(this)
        }

        // TODO(jpoole): Implement working directories for run states
        val programArgs = Commandline.translateCommandline(args.programArgs).toList()
        return PleaseProfileState(project, Please(project, pleaseArgs = plzArgs).run(args.target.toString(), programArgs = programArgs))
    }

    override fun writeExternal(element: Element) {
        element.setAttribute("target", args.target.toString())
        element.setAttribute("pleaseArgs", args.pleaseArgs)
        element.setAttribute("programArgs", args.programArgs)
        element.setAttribute("workingDir", args.workingDir)
    }

    override fun readExternal(element: Element) {
        val target = ApplicationManager.getApplication().runReadAction(Computable {
             resolveTarget(project, element.getAttributeValue("target") ?: "//some:target")
        })

        args = PleaseRunConfigArgs(
            target = target,
            pleaseArgs = element.getAttributeValue("pleaseArgs") ?: "",
            programArgs = element.getAttributeValue("programArgs") ?: "",
            workingDir = element.getAttributeValue("workingDir") ?: ""
        )
    }
}
