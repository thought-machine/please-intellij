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

            ProgressManager.getInstance().run(ResolveSubincludeBackgroundTask(file, file.getSubincludes().toList()))
        }
    }
}

