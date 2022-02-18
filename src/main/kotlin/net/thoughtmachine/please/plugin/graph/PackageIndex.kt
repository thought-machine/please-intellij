package net.thoughtmachine.please.plugin.graph

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.castSafelyTo
import com.intellij.util.indexing.*
import com.intellij.util.io.KeyDescriptor
import net.thoughtmachine.please.plugin.PleaseBuildFileType
import net.thoughtmachine.please.plugin.PleaseFile
import java.io.DataInput
import java.io.DataOutput
import java.nio.file.Path
import kotlin.io.path.absolutePathString

object PackageIndexExtension : ScalarIndexExtension<Pair<String, String>>() {
    private val id = ID.create<Pair<String, String>, Void>("net.thoughtmachine.please.plugin.graph.package")

    override fun getName(): ID<Pair<String, String>, Void> {
        return id
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
            .getFileData(PackageIndexExtension.name, file.virtualFile, file.project).keys.firstOrNull()
    }

    fun lookup(project: Project, pleaseRepo: String, pkg: String) : PleaseFile? {
        return FileBasedIndex.getInstance()
            .getContainingFiles(PackageIndexExtension.name, Pair(pleaseRepo, pkg), GlobalSearchScope.projectScope(project))
            .firstNotNullOfOrNull { PsiUtilCore.getPsiFile(project, it).castSafelyTo<PleaseFile>() }
    }
}




