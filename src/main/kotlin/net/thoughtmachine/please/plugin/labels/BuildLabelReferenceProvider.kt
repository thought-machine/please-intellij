@file:Suppress("UnstableApiUsage")

package net.thoughtmachine.please.plugin.labels

import com.intellij.model.Symbol
import com.intellij.model.SymbolResolveResult
import com.intellij.model.psi.*
import com.intellij.model.search.SearchRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.jetbrains.python.psi.PyStringLiteralExpression
import net.thoughtmachine.please.plugin.PleaseBuildFileType
import net.thoughtmachine.please.plugin.PleaseFile
import java.nio.file.Path
import java.nio.file.Paths

class BuildLabelReferenceProvider : PsiSymbolReferenceProvider {

    // TODO(jpoole): provide references for files as well
    override fun getReferences(
        element: PsiExternalReferenceHost,
        hints: PsiSymbolReferenceHints
    ): Collection<PsiSymbolReference> {
        if (element !is PyStringLiteralExpression) return emptyList()

        val file = element.containingFile
        if (file !is PleaseFile) return emptyList()

        file.virtualFile ?: return emptyList() // Can't resolve for in-memory files

        val text = element.stringValue.substringBefore("|") //Ignore the entrypoint
        when {
            text.startsWith(":") -> {
                val target = file.targets()
                    .firstOrNull { it.name == text.removePrefix(":") } ?: return emptyList()

                return mutableListOf(
                    BuildLabelSymbolReference(
                        element,
                        PsiSymbolService.getInstance().asSymbol(target.element)
                    )
                )
            }
            text.startsWith("///") -> {
                // TODO(jpoole): subrepos
                return mutableListOf()
            }
            text.startsWith("//") -> {
                val packagePath = text.removePrefix("//").substringBefore(":")
                val pleaseRoot = file.getProjectRoot() ?: return emptyList()
                val pleaseFile = findBuildFile(file.project, pleaseRoot, packagePath) ?: return emptyList()

                val name = if (text.contains(":")) text.substringAfter(":") else text.substringAfterLast("/")

                val result = pleaseFile.targets().filter { it.name == name }.map {
                    BuildLabelSymbolReference(element, PsiSymbolService.getInstance().asSymbol(it.element))
                }
                if(result.isNotEmpty()) {
                    return result
                }
                // Jump to the file if we can't find the actual rule.
                return listOf(BuildLabelSymbolReference(element, PsiSymbolService.getInstance().asSymbol(pleaseFile)))
            }
            else -> return mutableListOf()
        }

    }

    // We might be able to do something nice with "search everywhere" if we implement this though it's rather unclear
    // how it's meant to be hooked up. See SearchableSymbol for more information.
    override fun getSearchRequests(project: Project, target: Symbol) = emptyList<SearchRequest>()
}

fun findBuildFile(project: Project, projectRoot : Path, pkgName : String) : PleaseFile? {
    val virtFile = VfsUtil.findFile(Paths.get(projectRoot.toString(), pkgName), false)?.children
        ?.firstOrNull { it.fileType == PleaseBuildFileType } ?: return null

    val psiFile = PsiUtilCore.getPsiFile(project, virtFile)
    return if(psiFile is PleaseFile) psiFile else null
}

class BuildLabelSymbolReference(private val label: PyStringLiteralExpression, private val pyCallSymbol: Symbol) :
    PsiSymbolReference {
    override fun resolveReference(): MutableCollection<out SymbolResolveResult> {
        return mutableListOf(SymbolResolveResult { pyCallSymbol })
    }

    override fun getElement(): PsiElement {
        return label
    }

    override fun getRangeInElement(): TextRange {
        // TODO(jpoole): we could break up the element into numerous symbols for each part of the build label so
        //  we can jump to the the parent folders.
        return label.stringValueTextRange
    }
}