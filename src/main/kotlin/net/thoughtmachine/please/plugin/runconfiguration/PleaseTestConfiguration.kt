package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import net.thoughtmachine.please.plugin.PLEASE_ICON
import net.thoughtmachine.please.plugin.pleasecommandline.Please
import org.apache.tools.ant.types.Commandline
import org.jdom.Element
import javax.swing.JComponent
import javax.swing.JPanel

data class PleaseTestConfigArgs(
    var target: String,
    var pleaseArgs: String = "",
    var tests: String = ""
)

class PleaseTestConfigurationType : ConfigurationTypeBase("PleaseTestConfigurationType", "plz test", "Test a build target in a please project", PLEASE_ICON) {
    class Factory(type : PleaseTestConfigurationType) : ConfigurationFactory(type) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return PleaseTestConfiguration(project, this, PleaseTestConfigArgs("//some:target"))
        }

        override fun getId(): String {
            return "PleaseRunConfigurationType.Factory"
        }
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(Factory(this))
    }
}

class PleaseTestConfigurationSettings : SettingsEditor<PleaseTestConfiguration>() {
    private val target = JBTextField("//some:target")
    private val pleaseArgs = JBTextField()
    private val tests = JBTextField()


    override fun createEditor(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Target: "), target, 1, false)
            .addLabeledComponent(JBLabel("Please args: "), pleaseArgs, 3, false)
            .addLabeledComponent(JBLabel("Tests: "), tests, 4, false)
            .addComponentFillVertically(JPanel(), 0).panel
    }

    override fun applyEditorTo(s: PleaseTestConfiguration) {
        s.args.target = target.text
        s.args.pleaseArgs = pleaseArgs.text
        s.args.tests = tests.text
    }

    override fun resetEditorFrom(s: PleaseTestConfiguration) {
        target.text = s.args.target
        pleaseArgs.text = s.args.pleaseArgs
        tests.text = s.args.tests
    }
}

/**
 * A run configuration to `plz test //some:target`
 */
class PleaseTestConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    var args: PleaseTestConfigArgs
) : LocatableConfigurationBase<RunProfileState>(project, factory, "plz test"), PleaseRunConfigurationBase {
    // TODO(jpoole): Don't just assume we want to use the first one (configurable? What happens when there's none?)
    var runStateProvider : PleaseTargetRunStateProvider? = null

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PleaseTestConfigurationSettings()
    }

    override fun getActiveRunStateProvider(): PleaseTargetRunStateProvider {
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
            return PleaseBuildConfiguration.getBuildProfileState(project, args.target, plzArgs)
        }

        if (executor == DefaultDebugExecutor.getDebugExecutorInstance()) {
            return getActiveRunStateProvider().getRunState(this)
        }

        return PleaseProfileState(project, Please(project, pleaseArgs = plzArgs).test(args.target, args.tests))
    }

    override fun writeExternal(element: Element) {
        element.setAttribute("target", args.target)
        element.setAttribute("pleaseArgs", args.pleaseArgs)
        element.setAttribute("tests", args.tests)
    }

    override fun readExternal(element: Element) {
        args = PleaseTestConfigArgs(
            target = element.getAttributeValue("target") ?: "//some:target",
            pleaseArgs = element.getAttributeValue("pleaseArgs") ?: "",
            tests = element.getAttributeValue("tests") ?: "",
        )
    }
}
