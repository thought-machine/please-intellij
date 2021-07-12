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
import net.thoughtmachine.please.plugin.Target
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
        // If we're editing the file, chances are it wont parse so we can't use please for completions
        if (stringText.startsWith(":")) {
            return targetsForFile(file, stringText.substring(1, stringText.length))
                .map { it.name }
        }

        val projectRoot = file.getProjectRoot() ?: return emptyList()

        // TODO(jpoole): codify this with a subcommand or something
        val cmd = GeneralCommandLine("plz query alltargets $stringText".split(" "))
        cmd.workDirectory = projectRoot.toFile()
        cmd.environment["GO_FLAGS_COMPLETION"] = "1"

        val process = ProcessHandlerFactory.getInstance().createProcessHandler(cmd)

        return if (process.process.waitFor() == 0) {
            process.process.inputStream.bufferedReader().lines()
                .map { it.removePrefix("//").removePrefix(":") }
                .collect(Collectors.toList())
        } else {
            val error = String(process.process.inputStream.readAllBytes())
            Notifications.Bus.notify(Notification("Please", "Failed to complete label", error, NotificationType.ERROR))
            listOf()
        }
    }

    private fun targetsForFile(file: PleaseFile, prefix: String): List<Target> {
        return file.targets().filter { it.name.startsWith(prefix) }
    }

}

