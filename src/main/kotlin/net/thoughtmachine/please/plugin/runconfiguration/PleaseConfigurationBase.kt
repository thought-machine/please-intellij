package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.*
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.runners.DebuggableRunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.net.NetUtils
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerBundle
import net.thoughtmachine.please.plugin.graph.BuildLabel
import net.thoughtmachine.please.plugin.graph.BuildTarget
import net.thoughtmachine.please.plugin.graph.PackageIndexExtension
import org.jetbrains.debugger.DebuggableRunConfiguration
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * A base interface grouping all the Please run configurations together
 */
interface PleaseRunConfigurationBase : DebuggableRunConfiguration {
    /**
     * Returns the active PleaseTargetRunStateProvider that the configuration has selected.
     */
    fun getActiveRunStateProvider() : PleaseDebugRunStateProvider

    fun target() : BuildTarget
    fun pleaseArgs(): String

    override fun computeDebugAddress(state: RunProfileState?): InetSocketAddress {
        if (state is PleaseDebugState) {
            return state.getDebugAddress()
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
        return getActiveRunStateProvider()
            .createDebugProcess(socketAddress, session, executionResult, environment)
    }

    /**
     * Returns a list of PleaseTargetRunStateProvider that are applicable to this run configuration
     */
    fun getDebugRunStateProviders() : List<PleaseDebugRunStateProvider> {


        return runStateProviderEP.extensionList
            .filter { it.canRun(target()) }
    }

    companion object {
        private val runStateProviderEP = ExtensionPointName.create<PleaseDebugRunStateProvider>("net.thoughtmachine.please.plugin.pleaseDebugRunStateProvider")
    }
}

interface PleaseDebugRunStateProvider {

    /**
     * Whether this can provide a DebuggableRunProfileState for the given run configuration
     */
    fun canRun(config: BuildTarget) : Boolean

    /**
     * Returns DebuggableRunProfileState for the given run configuration
     */
    fun getRunState(config: PleaseRunConfigurationBase) : PleaseDebugState


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

interface PleaseDebugState : DebuggableRunProfileState {
    fun getDebugAddress() : InetSocketAddress
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