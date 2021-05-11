package please.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.intellij.plugin.please.parser.PleaseParser
import please.PleaseFile
import please.PleaseLanguage
import org.intellij.plugin.please.psi.PleaseTypes

private val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE, EOL)
private val COMMENTS = TokenSet.create(PleaseTypes.COMMENT, PleaseTypes.DOC_COMMENT)
private val STRINGS = TokenSet.create(PleaseTypes.STR_LIT)
private val FILE = IFileElementType(PleaseLanguage)


class PleaseParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer {
        return PleaseLexer()
    }

    override fun getWhitespaceTokens(): TokenSet {
        // Don't pass whitespace to the parser
        return WHITE_SPACES
    }

    override fun getCommentTokens(): TokenSet {
        // Don't pass comments to the parser
        return COMMENTS
    }

    override fun getStringLiteralElements(): TokenSet {
        // TODO(jpoole): add f strings etc. here
        return STRINGS
    }

    override fun createParser(project: Project?): PsiParser {
        return PleaseParser()
    }

    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return PleaseFile(viewProvider)
    }

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): ParserDefinition.SpaceRequirements {
        return ParserDefinition.SpaceRequirements.MAY
    }

    override fun createElement(node: ASTNode?): PsiElement {
        return PleaseTypes.Factory.createElement(node)
    }
}