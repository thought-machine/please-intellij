package org.intellij.plugin.please

import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactoryImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.ParserDefinition
import com.intellij.lang.ParserDefinition.SpaceRequirements
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerBase
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.*
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.Function
import org.intellij.plugin.please.parser.PleaseParser
import org.intellij.plugin.please.psi.PleaseFunctionCall
import org.intellij.plugin.please.psi.PleaseTypes
import org.wso2.lsp4intellij.IntellijLanguageClient
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTextField


val pleaseIcon = IconLoader.getIcon("/icons/please.png", PleaseFileType.javaClass)
object PleaseLanguage : Language("Please")

object PleaseFileType : LanguageFileType(PleaseLanguage) {
    override fun getIcon() = pleaseIcon

    override fun getName() = "Please"

    override fun getDefaultExtension() = ".plz"

    override fun getDescription() = ""
}

sealed class TokenMatchResult {
    object NoMatch : TokenMatchResult() {
        override fun len() = -1
    }
    class Match(var match: String, var type: IElementType) : TokenMatchResult() {
        override fun len() = match.length
    }

    abstract fun len() : Int
}

class TokenMatcher(regex: String, var type: IElementType) {
    private var r = Regex("^$regex")
    fun match(buffer: CharSequence, pos: Int): TokenMatchResult {
        val res = r.find(buffer.subSequence(pos, buffer.length))
        if (res != null) {
            return TokenMatchResult.Match(res.value, type)
        }
        return TokenMatchResult.NoMatch
    }
}

object EOL : IElementType("EOL", PleaseLanguage)
object DOC_COMMENT : IElementType("DOC_COMMENT", PleaseLanguage)

val matchers = listOf(
    // Operators
    TokenMatcher("\"\"\"", DOC_COMMENT),
    TokenMatcher("\\+", PleaseTypes.PLUS),
    TokenMatcher("-", PleaseTypes.MINUS),
    TokenMatcher("\\*", PleaseTypes.TIMES),
    TokenMatcher("/", PleaseTypes.DIVIDE),
    TokenMatcher("%", PleaseTypes.PERCENT),
    TokenMatcher("<", PleaseTypes.LEFT_CHEV),
    TokenMatcher(">", PleaseTypes.RIGHT_CHEV),
    TokenMatcher("and", PleaseTypes.AND),
    TokenMatcher("or", PleaseTypes.OR),
    TokenMatcher("is", PleaseTypes.IS),
    TokenMatcher("not", PleaseTypes.NOT),
    TokenMatcher("in", PleaseTypes.IN),
    TokenMatcher("==", PleaseTypes.EQUALS),
    TokenMatcher("!=", PleaseTypes.NOT_EQUALS),
    TokenMatcher(">=", PleaseTypes.GTE),
    TokenMatcher("<=", PleaseTypes.LTE),

    // Keywords
    TokenMatcher("=", PleaseTypes.EQ),
    TokenMatcher(":", PleaseTypes.COLON),
    TokenMatcher(",", PleaseTypes.COMMA),
    TokenMatcher("pass", PleaseTypes.PASS),
    TokenMatcher("continue", PleaseTypes.CONTINUE),
    TokenMatcher("def", PleaseTypes.DEF),
    TokenMatcher("False", PleaseTypes.FALSE_LIT),
    TokenMatcher("True", PleaseTypes.TRUE_LIT),

    // Syntax
    TokenMatcher("\\[", PleaseTypes.LBRACK),
    TokenMatcher("\\]", PleaseTypes.RBRACK),
    TokenMatcher("\\{", PleaseTypes.LBRACE),
    TokenMatcher("\\}", PleaseTypes.RBRACE),
    TokenMatcher("\\(", PleaseTypes.LPAREN),
    TokenMatcher("\\)", PleaseTypes.RPAREN),
    TokenMatcher("\\|", PleaseTypes.PIPE),

    // Regex matchers
    TokenMatcher("(#)[^\\r\\n]*", PleaseTypes.COMMENT),
    TokenMatcher("(\n|\r|\r\n)", EOL),
    TokenMatcher(" +", TokenType.WHITE_SPACE),
    TokenMatcher("([a-zA-Z]+|_)([a-zA-Z]|[0-9]|_)*", PleaseTypes.IDENT),
    TokenMatcher("[0-9]+", PleaseTypes.INT_LIT),
    TokenMatcher("('([^'\\\\]|\\\\.)*'|\\\"([^\\\"\\\\]|\\\\.)*\\\")", PleaseTypes.STR_LIT)
)


class PleaseLexer : LexerBase() {
    private lateinit var buffer: CharSequence
    // The offset (from the beginning of the buffer) to stop indexing at
    private var endOffset = 0

    private var pos = 0
    private var state = 0

