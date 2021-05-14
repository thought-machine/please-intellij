package net.thoughtmachine.please.plugin.subinclude

import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiManager
import com.intellij.util.castSafelyTo
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import net.thoughtmachine.please.plugin.PleaseFile

class SubincludeEditorFactoryListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        if (event.editor.editorKind == EditorKind.MAIN_EDITOR) {
            val virtFile = FileDocumentManager.getInstance().getFile(event.editor.document)!!
            val file = PsiManager.getInstance(event.editor.project!!).findFile(virtFile)
            if (file !is PleaseFile) {
                return
            }

            file.accept(ResovledSubincludeVisitor(file))
        }
    }
}

// TODO(jpoole): a common listener that visits subincludes (and use for the inspection too.
class ResovledSubincludeVisitor(private val file: PleaseFile) : PyRecursiveElementVisitor() {
    override fun visitPyCallExpression(call: PyCallExpression) {
        val functionName = call.callee?.name ?: ""
        if(functionName != "subinclude"){
            return
        }
        val includes = call.arguments.asSequence()
            .map { it.castSafelyTo<PyStringLiteralExpression>() }.filterNotNull()
            .toList()

        includes.asSequence()
            .map { it.castSafelyTo<PyStringLiteralExpression>() }.filterNotNull()
            .forEach { expr ->
                if(!PleaseSubincludeManager.resolvedSubincludes.containsKey(expr.stringValue)) {
                    ProgressManager.getInstance().run(ResolveSubincludeBackgroundTask(file, includes.map { it.stringValue }))
                }
            }
    }
}