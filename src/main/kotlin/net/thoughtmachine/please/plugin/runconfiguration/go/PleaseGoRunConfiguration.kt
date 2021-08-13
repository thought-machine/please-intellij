package net.thoughtmachine.please.plugin.runconfiguration.go

import com.goide.dlv.DlvDisconnectOption
import com.goide.execution.GoRunUtil
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactoryImpl
import com.intellij.execution.runners.DebuggableRunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import net.thoughtmachine.please.plugin.PLEASE_ICON
import net.thoughtmachine.please.plugin.runconfiguration.*
import net.thoughtmachine.please.plugin.pleasecommandline.Please
import org.apache.tools.ant.types.Commandline
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.debugger.DebuggableRunConfiguration
import java.net.InetSocketAddress
import java.nio.charset.Charset

class PleaseGoAction(private val project: Project, private val executor : Executor, private val target : String, private val test : String) :
    AnAction({"plz ${PleaseAction.verbForExecutor(executor)} $target"}, executor.icon) {

    override fun actionPerformed(e: AnActionEvent) {
        PleaseGoRunConfiguration(project, PleaseGoRunConfigurationType.Factory(PleaseGoRunConfigurationType()), target, "", "", "", test)
            .executeTarget(target, executor)
    }
}

class PleaseGoRunConfigurationType : ConfigurationTypeBase("PleaseGoRunConfigurationType", "Please (Golang)", "Run a please action on a target", PLEASE_ICON) {
    class Factory(type : PleaseGoRunConfigurationType) : ConfigurationFactory(type) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return PleaseGoRunConfiguration(project, this, "//some:target", "", "", "", "")
        }

        override fun getId(): String {
            return "PleaseGoRunConfigurationType.Factory"
        }
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(Factory(this))
    }
}

class PleaseGoRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    override var target: String,
    override var pleaseArgs: String,
    override var programArgs: String,
    override var workingDir: String,
    override var tests: String
) :
    LocatableConfigurationBase<RunProfileState>(project, factory, "Please (Golang)"), DebuggableRunConfiguration, PleaseRunConfigurationBase {

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PleaseRunConfigurationSettings(true)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        if (executor is DefaultDebugExecutor) {
            return PleaseGoDebugState(target, pleaseArgs, programArgs, workingDir, tests, this.project, super.computeDebugAddress(null))
        }
        return stateFor(executor)
    }

    override fun computeDebugAddress(state: RunProfileState): InetSocketAddress {
        return (state as PleaseGoDebugState).address
    }

    override fun createDebugProcess(
        socketAddress: InetSocketAddress,
        session: XDebugSession,
        executionResult: ExecutionResult?,
        environment: ExecutionEnvironment
    ): XDebugProcess {
        return GoRunUtil.createDlvDebugProcess(
            session,
            executionResult,
            socketAddress,
            true,
            DlvDisconnectOption.LEAVE_RUNNING
        )
    }
}

class PleaseGoDebugState(
    private var target: String,
    private var pleaseArgs : String,
    private var programArgs: String,
    private var workingDir: String,
    private var tests: String,
    private var project: Project,
    var address: InetSocketAddress
) : DebuggableRunProfileState {

    private fun startProcess(): ProcessHandler {
        val plzArgs = Commandline.translateCommandline(pleaseArgs).toList()
        val wd = if(workingDir == "") "." else workingDir

        val execCmd = listOf("TESTS=${tests}", "dlv", "exec", "\\\$PWD/\\\$OUT", "--api-version=2", "--headless=true",
            "--listen=:${address.port}", "--wd=$wd", "--", programArgs)

        val cmd = GeneralCommandLine(Please(this.project, pleaseArgs = plzArgs).exec(target, execCmd))

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
                val pleaseArgs = Commandline.translateCommandline(pleaseArgs).toList()
                val cmd = GeneralCommandLine(Please(this.project, pleaseArgs = pleaseArgs).build(target))

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

object PreloadGoRunConfig : PreloadingActivity() {
    override fun preload(indicator: ProgressIndicator) {
        PleaseLineMarkerProvider.actionProducers.add { project, target -> listOf(
            PleaseGoAction(project, DefaultDebugExecutor.getDebugExecutorInstance(), target, "")
        )}
    }
}