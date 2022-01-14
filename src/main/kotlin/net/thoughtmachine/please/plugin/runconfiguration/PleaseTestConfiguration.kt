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
        target.text = s.args.target.toString()
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
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PleaseTestConfigurationSettings()
    }

    override fun target() = args.target
    override fun pleaseArgs() = args.pleaseArgs

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val plzArgs = Commandline.translateCommandline(args.pleaseArgs).toList()
        if (executor == PleaseBuildExecutor) {
            return PleaseBuildConfiguration.getBuildProfileState(project, args.target.toString(), plzArgs)
        }

        if (executor == DefaultDebugExecutor.getDebugExecutorInstance()) {
            return PleaseDebugState(this, computeDebugAddress(null))
        }

        return PleaseProfileState(project, Please(project, pleaseArgs = plzArgs).test(args.target.toString(), args.tests))
    }

    override fun writeExternal(element: Element) {
        element.setAttribute("target", args.target.toString())
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
