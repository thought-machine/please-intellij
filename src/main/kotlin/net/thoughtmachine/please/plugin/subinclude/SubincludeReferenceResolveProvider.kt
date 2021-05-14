package net.thoughtmachine.please.plugin.subinclude

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.castSafelyTo
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import net.thoughtmachine.please.plugin.PleaseFile

class SubincludeReferenceResolveProvider : PyReferenceResolveProvider {
    override fun resolveName(expression: PyQualifiedExpression, context: TypeEvalContext): List<RatedResolveResult> {
        val file = expression.containingFile
        if (file !is PleaseFile) {
            return emptyList()
        }

        return findSubincludedLabels(expression.containingFile).asSequence()
            .map { PleaseSubincludeManager.resolvedSubincludes[it]?.asSequence() }
            .filterNotNull().flatten()
            .map(PsiManager.getInstance(expression.project)::findFile).filterNotNull()
            .map { it.castSafelyTo<PyFile>()!!.findExportedName(expression.name) }.filterNotNull()
            .map { RatedResolveResult(RatedResolveResult.RATE_NORMAL, it) }
            .toList()
    }

    private fun findSubincludedLabels(file: PsiFile): Set<String> {
        val loadStatements = file.children
            .mapNotNull { it as? PyExpressionStatement }
            // call expression is wrapped in a statement
            .mapNotNull { it.children.firstOrNull() as? PyCallExpression }
            .filter { it.callee?.name == "subinclude" }

        return loadStatements.asSequence().flatMap { statement ->
            statement.arguments.asSequence()
                .map { it.castSafelyTo<PyStringLiteralExpression>() }
                .filterNotNull()
                .map { it.stringValue }
                .asSequence()
        }.toSet()
    }
}