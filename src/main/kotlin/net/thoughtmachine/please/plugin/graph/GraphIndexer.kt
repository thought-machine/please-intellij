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
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.SlowOperations
import com.intellij.util.castSafelyTo
import com.intellij.util.indexing.*
import com.intellij.util.io.KeyDescriptor
import net.thoughtmachine.please.plugin.PleaseBuildFileType
import net.thoughtmachine.please.plugin.PleaseFile
import net.thoughtmachine.please.plugin.pleasecommandline.Please
import java.io.DataInput
import java.io.DataOutput
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private val packageID = ID.create<Pair<String, String>, Void>("net.thoughtmachine.please.plugin.graph.package")


object PackageIndexExtension : ScalarIndexExtension<Pair<String, String>>() {
    override fun getName(): ID<Pair<String, String>, Void> {
        return packageID
    }

    override fun getIndexer(): DataIndexer<Pair<String, String>, Void, FileContent> {
        return PackageIndexer
    }

    override fun getKeyDescriptor() = object : KeyDescriptor<Pair<String, String>> {
        override fun getHashCode(value: Pair<String, String>?): Int {
            return value.hashCode()
        }

        override fun isEqual(val1: Pair<String, String>?, val2: Pair<String, String>?): Boolean {
            return val1 == val2
        }

        override fun save(out: DataOutput, value: Pair<String, String>) {
            out.writeUTF(value.first)
            out.writeUTF(value.second)
        }

        override fun read(input: DataInput): Pair<String, String> {
            return Pair(input.readUTF(), input.readUTF())
        }

    }

    override fun getVersion() = 1

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(PleaseBuildFileType)
    }

    override fun dependsOnFileContent() = true

}


private data class TargetInfo(
    val name: String,
    val binary: Boolean,
    val test: Boolean,
    val labels: List<String> = emptyList()
)

object PackageIndexer : DataIndexer<Pair<String, String>, Void, FileContent> {

    // locatePackagePath walks the director tree to attempt to figure out what our package path and repo root are
    override fun map(filecontent: FileContent): Map<Pair<String, String>, Void?> {
        val file = filecontent.file
        var dir = Path.of(file.path).parent
        val path = mutableListOf<String>()
        while (true) {
            if (dir == null) {
                return emptyMap()
            }

            val dirFile = dir.toFile()
            if (dir.toFile().list()?.find { it == ".plzconfig" } != null) {
                val pkg = path.joinToString("/")
                val root = dir.toAbsolutePath()
                return mapOf(Pair(root.absolutePathString(), pkg) to null)
            } else {
                path.add(0, dirFile.name)
                dir = dir.parent
            }
        }
    }

    fun forFile(file: PsiFile) : Pair<String, String>? {
        return FileBasedIndex.getInstance()
            .getFileData(packageID, file.virtualFile, file.project).keys.firstOrNull()
    }

    fun lookup(project: Project, pleaseRepo: String, pkg: String) : PleaseFile? {
        return FileBasedIndex.getInstance()
            .getContainingFiles(packageID, Pair(pleaseRepo, pkg), GlobalSearchScope.projectScope(project))
            .firstNotNullOfOrNull { PsiUtilCore.getPsiFile(project, it).castSafelyTo<PleaseFile>() }
    }
}

class PackageService(val root: String) {
    private val pkgs = mutableMapOf<String, Package?>()

    // getPackageTargetInfo uses plz query print --json to get some information about the targets in this package.
    private fun getPackageTargetInfo(project: Project, root: String, pkg: String): Map<String, TargetInfo> {
        return try {
            val plzCmd = Please(project, true).query("print", "--json", "--omit_hidden", "--field=name", "--field=labels", "--field=test", "--field=binary", "//$pkg:all")
            val cmd = GeneralCommandLine(plzCmd).withWorkDirectory(root)
            val process = ProcessHandlerFactory.getInstance().createProcessHandler(cmd)

            return if (process.process.waitFor() == 0) {
                mapper.readValue(process.process.inputStream.readAllBytes())
            } else {
                val error = String(process.process.inputStream.readAllBytes())
                Notifications.Bus.notify(Notification("Please", "Failed to get target info", error, NotificationType.ERROR))
                emptyMap()
            }
        } catch (e :Exception) {
            Notifications.Bus.notify(Notification("Please", "Failed to get target info", e.message ?: "", NotificationType.ERROR))
            emptyMap()
        }
    }

    private fun computePackage(project: Project, pkgPath: String): Package? {
        val file = PackageIndexer.lookup(project, root, pkgPath) ?: return null
        val targetInfoMap = getPackageTargetInfo(file.project, root, pkgPath)


        val targets = file.targets().map {
            val label = BuildLabel(it.name, pkgPath)
            val targetInfo = targetInfoMap[label.toString()]
            BuildTarget(
                label = label,
                binary = targetInfo?.binary ?: false,
                test = targetInfo?.test ?: false,
                kind = it.kind(),
                labels = targetInfo?.labels ?: emptyList(),
            )
        }.associateBy { it.label.toString() }

        return Package(root, PackageLabel(pkgPath), targets)
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
                FileBasedIndex.getInstance().getAllKeys(packageID, project).map { it.first }
            }.mapNotNull { resolvePackage(project, it, pkg) }
        }
    }
}


