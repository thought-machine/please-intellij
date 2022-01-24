package net.thoughtmachine.please.plugin.graph

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.util.SlowOperations
import com.intellij.util.indexing.FileBasedIndex
import net.thoughtmachine.please.plugin.pleasecommandline.Please

class PackageService(val root: String) {
    private val pkgs = mutableMapOf<String, Package?>()

    // getPackageTargetInfo uses plz query print --json to get some information about the targets in this package.
    private fun getPackageTargetInfo(project: Project, root: String, pkg: String): Map<String, TargetInfo> {
        return try {
            val plzCmd = Please(project, true).query("print", "--json", "--omit_hidden", "--field=name", "--field=labels", "--field=test", "--field=binary", "//$pkg:all")
            val cmd = GeneralCommandLine(plzCmd).withWorkDirectory(root)
            val process = ProcessHandlerFactory.getInstance().createProcessHandler(cmd)

            val exitCode = process.process.waitFor()
            return if (exitCode == 0) {
                mapper.readValue(process.process.inputStream.readAllBytes())
            } else {
                val error = String(process.process.inputStream.readAllBytes())
                throw RuntimeException("Command `${plzCmd.joinToString(" ")}` failed:\nExit code: $exitCode\n$error")
            }
        } catch (e :Exception) {
            Notifications.Bus.notify(Notification("Please", "Failed to get target info", e.message ?: "", NotificationType.ERROR))
            emptyMap()
        }
    }

    private fun computePackage(project: Project, pkgPath: String): Package? {
        val file = PackageIndexer.lookup(project, root, pkgPath) ?: return null
        val targetInfoMap = getPackageTargetInfo(file.project, root, pkgPath)


        val pkg = Package(root, PackageLabel(pkgPath))

        file.targets().map {
            val label = BuildLabel(it.name, pkgPath)
            BuildTarget(
                label = label,
                info = targetInfoMap[label.toString()],
                kind = it.kind(),
                pkg = pkg,
            )
        }.forEach { pkg.addTarget(it) }

        return pkg
    }

    fun resolvePackage(project: Project, pkg: String): Package? {
        SlowOperations.assertSlowOperationsAreAllowed()

        return pkgs.getOrPut(pkg) {
            computePackage(project, pkg)
        }
    }

    companion object {
        private val mapper: ObjectMapper = ObjectMapper().registerModule(
            KotlinModule.Builder().configure(KotlinFeature.NullToEmptyCollection, true).build()
        ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        private val instances = mutableMapOf<String, PackageService>()
        private val allRoots = mutableMapOf<Project, List<String>>()

        fun getInstance(pleaseRoot: String) : PackageService {
            return instances.getOrPut(pleaseRoot) {
                PackageService(pleaseRoot)
            }
        }

        fun resolvePackage(project: Project, root: String, pkg: String): Package? {
            return getInstance(root).resolvePackage(project, pkg)
        }

        fun resolvePackage(project: Project, pkg: String) : List<Package> {
            return allRoots.getOrPut(project) {
                FileBasedIndex.getInstance().getAllKeys(PackageIndexExtension.name, project).map { it.first }
            }.mapNotNull { resolvePackage(project, it, pkg) }
        }
    }
}