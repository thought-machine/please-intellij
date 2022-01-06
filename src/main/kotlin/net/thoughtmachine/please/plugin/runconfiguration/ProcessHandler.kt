package net.thoughtmachine.please.plugin.runconfiguration

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.util.io.BaseDataReader
import com.intellij.util.io.BaseOutputReader

class PleaseProcessHandler(cmd: GeneralCommandLine) : KillableProcessHandler(cmd) {
    override fun readerOptions(): BaseOutputReader.Options {
        return object : BaseOutputReader.Options() {
            override fun policy(): BaseDataReader.SleepingPolicy {
                return BaseDataReader.SleepingPolicy.BLOCKING
            }

            override fun splitToLines(): Boolean {
                return false
            }

            override fun withSeparators(): Boolean {
                return true
            }
        }
    }
}