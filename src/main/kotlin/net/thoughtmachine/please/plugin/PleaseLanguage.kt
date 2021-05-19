package net.thoughtmachine.please.plugin

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.FileViewProvider
import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.impl.PyFileImpl
import java.nio.file.Path
import javax.swing.Icon


val PLEASE_ICON = IconLoader.getIcon("/icons/please.png", PleaseBuildFileType.javaClass)
object PleaseLanguage : Language("Please")

abstract class PleaseFileType : LanguageFileType(PleaseLanguage)

object PleaseBuildFileType : PleaseFileType() {
    override fun getIcon() = PLEASE_ICON

    override fun getName() = "Please BUILD file"

    override fun getDefaultExtension() = ".plz"

    override fun getDescription() = "Please BUILD file"
}

object PleaseBuildDefFileType : PleaseFileType() {
    override fun getIcon() = PLEASE_ICON

    override fun getName() = "Please build_defs file"

    override fun getDefaultExtension() = ".build_defs"

    override fun getDescription() = "Please build definition file"
}

class PleaseFile(viewProvider: FileViewProvider, private var type : PleaseFileType) : PyFileImpl(viewProvider, PleaseLanguage) {
    var locatedRepoRoot = false
    private var pkg : String? = null
    private var repo : Path? = null

    override fun getFileType(): FileType {
        return type
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
        // Build definitions file don't belong to a package.
        if (type == PleaseBuildDefFileType) {
            return null
        }

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

fun (PyCallExpression).asTarget(pkgName: String) : Target? {
    val nameArg = argumentList?.getKeywordArgument("name")?.valueExpression
    if(nameArg != null && nameArg is PyStringLiteralExpression) {
        return Target("//$pkgName:${nameArg.stringValue}", nameArg.stringValue, this)
    }
    return null
}

private class TargetVisitor(private val pkgName : String) : PyRecursiveElementVisitor() {
    val targets = mutableListOf<Target>()

    override fun visitPyCallExpression(node: PyCallExpression) {
        val target = node.asTarget(pkgName)
        if (target != null) {
            targets.add(target)
        }
    }
}

data class Target(val label : String, val name : String, val element: PyCallExpression)

class PleasePythonInspections : PyInspectionExtension() {
    override fun ignoreInterpreterWarnings(file: PyFile): Boolean {
        return file is PleaseFile
    }
}