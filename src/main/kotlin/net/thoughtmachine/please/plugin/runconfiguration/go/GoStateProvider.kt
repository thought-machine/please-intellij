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


object GoStateProvider : PleaseDebugger {
    override fun canRun(target: BuildTarget): Boolean {
        System.out.println(target.kind)
        return target.kind == "go_test" || target.kind == "go_binary"
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

