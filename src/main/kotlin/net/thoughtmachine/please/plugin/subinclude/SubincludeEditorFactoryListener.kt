package net.thoughtmachine.please.plugin.subinclude

import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiManager
import net.thoughtmachine.please.plugin.PleaseFile

class SubincludeEditorFactoryListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        if (event.editor.editorKind == EditorKind.MAIN_EDITOR) {
            val virtFile = FileDocumentManager.getInstance().getFile(event.editor.document)!!
            val file = PsiManager.getInstance(event.editor.project!!).findFile(virtFile)
            if (file !is PleaseFile) {
                return
            }

            DumbService.getInstance(file.project).smartInvokeLater {
                ProgressManager.getInstance().run(ResolveSubincludeBackgroundTask(file, file.getSubincludes().toList()))
            }
        }
    }
}

