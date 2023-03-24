package net.thoughtmachine.please.plugin.subinclude

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.jetbrains.rd.util.concurrentMapOf
import net.thoughtmachine.please.plugin.PleaseFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Paths
import java.util.stream.Collectors

object PleaseSubincludeManager {
    val resolvedSubincludes = concurrentMapOf<String, Set<VirtualFile>>()
    fun resolveSubinclude(fromFile : PleaseFile, subinclude: String): Set<VirtualFile> {
        if(resolvedSubincludes.containsKey(subinclude)) {
            val includes = resolvedSubincludes[subinclude]!!
            // Check if the file has been deleted since we last resolved it
            if (includes.all { it.exists() }) {
                return includes
            }
            resolvedSubincludes.remove(subinclude)
        }

        val pkg = ApplicationManager.getApplication().runReadAction(Computable { fromFile.getPleasePackage() }) ?: return setOf()

        val cmd = GeneralCommandLine("plz query outputs $subinclude".split(" "))
        cmd.setWorkDirectory(fromFile.virtualFile.parent.path)

        val process = ProcessHandlerFactory.getInstance().createProcessHandler(cmd)

        if(process.process.waitFor() == 0) {
            val files = process.process.inputStream.bufferedReader().lines()
                .map (::resolveFilegroup)
                .map { VfsUtil.findFile(Paths.get(pkg.pleaseRoot, it), true) }
                .collect(Collectors.toSet()).filterNotNull().toSet()

            resolvedSubincludes[subinclude] = files
            return files
        }

        return emptySet()
    }

    // resolveFilegroup will try and find an identical file rather than the copy in `plz-out/gen` created by the
    // filegroup. This is useful as subincludes are usually simple filegroups.
    private fun resolveFilegroup(filePath : String) : String {
        try {
            val resolvedPath = filePath.removePrefix("plz-out/gen/")
            val origFile = File(filePath)
            val resolvedFile = File(resolvedPath)

            if (FileUtils.contentEquals(origFile, resolvedFile)) {
                return resolvedPath
            }
            return filePath

        } catch (ex : Exception) {
            return filePath
        }
    }
}

class DeletedSubincludeListener : BulkFileListener {
    override fun before(events: List<VFileEvent>) {
        events
            .filter { it is VFileDeleteEvent || it is VFileMoveEvent }
            .mapNotNull { it.file }
            .forEach { changedFile ->
                PleaseSubincludeManager.resolvedSubincludes
                    .filter { it.value.contains(changedFile) }
                    .forEach {PleaseSubincludeManager.resolvedSubincludes.remove(it.key)}
            }
    }
}
