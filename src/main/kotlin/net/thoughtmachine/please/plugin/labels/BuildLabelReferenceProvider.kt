@file:Suppress("UnstableApiUsage")

package net.thoughtmachine.please.plugin.labels

import com.intellij.model.Symbol
import com.intellij.model.SymbolResolveResult
import com.intellij.model.psi.*
import com.intellij.model.search.SearchRequest
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyStringLiteralExpression
import net.thoughtmachine.please.plugin.PleaseFile
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class BuildLabelReferenceProvider : PsiSymbolReferenceProvider {
    override fun getReferences(
        element: PsiExternalReferenceHost,
        hints: PsiSymbolReferenceHints
    ): Collection<PsiSymbolReference> {
        if(element !is PyStringLiteralExpression) return listOf()

        val file = element.containingFile
        if(file !is PleaseFile) return listOf()

        file.virtualFile ?: return listOf() // Can't resolve for in-memory files

        val text = element.stringValue
        if(text.startsWith(":")) {
            val target = file.targets()
                .firstOrNull { it.name == text.removePrefix(":") } ?: return listOf()

            return mutableListOf(BuildLabelSymbolReference(
                element,
                PsiSymbolService.getInstance().asSymbol(target.element)
            ))
        } else if(text.startsWith("//")) {
            val packagePath = text.removePrefix("//").substringBefore(":")
            val pleaseRoot = file.getProjectRoot() ?: return listOf()
            val pleaseFile = findBuildFile(file.project, pleaseRoot, packagePath) ?: return listOf()

            val name = if(text.contains(":")) text.substringAfter(":") else text.substringAfterLast("/")

            return pleaseFile.targets().filter { it.name == name }.map {
                BuildLabelSymbolReference(element, PsiSymbolService.getInstance().asSymbol(it.element))
            }
        }

        return mutableListOf()
    }

    // We might be able to do something nice with "search everywhere" if we implement this
    override fun getSearchRequests(project: Project, target: Symbol) = emptyList<SearchRequest>()
}

class BuildLabelSymbolReference(private val label: PyStringLiteralExpression, private val symbol: Symbol) : PsiSymbolReference {
    override fun resolveReference(): MutableCollection<out SymbolResolveResult> {
        return mutableListOf(SymbolResolveResult { symbol })
    }

    override fun getElement(): PsiElement {
        return label
    }

    override fun getRangeInElement(): TextRange {
        return label.stringValueTextRange
    }
}