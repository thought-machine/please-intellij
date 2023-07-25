package net.thoughtmachine.please.plugin.runconfiguration.go

import com.goide.dlv.DlvDisconnectOption
import com.goide.execution.GoRunUtil
import com.intellij.execution.ExecutionResult
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import net.thoughtmachine.please.plugin.graph.BuildTarget
import net.thoughtmachine.please.plugin.runconfiguration.*
import java.net.InetSocketAddress


object GoDebugger : PleaseDebugger {
    override fun canRun(target: BuildTarget): Boolean {
        return true
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

