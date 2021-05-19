package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactoryImpl
import com.intellij.execution.runners.DebuggableRunProfileState
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.lang.RuntimeException
import java.net.InetSocketAddress
import java.nio.charset.Charset

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

class PleaseBuildState(private var target: String, private var project: Project) : RunProfileState {

    private fun startProcess(): ProcessHandler {
        val cmd = PtyCommandLine(mutableListOf("plz", "build", "-p", "-v", "notice", target))
        cmd.setWorkDirectory(project.basePath!!)
        return ProcessHandlerFactoryImpl.getInstance().createColoredProcessHandler(cmd)
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val process = startProcess()
        return DefaultExecutionResult(project.createConsole(process), process)
    }
}

class PleaseDebugState(private var target: String, private var project: Project, var address: InetSocketAddress) :
    DebuggableRunProfileState {

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