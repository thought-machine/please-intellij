package net.thoughtmachine.please.plugin.subinclude

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.concurrentMapOf
import net.thoughtmachine.please.plugin.PleaseFile
import java.nio.file.Paths
import java.util.stream.Collectors

object PleaseSubincludeManager {
    val resolvedSubincludes = concurrentMapOf<String, Set<VirtualFile>>()
    fun resolveSubinclude(fromFile : PleaseFile, subinclude: String): Set<VirtualFile> {
        if(resolvedSubincludes.containsKey(subinclude)) {
            return resolvedSubincludes[subinclude]!!
        }

        val cmd = GeneralCommandLine("plz query outputs $subinclude".split(" "))
        if(fromFile.getProjectRoot() == null){
            return setOf()
        }
        cmd.workDirectory = fromFile.getProjectRoot()!!.toFile()

        val process = ProcessHandlerFactory.getInstance().createProcessHandler(cmd)

        if(process.process.waitFor() == 0) {
            val files = process.process.inputStream.bufferedReader().lines()
                .map { VfsUtil.findFile(Paths.get(fromFile.getProjectRoot().toString(), it), true) }
                .collect(Collectors.toSet()).filterNotNull().toSet()
            resolvedSubincludes[subinclude] = files
            return files
        } else {
            val error = String(process.process.inputStream.readAllBytes())
            Notifications.Bus.notify(Notification("Please", "Failed to update subincludes", error, NotificationType.ERROR))
        }

        return emptySet()
    }
}
