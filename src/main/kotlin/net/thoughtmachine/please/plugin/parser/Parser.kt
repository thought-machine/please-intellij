package net.thoughtmachine.please.plugin.parser

import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.jetbrains.python.PythonParserDefinition
import net.thoughtmachine.please.plugin.PleaseFile
import net.thoughtmachine.please.plugin.PleaseLanguage

private val FILE = IFileElementType(PleaseLanguage)

/**
 * Some boilerplate to register the Please BUILD file parser with intellij
 */
class PleaseParserDefinition : PythonParserDefinition() {
    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return PleaseFile(viewProvider)
    }
}