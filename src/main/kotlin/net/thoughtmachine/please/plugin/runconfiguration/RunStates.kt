package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactoryImpl
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import org.apache.tools.ant.types.Commandline
import javax.swing.Icon

object PleaseBuildExecutor : DefaultRunExecutor() {
    override fun getIcon(): Icon {
        return AllIcons.Actions.Compile
    }

    override fun getStartActionText(): String {
        return "Build"
    }
}

object PleaseTestExecutor : DefaultRunExecutor() {
    override fun getStartActionText(): String {
        return "Test"
    }
}

fun (Project).createConsole(processHandler: ProcessHandler): ConsoleView {
    val console = TextConsoleBuilderFactory.getInstance().createBuilder(this).console
    console.attachToProcess(processHandler)
    return console
}

class PleaseProfileState(
    private var target: String,
    private var project: Project,
    private val cmd : String,
    private val pleaseArgs : String,
    private val programArgs : String
) : RunProfileState {
    private fun startProcess(): ProcessHandler {
        val plzArgs = Commandline.translateCommandline(pleaseArgs)
        val progArgs = Commandline.translateCommandline(programArgs)

        val cmd = PtyCommandLine(mutableListOf("plz", cmd, "-p", "-v", "info", target) + plzArgs + listOf("--") + progArgs)
        cmd.setWorkDirectory(project.basePath!!)
        return ProcessHandlerFactoryImpl.getInstance().createColoredProcessHandler(cmd)
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        val process = startProcess()
        return DefaultExecutionResult(project.createConsole(process), process)
    }
}

