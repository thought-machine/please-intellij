package net.thoughtmachine.please.plugin.labels

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import net.thoughtmachine.please.plugin.PleaseFile
import net.thoughtmachine.please.plugin.PleaseFileType
import net.thoughtmachine.please.plugin.Target
import java.io.File
import java.nio.file.Paths

class BuildLabelCompletionContributor : CompletionContributor() {
    init {
        val pattern = PlatformPatterns.psiElement().inside(PyStringLiteralExpression::class.java)
        extend(CompletionType.BASIC, pattern, BuildLabelCompletionProvider())
    }
}

class BuildLabelCompletionProvider() : CompletionProvider<CompletionParameters>(){
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val stringLit = parameters.position.parent
        if(stringLit is PyStringLiteralExpression) {
            // I have no idea what they were smoking when they came up with this nonsense but this seems to be added to
            // all the text of leaf nodes in the AST for completions.
            val value = stringLit.stringValue.removeSuffix(CompletionUtilCore.DUMMY_IDENTIFIER)
            val thisFile = parameters.originalFile
            if (thisFile is PleaseFile) {
                results(value, thisFile).forEach{
                    result.addElement(LookupElementBuilder.create(it))
                }
            }
        }
    }

    // TODO(jpoole): unit test thing thing. It's a bit fiddly.
    private fun results(stringText : String, file: PleaseFile) : List<String> {
        if(stringText.startsWith(":")) {
            return targetsForFile(file, stringText.substring(1, stringText.length))
                .map { it.name }
        } else if(stringText.startsWith("//")) {
            val projectRoot = file.getProjectRoot() ?: return listOf()

            if(stringText.contains(':')) {
                val pkgDir = stringText.substringBefore(':').removePrefix("//")
                val pkgFile = VfsUtil.findFile(Paths.get(projectRoot.toString(), pkgDir), false)?.children
                    ?.firstOrNull { it.fileType == PleaseFileType }

                if(pkgFile != null) {
                    val psiFile = PsiUtilCore.getPsiFile(file.project, pkgFile)
                    if(psiFile is PleaseFile) {
                        return targetsForFile(psiFile, stringText.substringAfter(':'))
                            .map { it.label.removePrefix("//") }
                    }
                }
            } else {
                val isTerminated = stringText.endsWith("/")
                val relPath = Paths.get(stringText.removePrefix("//"))
                val relPkgDir = if(isTerminated) relPath else relPath.parent

                val path = projectRoot.resolve(relPath)
                val pkgDir = if(isTerminated) path else path.parent
                val prefix = if(isTerminated) "" else path.fileName.toString()

                return File(pkgDir.toUri()).listFiles()!!
                    .filter { it.isDirectory }
                    .filter { it.name.startsWith(prefix) }
                    .map { relPkgDir?.resolve(it.name)?.toString() ?: it.name }
            }
        }
        return listOf()
    }

    private fun targetsForFile(file: PleaseFile, prefix: String) : List<Target> {
        return file.targets().filter { it.name.startsWith(prefix) }
    }

}