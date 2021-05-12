package net.thoughtmachine.please.plugin

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.FileViewProvider
import com.jetbrains.python.psi.impl.PyFileImpl
import java.nio.file.Path
import javax.swing.Icon


val PLEASE_ICON = IconLoader.getIcon("/icons/please.png", PleaseFileType.javaClass)
object PleaseLanguage : Language("Please")

// TODO(jpoole): We want a distinction between build_defs and BUILD files as we don't want to provide gutter icons on
//  build_def files in the same way.
object PleaseFileType : LanguageFileType(PleaseLanguage) {
    override fun getIcon() = PLEASE_ICON

    override fun getName() = "Please"

    override fun getDefaultExtension() = ".plz"

    override fun getDescription() = "Please BUILD file"
}

class PleaseFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, PleaseLanguage) {
    private var pkg = ""

    override fun getFileType(): FileType {
        return PleaseFileType
    }

    override fun toString(): String {
        return "Please File"
    }

    override fun getIcon(flags: Int): Icon {
        return PLEASE_ICON
    }

    /**
     * Gets the Please package name for the File by walking up the file tree to find the .plzconfig.
     */
    fun getPleasePackage() : String {
        if (pkg != "") {
            return pkg
        }
        var dir = Path.of(virtualFile.path).parent
        val path = mutableListOf<String>()
        while(true) {
            if(dir == null){
                throw RuntimeException("Could not locate .plzconfig")
            }

            val dirFile = dir.toFile()
            if (dir.toFile().list()!!.find { it == ".plzconfig" } != null) {
                pkg = path.joinToString("/")
                return pkg
            } else {
                path.add(0, dirFile.name)
                dir = dir.parent
            }
        }
    }
}
