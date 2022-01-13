package net.thoughtmachine.please.plugin.graph

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor
import net.thoughtmachine.please.plugin.PleaseBuildFileType
import net.thoughtmachine.please.plugin.PleaseFile
import java.io.DataInput
import java.io.DataOutput

private val packageID = ID.create<String, Package>("net.thoughtmachine.please.plugin.graph.package")


object PackageIndexExtension :  FileBasedIndexExtension<String, Package>() {

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
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    override fun save(out: DataOutput, value: Package) {
        out.writeUTF(mapper.writeValueAsString(value))
    }

    override fun read(data: DataInput): Package {
        return mapper.readValue(data.readUTF().toByteArray(), Package::class.java)
    }
}

class PackageIndexer : DataIndexer<String, Package, FileContent> {
    override fun map(inputData: FileContent) : Map<String, Package> {
        val file = inputData.psiFile
        if(file !is PleaseFile) {
            return mapOf()
        }

        val targets = file.targets().map {
            val label = BuildLabel.parse(it.label)
            BuildTarget(label, it.element.callee?.text ?: "", listOf())
        }.associateBy { it.label }

        val pkgPath = file.getPleasePackage() ?: return mapOf()

        return mapOf(pkgPath to Package("", PackageLabel(pkgPath, null), targets))
    }

}

fun resolveTarget(project: Project, target: String): BuildTarget {
    val label = BuildLabel.parse(target)
    val pkgs = FileBasedIndex.getInstance().getValues(PackageIndexExtension.name, label.pkg, GlobalSearchScope.allScope(project))

    return pkgs.first().targets[label] ?: BuildTarget.of(target)
}
