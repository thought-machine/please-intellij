package net.thoughtmachine.please.plugin.task

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandlerFactoryImpl
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.lang.RuntimeException
import java.nio.charset.Charset
import kotlin.concurrent.thread

abstract class PleaseTask(project: Project, title: String, private val projectRoot: String, private vararg val cmd: String) : Task.Backgroundable(project, title) {
    override fun run(indicator: ProgressIndicator) {
//        val cmd = GeneralCommandLine(listOf("plz", "-v", "notice", "-p", *cmd))
//        thread {
//            while (indicator.isRunning){
//
//            }
//
//        }
//
//        val cmd = GeneralCommandLine(mutableListOf("plz", "--config=dbg", "build", "-p", "-v", "notice", target))
//        cmd.setWorkDirectory(project.basePath!!)
//        cmd.isRedirectErrorStream = true
//        val process = ProcessHandlerFactoryImpl.getInstance().createColoredProcessHandler(cmd)
//        var output = ""
//        process.process.inputStream.bufferedReader(Charset.defaultCharset()).lines().forEach {
//            indicator.text2 = it
//            output += it
//        }
//
//        if(process.process.waitFor() != 0) {
//            throw RuntimeException(output)
//        }
    }

}