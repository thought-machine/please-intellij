package net.thoughtmachine.please.plugin

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.FileViewProvider
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
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

class PleaseFile(viewProvider: FileViewProvider) : PyFileImpl(viewProvider, PleaseLanguage) {
    var locatedRepoRoot = false
    private var pkg : String? = null
    private var repo : Path? = null

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
    fun getPleasePackage() : String? {
        locatePleaseRepo()
        return pkg
    }

    fun getProjectRoot() : Path? {
        locatePleaseRepo()
        return repo
    }

    private fun locatePleaseRepo() {
        // TODO(jpoole): move build defs out into their own file type
        if (locatedRepoRoot) {
            return
        }
        var dir = Path.of(virtualFile.path).parent
        val path = mutableListOf<String>()
        while(true) {
            if(dir == null){
                return
            }

            val dirFile = dir.toFile()
            if (dir.toFile().list()?.find { it == ".plzconfig" } != null) {
                pkg = path.joinToString("/")
                repo = dir.toAbsolutePath()
                locatedRepoRoot = true
                return
            } else {
                path.add(0, dirFile.name)
                dir = dir.parent
            }
        }
    }

    fun targets() : List<Target> {
        val pkg = getPleasePackage()
        if(pkg != null) {
            val visitor = TargetVisitor(pkg)
            accept(visitor)
            return visitor.targets
        }
        return listOf()
    }
}

private class TargetVisitor(private val pkgName : String) : PyRecursiveElementVisitor() {
    val targets = mutableListOf<Target>()

    override fun visitPyCallExpression(node: PyCallExpression) {
        val nameArg = node.argumentList?.getKeywordArgument("name")?.valueExpression
        if(nameArg != null && nameArg is PyStringLiteralExpression) {
            targets.add(Target("//$pkgName:${nameArg.stringValue}", nameArg.stringValue, node))
        }
    }
}

data class Target(val label : String, val name : String, val element: PyCallExpression)
