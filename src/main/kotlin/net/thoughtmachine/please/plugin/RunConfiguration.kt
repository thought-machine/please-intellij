package net.thoughtmachine.please.plugin

import com.goide.dlv.DlvDisconnectOption
import com.goide.execution.GoRunUtil
import com.intellij.execution.*
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactoryImpl
import com.intellij.execution.runners.DebuggableRunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyStubElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.*
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.isRejected
import org.jetbrains.debugger.DebuggableRunConfiguration
import java.lang.Exception
import java.lang.RuntimeException
import java.net.InetSocketAddress
import java.nio.charset.Charset
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.SwingUtilities

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

        if(element.elementType != PyTokenTypes.IDENTIFIER) {
            return null
        }

        val callExpr = element.parent.parent
        if (callExpr !is PyCallExpression) {
            return null
        }

        val file = element.containingFile
        if(file !is PleaseFile) {
            return null
        }

        val name = callExpr.argumentList?.getKeywordArgument("name")
        if (name != null) {
            val expr = name.valueExpression
            if(expr is PyStringLiteralExpression) {
                if(file.getPleasePackage() == null) {
                    return null
                }
                val target = "//${file.getPleasePackage()}:${expr.stringValue}"
                return Info(
                    AllIcons.Actions.Execute, com.intellij.util.Function { "run $target" },
                    PleaseAction(element.project, DefaultRunExecutor.getRunExecutorInstance(), target, AllIcons.Actions.Execute),
                    PleaseAction(element.project, DefaultDebugExecutor.getDebugExecutorInstance(), target, AllIcons.Actions.StartDebugger)
                )
            }
        }

        return null
    }
}

/**
 * This is the actual action the gutter icons perform which creates and runs a please run configuration for the target.
 */
class PleaseAction(private val project: Project, private val executor : Executor, private val target : String, icon : Icon) : AnAction({ "plz run $target" }, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        val mgr = RunManager.getInstance(project) as RunManagerImpl
        val runConfig = PleaseRunConfiguration(project, PleaseRunConfigurationType.Factory(PleaseRunConfigurationType()), target)
        runConfig.name = "plz run $target"
        val config = RunnerAndConfigurationSettingsImpl(mgr, runConfig)

        mgr.addConfiguration(config)

        ProgramRunnerUtil.executeConfiguration(config, executor)
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

private fun (Project).createConsole(processHandler: ProcessHandler): ConsoleView {
    val console = TextConsoleBuilderFactory.getInstance().createBuilder(this).console
    console.attachToProcess(processHandler)
    return console
}

class PleaseRunState(private var target: String, private var project: Project) : RunProfileState {
    private fun startProcess(): ProcessHandler {
        val cmd = PtyCommandLine(mutableListOf("plz", "run", "-p", "-v", "notice", target))
        cmd.setWorkDirectory(project.basePath!!)
        return ProcessHandlerFactoryImpl.getInstance().createColoredProcessHandler(cmd)
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val process = startProcess()
        return DefaultExecutionResult(project.createConsole(process), process)
    }
}

class PleaseDebugState(private var target: String, private var project: Project, var address: InetSocketAddress) : DebuggableRunProfileState {
    private fun startProcess(): ProcessHandler {
        val cmd = GeneralCommandLine(mutableListOf("plz", "run", "--config=dbg", "-p", "--verbosity=notice", "--in_tmp_dir", "--cmd", "dlv exec ./\\\$OUT --api-version=2 --headless=true --listen=:${address.port}", target))
        //TODO(jpoole): this should use the files project root
        cmd.setWorkDirectory(project.basePath!!)
        return ProcessHandlerFactoryImpl.getInstance().createColoredProcessHandler(cmd)
    }

    private fun execute() : ExecutionResult {
        val process = startProcess()
        return DefaultExecutionResult(project.createConsole(process), process)
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        return execute()
    }

    override fun execute(debugPort: Int): Promise<ExecutionResult> {
        val promise = AsyncPromise<ExecutionResult>()

        val task = object : Task.Backgroundable(project, "plz build $target") {
            override fun onSuccess() {
                promise.setResult(execute())
            }

            override fun onThrowable(error: Throwable) {
                Notifications.Bus.notify(Notification("Please", "Failed build $target", error.message!!, NotificationType.ERROR))
                promise.setResult(null)
            }

            override fun run(indicator: ProgressIndicator) {

                val cmd = GeneralCommandLine(mutableListOf("plz", "--config=dbg", "build", "-p", "-v", "notice", target))
                cmd.setWorkDirectory(project.basePath!!)
                cmd.isRedirectErrorStream = true
                val process = ProcessHandlerFactoryImpl.getInstance().createColoredProcessHandler(cmd)
                var output = ""
                process.process.inputStream.bufferedReader(Charset.defaultCharset()).lines().forEach {
                    indicator.text2 = it
                    output += it
                }

                if(process.process.waitFor() != 0) {
                    throw RuntimeException(output)
                }
            }
        }
        ProgressManager.getInstance().run(task)

        return promise
    }
}
