package net.thoughtmachine.please.plugin.builtins

import com.intellij.psi.PsiManager
import com.intellij.util.castSafelyTo
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import net.thoughtmachine.please.plugin.PleaseLanguage

class BuiltinReferenceResolveProvider : PyReferenceResolveProvider {
    override fun resolveName(expression: PyQualifiedExpression, context: TypeEvalContext): List<RatedResolveResult> {
        if (expression.containingFile.language !is PleaseLanguage) {
            return emptyList()
        }

        return BuitlinSetContributor.PLEASE_BUILTINS.asSequence()
            .map(PsiManager.getInstance(expression.project)::findFile)
            .map { it.castSafelyTo<PyFile>() }
            .filterNotNull()
            .map { it.findExportedName(expression.name) }
            .filterNotNull()
            .map { RatedResolveResult(RatedResolveResult.RATE_NORMAL, it) }
            .toList()
    }
}