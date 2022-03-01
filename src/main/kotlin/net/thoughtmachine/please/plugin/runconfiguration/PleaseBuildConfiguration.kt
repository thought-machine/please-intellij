package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import net.thoughtmachine.please.plugin.PLEASE_ICON
import net.thoughtmachine.please.plugin.graph.BuildTarget
import net.thoughtmachine.please.plugin.pleasecommandline.Please
import org.apache.tools.ant.types.Commandline
import org.jdom.Element
import javax.swing.JComponent
import javax.swing.JPanel

data class PleaseBuildConfigArgs(
    var target: String,
    var pleaseArgs: String = "",
    var pleaseRoot: String = "",
)

object PleaseBuildConfigurationType : ConfigurationTypeBase("PleaseBuildConfigurationType", "plz build", "Build a build target in a please project", PLEASE_ICON) {
    class Factory(type : PleaseBuildConfigurationType) : ConfigurationFactory(type) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return PleaseBuildConfiguration(project, this, PleaseBuildConfigArgs("//some:target"))
        }

        override fun getId(): String {
            return "PleaseBuildConfigurationType.Factory"
        }
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(Factory(this))
    }
}

class PleaseBuildConfigurationSettings : SettingsEditor<PleaseBuildConfiguration>() {
    private val target = JBTextField("//some:target")
    private val pleaseArgs = JBTextField()
    private val pleaseRoot = JBTextField()


    override fun createEditor(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Target: "), target, 1, false)
            .addLabeledComponent(JBLabel("Please args: "), pleaseArgs, 2, false)
            .addLabeledComponent(JBLabel("Please project root: "), pleaseRoot, 3, false)
            .addComponentFillVertically(JPanel(), 0).panel
    }

    override fun applyEditorTo(s: PleaseBuildConfiguration) {
        s.args.target = target.text
        s.args.pleaseArgs = pleaseArgs.text
        s.args.pleaseRoot = pleaseRoot.text
    }

    override fun resetEditorFrom(s: PleaseBuildConfiguration) {
        target.text = s.args.target
        pleaseArgs.text = s.args.pleaseArgs
        pleaseRoot.text = s.args.pleaseRoot
    }
}

/**
 * A run configuration to `plz build //some:target`
 */
class PleaseBuildConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    var args: PleaseBuildConfigArgs
) : LocatableConfigurationBase<RunProfileState>(project, factory, "plz build"), PleaseRunConfigurationBase {
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PleaseBuildConfigurationSettings()
    }

    override fun target(): String {
        return args.target
    }

    override fun pleaseRoot(): String {
        return args.pleaseRoot
    }

    override fun pleaseArgs(): String {
        return args.pleaseArgs
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return getBuildProfileState(project, args.pleaseRoot, args.target, Commandline.translateCommandline(args.pleaseArgs).toList())
    }

    companion object {
        fun getBuildProfileState(project: Project, pleaseRoot: String?, target: String, pleaseArgs: List<String>): PleaseProfileState {
            return PleaseProfileState(project, pleaseRoot, Please(project, pleaseArgs = pleaseArgs).build(target))
        }
    }

    override fun writeExternal(element: Element) {
        element.setAttribute("target", args.target)
        element.setAttribute("pleaseRoot", args.pleaseRoot)
        element.setAttribute("pleaseArgs", args.pleaseArgs)
    }

    override fun readExternal(element: Element) {
        args = PleaseBuildConfigArgs(
            target = element.getAttributeValue("target") ?: "//some:target",
            pleaseArgs = element.getAttributeValue("pleaseArgs") ?: "",
            pleaseRoot = element.getAttributeValue("pleaseRoot") ?: ""
        )
    }
}

object PleaseBuildRunConfigurationProducer: PleaseRunConfigurationProducerBase<PleaseBuildConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return PleaseBuildConfigurationType.configurationFactories[0]
    }

    override fun setupConfigurationFromTarget(target: BuildTarget, configuration: PleaseBuildConfiguration): Boolean {
        val info = target.info ?: return false
        if(info.test) {
            return false
        }
        if(info.binary) {
            return false
        }

        configuration.args.target = target.label.toString()
        configuration.args.pleaseRoot = target.pkg.pleaseRoot
        configuration.name = "build :${target.label.name}"
        return true
    }
}
