package net.thoughtmachine.please.plugin.parser

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import net.thoughtmachine.please.plugin.psi.PleaseTypes

/**
 * Provides highlighting for Please build files
 */
object PleaseSyntaxHighlighter : SyntaxHighlighterBase() {
    private val colors = mapOf(
        TokenType.BAD_CHARACTER to HighlighterColors.BAD_CHARACTER,
        PleaseTypes.COLON to DefaultLanguageHighlighterColors.SEMICOLON,
        PleaseTypes.COMMA to DefaultLanguageHighlighterColors.COMMA,
        PleaseTypes.COMMENT to DefaultLanguageHighlighterColors.LINE_COMMENT,
        PleaseTypes.DOC_COMMENT to DefaultLanguageHighlighterColors.DOC_COMMENT,
        PleaseTypes.CONTINUE to DefaultLanguageHighlighterColors.KEYWORD,
        PleaseTypes.DEF to DefaultLanguageHighlighterColors.KEYWORD,
        PleaseTypes.EQ to DefaultLanguageHighlighterColors.OPERATION_SIGN,
        PleaseTypes.FALSE_LIT to DefaultLanguageHighlighterColors.KEYWORD,
        PleaseTypes.IDENT to DefaultLanguageHighlighterColors.IDENTIFIER,
        PleaseTypes.INT_LIT to DefaultLanguageHighlighterColors.NUMBER,
        PleaseTypes.LBRACE to DefaultLanguageHighlighterColors.BRACES,
        PleaseTypes.LBRACK to DefaultLanguageHighlighterColors.BRACKETS,
        PleaseTypes.LPAREN to DefaultLanguageHighlighterColors.PARENTHESES,
        PleaseTypes.PASS to DefaultLanguageHighlighterColors.KEYWORD,
        PleaseTypes.PIPE to DefaultLanguageHighlighterColors.OPERATION_SIGN,
        PleaseTypes.RBRACE to DefaultLanguageHighlighterColors.BRACES,
        PleaseTypes.RBRACK to DefaultLanguageHighlighterColors.BRACKETS,
        PleaseTypes.RPAREN to DefaultLanguageHighlighterColors.PARENTHESES,
        PleaseTypes.STR_LIT to DefaultLanguageHighlighterColors.STRING,
        PleaseTypes.TRUE_LIT to DefaultLanguageHighlighterColors.KEYWORD
    )

    override fun getHighlightingLexer(): Lexer {
        return PleaseLexer()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        val key = colors[tokenType]
        if (key != null) {
            return arrayOf(key)
        }
        return arrayOf()
    }
}
