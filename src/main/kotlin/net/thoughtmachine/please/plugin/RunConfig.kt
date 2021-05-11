package net.thoughtmachine.please.plugin

import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactoryImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import net.thoughtmachine.please.plugin.parser.psi.PleaseFunctionCall
import net.thoughtmachine.please.plugin.parser.psi.PleaseTypes
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTextField

/**
 * Provides gutter icons against build rules in BUILD files
 */
class PleaseLineMarkerProvider : RunLineMarkerContributor() {

    // getInfo needs to apply the run info to the LeafPsiElement as that's what intellij demands. It looks for the
    // IDENT of the function call and applies the run actions to that.
    override fun getInfo(element: PsiElement): Info? {
        //TODO(jpoole): check if we're inspecting a BUILD file or a .build_defs file. We're wasting our time for
        //  build_def files as they don't contain build targets.
        if(element !is LeafPsiElement) {
            return null
        }

        // We're looking for the IDENT of the function call here, not any of the other tokens
        if(element.elementType != PleaseTypes.IDENT) {
            return null
        }

        val parent = element.parent
        if (parent !is PleaseFunctionCall) {
            return null
        }

        val file = element.containingFile

        val name = parent.functionCallParamList.find { it.ident?.text == "name" }?.expression?.value?.strLit?.text
        if (name != null && file is PleaseFile) {
            val target = "//${file.getPleasePackage()}:${name.trim { it == '\"' || it == '\''} }"
            return Info(
                AllIcons.Actions.Execute, com.intellij.util.Function { "run $target" },
                PleaseAction(element.project, "run", target, AllIcons.Actions.Execute),
                PleaseAction(element.project, "test", target, AllIcons.Actions.Execute),
                PleaseAction(element.project, "build", target, AllIcons.Actions.Compile)
                // TODO(jpoole): implement this
                // PleaseAction(element.project, "debug", target, AllIcons.Actions.StartDebugger)
            )
        }
        return null
    }
}

/**
 * This is the actual action the gutter icons perform which creates and runs a please run configuration for the target.
 */
class PleaseAction(private val project: Project, private val action : String, private val target : String, icon : Icon) : AnAction({ "plz $action $target" }, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        val mgr = RunManager.getInstance(project) as RunManagerImpl
        val runConfig = PleaseRunConfiguration(project, PleaseRunConfigurationType.Factory(PleaseRunConfigurationType()), target, action)
        runConfig.name = "plz $action $target"
        val config = RunnerAndConfigurationSettingsImpl(mgr, runConfig)

        mgr.addConfiguration(config)

        ProgramRunnerUtil.executeConfiguration(config, DefaultRunExecutor())
    }
}

class PleaseRunConfigurationType : ConfigurationTypeBase("PleaseRunConfigurationType", "Please", "Run a please action on a target", PLEASE_ICON) {
    class Factory(type : PleaseRunConfigurationType) : ConfigurationFactory(type) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return PleaseRunConfiguration(project, this, "//some:target", "build")
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

class PleaseRunConfiguration(project: Project, factory: ConfigurationFactory, var target: String, var action: String) : RunConfigurationBase<PleaseLaunchState>(project, factory, "Please"){
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PleaseRunConfigurationSettings()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return PleaseLaunchState(action, target,  project.basePath!!, environment)
    }
}

class PleaseLaunchState(private var action: String, private var target: String, private var projectRoot : String, environment: ExecutionEnvironment) : CommandLineState(environment) {
    override fun startProcess(): ProcessHandler {
        val cmd = PtyCommandLine(mutableListOf("plz", "-p", "-v", "notice", action, target))
        cmd.setWorkDirectory(projectRoot)
        return ProcessHandlerFactoryImpl.getInstance().createColoredProcessHandler(cmd)
    }
}