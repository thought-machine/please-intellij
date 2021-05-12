package net.thoughtmachine.please.plugin.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.lexer.PythonIndentingLexer
import com.jetbrains.python.parsing.PyParser
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyStubElementType
import net.thoughtmachine.please.plugin.PleaseFile
import net.thoughtmachine.please.plugin.PleaseLanguage

private val FILE = IFileElementType(PleaseLanguage)

/**
 * Some boilerplate to register the Please BUILD file parser with intellij
 */
class PleaseParserDefinition : ParserDefinition {
    override fun createLexer(project: Project) = PythonIndentingLexer()

    override fun createParser(project: Project) = PyParser()

    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    override fun getCommentTokens() = TokenSet.create(PyTokenTypes.END_OF_LINE_COMMENT)

    override fun getStringLiteralElements() = TokenSet.orSet(PyTokenTypes.STRING_NODES, PyTokenTypes.FSTRING_TOKENS)

    override fun getWhitespaceTokens() = TokenSet.create(PyTokenTypes.LINE_BREAK, PyTokenTypes.SPACE, PyTokenTypes.TAB, PyTokenTypes.FORMFEED)

    override fun createElement(node: ASTNode): PsiElement {
        return when (val type = node.elementType) {
            is PyElementType -> {
                type.createElement(node)
            }
            is PyStubElementType<*, *> -> {
                type.createElement(node)
            }
            else -> {
                ASTWrapperPsiElement(node)
            }
        }
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return PleaseFile(viewProvider)
    }
}