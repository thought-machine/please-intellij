package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.*
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactoryImpl
import com.intellij.execution.runners.DebuggableRunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.util.net.NetUtils
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerBundle
import net.thoughtmachine.please.plugin.graph.*
import net.thoughtmachine.please.plugin.pleasecommandline.Please
import org.apache.tools.ant.types.Commandline
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.debugger.DebuggableRunConfiguration
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.Charset

val targetKey = Key.create<BuildTarget>("pleaseTarget")

/**
 * A base interface grouping all the Please run configurations together
 */
interface PleaseRunConfigurationBase : DebuggableRunConfiguration {

    fun target() : String
    fun pleaseRoot() : String?
    fun pleaseArgs(): String

    override fun computeDebugAddress(state: RunProfileState?): InetSocketAddress {
        if (state is PleaseDebugState) {
            return state.address
        }
        return try {
            InetSocketAddress(InetAddress.getLoopbackAddress(), NetUtils.findAvailableSocketPort())
        } catch (e: IOException) {
            throw ExecutionException(XDebuggerBundle.message("error.message.cannot.find.available.port"), e)
        }
    }

    override fun createDebugProcess(
        socketAddress: InetSocketAddress,
        session: XDebugSession,
        executionResult: ExecutionResult?,
        environment: ExecutionEnvironment
    ): XDebugProcess {
        val target = environment.getUserData(targetKey)!!
        val debugger = PleaseDebugState.runStateProviderEP.extensionList.firstOrNull { it.canRun(target) } ?: throw RuntimeException("No debuggers for $target")
        return debugger.createDebugProcess(socketAddress, session, executionResult, environment)
    }
}

interface PleaseDebugger {

    /**
     * Whether this can provide a DebuggableRunProfileState for the given run configuration
     */
    fun canRun(target: BuildTarget) : Boolean


    /**
     * creates the debugger process that connects to the running executable started by the run state
     */
    fun createDebugProcess(
        socketAddress: InetSocketAddress,
        session: XDebugSession,
        executionResult: ExecutionResult?,
        environment: ExecutionEnvironment
    ): XDebugProcess
}

class PleaseDebugState(val config: PleaseRunConfigurationBase, val environment: ExecutionEnvironment, val address: InetSocketAddress) : DebuggableRunProfileState {
    private fun startProcess() : ProcessHandler {
        val plzArgs = Commandline.translateCommandline(config.pleaseArgs()).toList()
        val cmd = if(config is PleaseRunConfiguration) {
            GeneralCommandLine(Please(config.project, pleaseArgs = plzArgs).debug(
                config.target(),
                address.port,
                Commandline.translateCommandline(config.args.programArgs).toList()
            ))
        } else {
            GeneralCommandLine(Please(config.project, pleaseArgs = plzArgs).debug(
                config.target(),
                address.port,
                Commandline.translateCommandline((config as PleaseTestConfiguration).args.tests).toList()
            ))
        }
        val target = environment.getUserData(targetKey)!!
        cmd.setWorkDirectory(target.pkg.pleaseRoot)

        return PleaseProcessHandler(cmd)
    }

    private fun execute() : ExecutionResult {
        val process = startProcess()
        return DefaultExecutionResult(config.project.createConsole(process), process)
    }

    override fun execute(debugPort: Int): Promise<ExecutionResult> {
        val promise = AsyncPromise<ExecutionResult>()

        val task = object : Task.Backgroundable(config.project, "plz build ${config.target()}") {
            override fun onSuccess() {
                promise.setResult(execute())
            }

            override fun onThrowable(error: Throwable) {
                promise.setError(error)
                error.printStackTrace()
            }

            override fun run(indicator: ProgressIndicator) {
                val label = parseLabel(config.target())
                val pkg = ApplicationManager.getApplication().runReadAction(Computable {
                    if (config.pleaseRoot() != null){
                        PackageService.resolvePackage(project, config.pleaseRoot()!!, label.pkg)
                    } else {
                        PackageService.resolvePackage(project, label.pkg).firstOrNull()
                    }
                })
                val target = pkg?.targetByName(label.name) ?: throw RuntimeException("Can't find target ${config.target()}")

                environment.putUserData(targetKey, target)

                val plzArgs = Commandline.translateCommandline(config.pleaseArgs()).toList()
                val cmd = GeneralCommandLine(Please(this.project, pleaseArgs = plzArgs).build(config.target()))

                cmd.setWorkDirectory(target.pkg.pleaseRoot)
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

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        TODO("Not yet implemented")
    }

    companion object {
        val runStateProviderEP = ExtensionPointName.create<PleaseDebugger>("net.thoughtmachine.please.plugin.pleaseDebugger")
    }
}

/**
 * This is the actual action the gutter icons perform which creates and runs a please run configuration for the target.
 */
class PleaseAction(
    val project: Project,
    val target: String,
    private val command : String,
    private val executor: Executor,
    private val config: RunConfiguration
) :
    AnAction({"plz $command $target"}, executor.icon) {
    override fun actionPerformed(e: AnActionEvent) {
        val mgr = RunManager.getInstance(project) as RunManagerImpl
        val config = RunnerAndConfigurationSettingsImpl(mgr, config)

        mgr.addConfiguration(config)
        config.name = "plz $command $target"

        ProgramRunnerUtil.executeConfiguration(config, executor)
    }
}