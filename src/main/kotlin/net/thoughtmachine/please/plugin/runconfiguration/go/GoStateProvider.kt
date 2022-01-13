package net.thoughtmachine.please.plugin.runconfiguration.go

import com.goide.dlv.DlvDisconnectOption
import com.goide.execution.GoRunUtil
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactoryImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.util.text.SemVer
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import net.thoughtmachine.please.plugin.graph.BuildTarget
import net.thoughtmachine.please.plugin.pleasecommandline.Please
import net.thoughtmachine.please.plugin.runconfiguration.*
import org.apache.tools.ant.types.Commandline
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.net.InetSocketAddress
import java.nio.charset.Charset


object GoStateProvider : PleaseDebugRunStateProvider {
    override fun canRun(target: BuildTarget): Boolean {
        System.out.println(target.kind)
        return target.kind == "go_test" || target.kind == "go_binary"
    }

    override fun getRunState(config: PleaseRunConfigurationBase): PleaseDebugState {
        return PleaseGoDebugState(config, config.computeDebugAddress(null))
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
    val config: PleaseRunConfigurationBase,
    val address: InetSocketAddress
) : PleaseDebugState {
    override fun getDebugAddress() = address

    private fun getPleaseVersion() : SemVer? {
        val cmd = GeneralCommandLine(Please(config.project).version())
        val process = PleaseProcessHandler(cmd)
        if (process.process.waitFor() == 0) {
            val output = String(process.process.inputStream.readAllBytes())
                .removePrefix("Please version ")
                .trim()
            return SemVer.parseFromText(output)
        }
        val error = String(process.process.inputStream.readAllBytes())
        Notifications.Bus.notify(Notification("Please", "Failed to determine version. Some features may not work as expected.", error, NotificationType.WARNING))

        return null
    }

    private fun startForPleaseRun(config: PleaseRunConfiguration): ProcessHandler {
        val runCmd = listOf("dlv", "exec", "\\\$PWD/\\\$OUT", "--api-version=2", "--headless=true",
            "--listen=:${address.port}", "--wd=${config.args.workingDir}", "--") + config.args.programArgs
        val plzArgs = Commandline.translateCommandline(config.pleaseArgs()).toList()
        val cmd = GeneralCommandLine(Please(config.project, pleaseArgs = plzArgs).run(
            config.args.target.label.toString(),
            inTmpDir = true,
            cmd = runCmd
        ))
        //TODO(jpoole): this should use the files project root
        cmd.setWorkDirectory(config.project.basePath!!)
        return PleaseProcessHandler(cmd)
    }

    private fun startForPleaseTest(config: PleaseTestConfiguration): ProcessHandler {
        val version = getPleaseVersion()
        val shouldExec = version != null && (version.major > 16 || version.major == 16 && version.minor >= 4)

        val dlvCmd = listOf("TESTS=${config.args.tests}", "dlv", "exec", "\\\$PWD/\\\$OUT", "--api-version=2", "--headless=true",
            "--listen=:${address.port}")

        val plzArgs = Commandline.translateCommandline(config.args.pleaseArgs).toList()

        val cmd = if (shouldExec) {
            GeneralCommandLine(Please(config.project, pleaseArgs = plzArgs).exec(config.args.target.toString(), dlvCmd))
        } else {
            GeneralCommandLine(Please(config.project, pleaseArgs = plzArgs).run(
                config.args.target.toString(),
                inTmpDir = true,
                cmd = dlvCmd
            ))
        }

        //TODO(jpoole): this should use the files project root
        cmd.setWorkDirectory(config.project.basePath!!)
        return PleaseProcessHandler(cmd)
    }

    private fun startProcess() : ProcessHandler {
        if(config is PleaseRunConfiguration) {
            return startForPleaseRun(config)
        }

        return startForPleaseTest(config as PleaseTestConfiguration)
    }

    private fun execute() : ExecutionResult {
        val process = startProcess()
        return DefaultExecutionResult(config.project.createConsole(process), process)
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        return execute()
    }

    override fun execute(debugPort: Int): Promise<ExecutionResult> {
        val promise = AsyncPromise<ExecutionResult>()

        val task = object : Task.Backgroundable(config.project, "plz build ${config.target()}") {
            override fun onSuccess() {
                promise.setResult(execute())
            }

            override fun onThrowable(error: Throwable) {
                Notifications.Bus.notify(Notification("Please", "Failed build ${config.target()}", "", NotificationType.ERROR))
                promise.setResult(execute())
            }

            override fun run(indicator: ProgressIndicator) {
                val plzArgs = Commandline.translateCommandline(config.pleaseArgs()).toList()
                val cmd = GeneralCommandLine(Please(this.project, pleaseArgs = plzArgs).build(config.target().label.toString()))

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