    private var currentToken: IElementType? = null
    private var currentTokenStart = 0
    private var currentTokenLength : Int = 0
    private var indentations = mutableListOf(0)

    // longestMatch will try and match any of the token types returning the longest match. If there's a tie, the first
    // match is returned. This means keywords match before identifiers, as keyword appear earlier in the list.
    private fun longestMatch() : TokenMatchResult {
        var match : TokenMatchResult = TokenMatchResult.NoMatch
        for (matcher in matchers) {
            val m = matcher.match(buffer, pos)
            if (m.len() > match.len()) {
                match = m
            }
        }
        return match
    }

    // locateToken finds a token at the current position
    private fun locateToken() {
        if(currentToken != null) {
            return
        }
        currentTokenStart = pos

        val m = longestMatch()
        if (m is TokenMatchResult.Match) {
            currentToken = m.type
            currentTokenLength = m.len()
        } else {
            currentToken = TokenType.BAD_CHARACTER
            currentTokenLength = 1
        }

        // Check we haven't exceeded the end of the buffer
        if(pos + currentTokenLength > endOffset) {
            currentTokenLength = 0
            currentToken = null
            return
        }


        // Emit a block start/end if we changed indentation
        if(currentToken == EOL) {
            val indentation = calculateIndent(pos+1) // +1 to exclude the new line itself
            // If we couldn't determine indentation (meaning the line was blank) don't generate a new block
            if (indentation == -1) {
                return
            }

            currentTokenLength = indentation+1 // +1 to include the new line

            // If we've indented further, add to the stack
            if (indentation > indentations.last()) {
                indentations.add(indentation)
                currentToken = PleaseTypes.OPEN_BLOCK
            }
            // Otherwise try and match the last level of indentation
            else if(indentations.size >= 2 && indentation == indentations[indentations.size-2]) {
                indentations.removeAt(indentations.lastIndex)
                currentToken = PleaseTypes.CLOSE_BLOCK
            }
        }
    }

    private fun calculateIndent(from : Int) : Int {
        var i = from
        while (true) {
            if(i == buffer.length || i == endOffset) {
                return -1 // Reached the end of the buffer, no match
            } else if(buffer[i] == ' ') {
                i++ // found another space, increment indentation
            } else if(buffer[i] == '\n' || buffer[i] == '\r') {
                return -1 // empty lines don't count towards blocks, no match
            } else {
                return i-from // return the difference between the start of the
            }
        }
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.pos = startOffset
        currentToken = null
        currentTokenStart = startOffset
        this.endOffset = if (this.endOffset <= 0) buffer.length else endOffset
        this.buffer = buffer
        this.state = initialState
        indentations = mutableListOf(0)

        locateToken()
    }


    override fun getState(): Int {
        return state
    }

    override fun getBufferEnd(): Int {
        return endOffset
    }

    override fun getBufferSequence(): CharSequence {
        return buffer
    }

    override fun advance() {
        locateToken() // make sure we've set the current token; it's probably already located, but I don't trust this stateful API
        pos += currentTokenLength
        currentToken = null
        if (pos < endOffset) {
            locateToken() // locate the next token
        }
    }


    override fun getTokenType(): IElementType? {
        locateToken()
        return currentToken
    }

    override fun getTokenStart(): Int {
        locateToken()
        return currentTokenStart
    }

    override fun getTokenEnd(): Int {
        locateToken()
        return currentTokenStart + currentTokenLength
    }
}

class PleaseFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, PleaseLanguage) {
    var pkg = ""
    override fun getFileType(): FileType {
        return PleaseFileType
    }

    override fun toString(): String {
        return "Please File"
    }

    fun getPleasePackage() : String {
        if (pkg != "") {
            return pkg
        }
        var dir = Path.of(virtualFile.path).parent
        val path = mutableListOf<String>()
        while(true) {
            if(dir == null){
                throw RuntimeException("Could not locate .plzconfig")
            }

            val dirFile = dir.toFile()
            if (dir.toFile().list()!!.find { it == ".plzconfig" } != null) {
                pkg = path.joinToString("/")
                return pkg
            } else {
                path.add(0, dirFile.name)
                dir = dir.parent
            }
        }
    }
}

class PleaseParserDefinition : ParserDefinition {
    private val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE, EOL)
    private val COMMENTS = TokenSet.create(PleaseTypes.COMMENT)
    private val STRINGS = TokenSet.create(PleaseTypes.STR_LIT)

    val FILE = IFileElementType(PleaseLanguage)

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

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): SpaceRequirements {
        return SpaceRequirements.MAY
    }

    override fun createElement(node: ASTNode?): PsiElement {
        return PleaseTypes.Factory.createElement(node)
    }
}

