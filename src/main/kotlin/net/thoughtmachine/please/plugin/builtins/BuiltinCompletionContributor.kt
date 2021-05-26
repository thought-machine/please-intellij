package net.thoughtmachine.please.plugin.builtins

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiManager
import com.intellij.util.ProcessingContext
import com.intellij.util.castSafelyTo
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyFile

class BuiltinCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PyTokenTypes.IDENTIFIER), BuiltinCompletionProvider())
    }
}

class BuiltinCompletionProvider : CompletionProvider<CompletionParameters>(){
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val text = parameters.position.text.removeSuffix(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)

        return PleaseBuiltins.PLEASE_BUILTINS.asSequence()
            .map(PsiManager.getInstance(parameters.editor.project!!)::findFile)
            .map { it.castSafelyTo<PyFile>() }
            .filterNotNull()
            .flatMap { it.iterateNames().asSequence() }
            .map { it.name }.filterNotNull()
            .filter { it.startsWith(text) }
            .forEach { result.addElement(LookupElementBuilder.create(it)) }
    }
}

