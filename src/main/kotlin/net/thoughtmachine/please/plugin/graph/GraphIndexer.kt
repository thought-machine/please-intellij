package net.thoughtmachine.please.plugin.graph

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor
import net.thoughtmachine.please.plugin.PleaseBuildFileType
import net.thoughtmachine.please.plugin.PleaseFile
import net.thoughtmachine.please.plugin.pleasecommandline.Please
import java.io.DataInput
import java.io.DataOutput
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private val packageID = ID.create<String, Package>("net.thoughtmachine.please.plugin.graph.package")


object PackageIndexExtension : FileBasedIndexExtension<String, Package>() {

    override fun getName(): ID<String, Package> {
        return packageID
    }

    override fun getIndexer() = PackageIndexer()

    override fun getValueExternalizer() = PackageExternalizer

    override fun getVersion() = 1

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(PleaseBuildFileType)
    }

    override fun dependsOnFileContent() = true

    override fun getKeyDescriptor(): KeyDescriptor<String> = object : KeyDescriptor<String> {
        override fun getHashCode(value: String): Int {
            return value.hashCode()
        }

        override fun isEqual(val1: String, val2: String): Boolean {
            return val1 == val2
        }

        override fun save(out: DataOutput, value: String) {
            out.writeUTF(value)
        }

        override fun read(input: DataInput): String {
            return input.readUTF()
        }

    }
}

object PackageExternalizer : DataExternalizer<Package> {
    private val mapper = jacksonObjectMapper()

    override fun save(out: DataOutput, value: Package) {
        out.writeUTF(mapper.writeValueAsString(value))
    }

    override fun read(data: DataInput): Package {
        return mapper.readValue(data.readUTF().toByteArray(), Package::class.java)
    }
}

private data class TargetInfo(
    val name: String,
    val binary: Boolean,
    val test: Boolean,
    val labels: List<String> = emptyList()
)

class PackageIndexer : DataIndexer<String, Package, FileContent> {
    private val mapper: ObjectMapper = ObjectMapper().registerModule(
        KotlinModule.Builder().configure(KotlinFeature.NullToEmptyCollection, true).build()
    ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    // locatePackagePath walks the director tree to attempt to figure out what our package path and repo root are
    private fun locatePackagePath(file: VirtualFile): Pair<String, String>? {
        println("${file.path} locate pkg")
        var dir = Path.of(file.path).parent
        val path = mutableListOf<String>()
        while (true) {
            if (dir == null) {
                println("${file.path} locate pkg failed")
                return null
            }

            val dirFile = dir.toFile()
            if (dir.toFile().list()?.find { it == ".plzconfig" } != null) {
                val pkg = path.joinToString("/")
                val root = dir.toAbsolutePath()
                println("${file.path} done pkg: $pkg")
                return Pair(root.absolutePathString(), pkg)
            } else {
                path.add(0, dirFile.name)
                dir = dir.parent
            }
        }
    }

    // getPackageTargetInfo uses plz query print --json to get some information about the targets in this package.
    private fun getPackageTargetInfo(project: Project, root: String, pkg: String): Map<String, TargetInfo> {
        val plzCmd = Please(project, true).query("print", "--json", "--omit_hidden", "--field=name", "--field=labels", "--field=test", "--field=binary", "//$pkg:all")
        println(plzCmd.joinToString(" "))

        val cmd = GeneralCommandLine(plzCmd).withWorkDirectory(root)
        val process = ProcessHandlerFactory.getInstance().createProcessHandler(cmd)

        return if (process.process.waitFor() == 0) {
            println("success.. deserialising ")
            mapper.readValue(process.process.inputStream.readAllBytes())
        } else {
            val error = String(process.process.inputStream.readAllBytes())
            Notifications.Bus.notify(Notification("Please", "Failed to complete label", error, NotificationType.ERROR))
            emptyMap()
        }
    }

    override fun map(inputData: FileContent): Map<String, Package> {
        val file = inputData.psiFile
        if (file !is PleaseFile) {
            return mapOf()
        }

        val (root, pkgPath) = locatePackagePath(inputData.file) ?: return mapOf()

        val targetInfoMap = try {
            getPackageTargetInfo(inputData.project, root, pkgPath)
        } catch (e :Exception) {
            println("$pkgPath failed to query print")
            emptyMap()
        }
        println("found pkg info")

        val targets = file.targets().map {
            val label = BuildLabel(it.name, pkgPath)
            val targetInfo = targetInfoMap[label.toString()]
            BuildTarget(
                label = label,
                binary = targetInfo?.binary ?: true,
                test = targetInfo?.test ?: true,
                kind = it.kind(),
                labels = targetInfo?.labels ?: emptyList(),
            )
        }.associateBy { it.label.toString() }

        return mapOf(pkgPath to Package(root, PackageLabel(pkgPath), targets))
    }

}

fun resolveTarget(project: Project, target: String): BuildTarget {
    val label = parseLabel(target)
    val pkgs = FileBasedIndex.getInstance()
        .getValues(PackageIndexExtension.name, label.pkg, GlobalSearchScope.allScope(project))

    return pkgs.first().targets[target]!!
}