class PleaseSyntaxHighlighter : SyntaxHighlighterBase() {
    private val colors = mapOf(
        TokenType.BAD_CHARACTER to HighlighterColors.BAD_CHARACTER,
        PleaseTypes.COLON to DefaultLanguageHighlighterColors.SEMICOLON,
        PleaseTypes.COMMA to DefaultLanguageHighlighterColors.COMMA,
        PleaseTypes.COMMENT to DefaultLanguageHighlighterColors.LINE_COMMENT,
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
        TokenType.BAD_CHARACTER.to(TokenType.CODE_FRAGMENT)
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

class PleaseSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(
        project: Project?,
        virtualFile: VirtualFile?
    ): SyntaxHighlighter {
        return PleaseSyntaxHighlighter()
    }
}


class PleaseLineMarkerProvider : RunLineMarkerContributor() {

    // getInfo needs to apply the run info to the LeafPsiElement as that's what intellij demands. It looks for the
    // IDENT of the function call and applies the run actions to that.
    override fun getInfo(element: PsiElement): Info? {
        if(element !is LeafPsiElement) {
            return null
        }
        // We're looking for the IDENT of the function call here, not any of the other tokens
        if(element.elementType != PleaseTypes.IDENT) {
            return null
        }

        val parent = element.parent
        if (parent !is PleaseFunctionCall) {
            return null
        }

        val name = parent.functionCallParamList.find { it.ident?.text == "name" }?.expression?.value?.strLit?.text
        val file = element.containingFile
        if (name != null && file is PleaseFile) {
            val target = "//${file.getPleasePackage()}:${name.trim { it == '\"'}}"
            return Info(
                AllIcons.Actions.Execute, Function { "run $target" },
                PleaseAction(element.project, "run", target, AllIcons.Actions.Execute),
                PleaseAction(element.project, "test", target, AllIcons.Actions.Execute),
                PleaseAction(element.project, "build", target, AllIcons.Actions.Compile),
                PleaseAction(element.project, "debug", target, AllIcons.Actions.StartDebugger)
            )
        }
        return null
    }

}

class PleaseAction(private val project: Project, private val action : String, private val target : String, icon : Icon) : AnAction({ "plz $action $target" }, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        val mgr = RunManager.getInstance(project) as RunManagerImpl
        val runConfig = PleaseRunConfiguration(project, PleaseRunConfigurationType.Factory(PleaseRunConfigurationType()), target, action)
        runConfig.name = "plz $action $target"
        val config = RunnerAndConfigurationSettingsImpl(mgr, runConfig)

        mgr.addConfiguration(config)

        ProgramRunnerUtil.executeConfiguration(config, DefaultRunExecutor())

    }
}

class PleaseRunConfigurationType : ConfigurationTypeBase("PleaseRunConfigurationType", "Please", "Run a please action on a target", pleaseIcon) {
    class Factory(type : PleaseRunConfigurationType) : ConfigurationFactory(type) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return PleaseRunConfiguration(project, this, "//some:target", "build")
        }

        override fun getId(): String {
            return "PleaseRunConfigurationType.Factory"
        }
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(Factory(this))
    }
}

class PleaseRunConfigurationSettings() : SettingsEditor<PleaseRunConfiguration>() {
    private val target = JTextField("//some:target")

    override fun resetEditorFrom(s: PleaseRunConfiguration) {
        target.text = s.target
    }

    override fun createEditor(): JComponent {
        return target
    }

    override fun applyEditorTo(s: PleaseRunConfiguration) {
        s.target = target.text
    }

}

class PleaseRunConfiguration(project: Project, factory: ConfigurationFactory, var target: String, var action: String) : RunConfigurationBase<PleaseLaunchState>(project, factory, "Please"){
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PleaseRunConfigurationSettings()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return PleaseLaunchState(action, target,  project.basePath!!, environment)
    }
}

class PleaseLaunchState(private var action: String, private var target: String, private var projectRoot : String, environment: ExecutionEnvironment) : CommandLineState(environment) {
    override fun startProcess(): ProcessHandler {
        val cmd = GeneralCommandLine("plz", action, target)
        cmd.setWorkDirectory(projectRoot)
        cmd.withRedirectErrorStream(true)
        return ProcessHandlerFactoryImpl.getInstance().createColoredProcessHandler(cmd)
    }
}

class PleaseLSPPreloadActivity : PreloadingActivity() {
    override fun preload(indicator: ProgressIndicator) {
        // TODO(jpoole): use please to download this via //:_please:lsp
        val home = System.getProperty("user.home")

        IntellijLanguageClient.addServerDefinition(
            PleaseFileType,
            RawCommandServerDefinition(
                "plz",
                arrayOf("$home/.please/build_langserver")
            )
        )
    }
}