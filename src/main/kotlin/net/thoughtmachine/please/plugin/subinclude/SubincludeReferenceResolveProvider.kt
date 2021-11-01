package net.thoughtmachine.please.plugin.subinclude

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.util.castSafelyTo
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import net.thoughtmachine.please.plugin.PleaseFile

object SubincludeReferenceResolveProvider : PyReferenceResolveProvider {
    override fun resolveName(expression: PyQualifiedExpression, context: TypeEvalContext): List<RatedResolveResult> {
        val file = expression.containingFile
        if (file !is PleaseFile) {
            return emptyList()
        }
        if (expression.name == null) {
            return emptyList()
        }

        return resolveName(file, expression.name!!)
            .map { RatedResolveResult(RatedResolveResult.RATE_NORMAL, it) }
            .toList()
    }

    fun resolveName(file: PleaseFile, name : String): List<PsiElement> {
        return file.getSubincludes().asSequence()
            .map { PleaseSubincludeManager.resolvedSubincludes[it]?.asSequence() }
            .filterNotNull().flatten()
            .map(PsiManager.getInstance(file.project)::findFile).filterNotNull()
            .map { it.castSafelyTo<PyFile>()?.findExportedName(name) }.filterNotNull()
            .toList()
    }
}