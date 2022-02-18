package net.thoughtmachine.please.plugin.labels

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import net.thoughtmachine.please.plugin.PleaseFile
import net.thoughtmachine.please.plugin.PsiTarget
import java.nio.file.Path
import java.util.stream.Collectors

class BuildLabelCompletionContributor : CompletionContributor() {
    init {
        val pattern = PlatformPatterns.psiElement().inside(PyStringLiteralExpression::class.java)
        extend(CompletionType.BASIC, pattern, BuildLabelCompletionProvider())
    }
}

class BuildLabelCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val stringLit = parameters.position.parent
        if (stringLit is PyStringLiteralExpression) {
            // I have no idea what they were smoking when they came up with this nonsense but this seems to be added to
            // all the text of leaf nodes in the AST for completions.
            val value = stringLit.stringValue.removeSuffix(CompletionUtilCore.DUMMY_IDENTIFIER)
            val thisFile = parameters.originalFile
            if (thisFile is PleaseFile) {
                results(value, thisFile).forEach {
                    result.addElement(LookupElementBuilder.create(it))
                }
            }
        }
    }

    private fun results(stringText: String, file: PleaseFile): List<String> {
        // If we're editing the file, chances are it won't parse so we can't use please for completions
        if (stringText.startsWith(":")) {
            return targetsForFile(file, stringText.substring(1, stringText.length))
                .map { it.name }
        }

        // TODO(jpoole): this should probably use the index 
        val cmd = GeneralCommandLine("plz query completions $stringText".split(" "))
        cmd.withWorkDirectory(file.virtualFile.parent.path)

        val process = ProcessHandlerFactory.getInstance().createProcessHandler(cmd)

        return if (process.process.waitFor() == 0) {
            // Large numbers of results here seem to break intellij
            process.process.inputStream.bufferedReader().lines()
                .map { it.removePrefix("//").removePrefix(":") }
                .collect(Collectors.toList()).take(50)
        } else {
            val error = String(process.process.inputStream.readAllBytes())
            Notifications.Bus.notify(Notification("Please", "Failed to complete label", error, NotificationType.ERROR))
            emptyList()
        }
    }

    private fun targetsForFile(file: PleaseFile, prefix: String): List<PsiTarget> {
        return file.targets().filter { it.name.startsWith(prefix) }
    }

}

