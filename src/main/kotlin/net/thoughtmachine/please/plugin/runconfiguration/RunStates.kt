package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.util.io.BaseDataReader.SleepingPolicy
import com.intellij.util.io.BaseOutputReader
import net.thoughtmachine.please.plugin.pleasecommandline.PleaseCommand
import javax.swing.Icon


object PleaseBuildExecutor : DefaultRunExecutor() {
    override fun getIcon(): Icon {
        return AllIcons.Actions.Compile
    }

    override fun getStartActionText(): String {
        return "Build"
    }
}

fun (Project).createConsole(processHandler: ProcessHandler): ConsoleView {
    val console = TerminalExecutionConsole(this, processHandler)

    console.attachToProcess(processHandler)
    return console
}

class PleaseProfileState(
    private var project: Project,
    private var pleaseCommand: PleaseCommand
) : RunProfileState {
    private fun startProcess(): ProcessHandler {
        val cmd = PtyCommandLine(pleaseCommand)
        cmd.setWorkDirectory(project.basePath!!)

        return PleaseProcessHandler(cmd)
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val process = startProcess()
        return DefaultExecutionResult(project.createConsole(process), process)
    }
}

