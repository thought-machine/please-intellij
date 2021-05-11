package please.parser

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.intellij.plugin.please.*
import org.intellij.plugin.please.psi.PleaseTypes
import please.PleaseLanguage

object EOL : IElementType("EOL", PleaseLanguage)

private val MATCHERS = listOf(
    // Operators
    DocCommentMatcher,
    BlockStartMatcher,
    StringMatcher("+", PleaseTypes.PLUS),
    StringMatcher("-", PleaseTypes.MINUS),
    StringMatcher("*", PleaseTypes.TIMES),
    StringMatcher("/", PleaseTypes.DIVIDE),
    StringMatcher("%", PleaseTypes.PERCENT),
    StringMatcher("<", PleaseTypes.LEFT_CHEV),
    StringMatcher(">", PleaseTypes.RIGHT_CHEV),
    StringMatcher("and", PleaseTypes.AND),
    StringMatcher("or", PleaseTypes.OR),
    StringMatcher("is", PleaseTypes.IS),
    StringMatcher("if", PleaseTypes.IF),
    StringMatcher("elif", PleaseTypes.ELIF),
    StringMatcher("else", PleaseTypes.ELSE),
    StringMatcher("not", PleaseTypes.NOT),
    StringMatcher("in", PleaseTypes.IN),
    StringMatcher("==", PleaseTypes.EQUALS),
    StringMatcher("!=", PleaseTypes.NOT_EQUALS),
    StringMatcher(">=", PleaseTypes.GTE),
    StringMatcher("<=", PleaseTypes.LTE),

    // Keywords
    StringMatcher("=", PleaseTypes.EQ),
    StringMatcher(":", PleaseTypes.COLON),
    StringMatcher(",", PleaseTypes.COMMA),
    StringMatcher("pass", PleaseTypes.PASS),
    StringMatcher("continue", PleaseTypes.CONTINUE),
    StringMatcher("def", PleaseTypes.DEF),
    StringMatcher("False", PleaseTypes.FALSE_LIT),
    StringMatcher("True", PleaseTypes.TRUE_LIT),

    // Syntax
    RegexMatcher("\\[", PleaseTypes.LBRACK),
    RegexMatcher("\\]", PleaseTypes.RBRACK),
    RegexMatcher("\\{", PleaseTypes.LBRACE),
    RegexMatcher("\\}", PleaseTypes.RBRACE),
    RegexMatcher("\\(", PleaseTypes.LPAREN),
    RegexMatcher("\\)", PleaseTypes.RPAREN),
    RegexMatcher("\\|", PleaseTypes.PIPE),

    // Regex matchers
    RegexMatcher("(#)[^\\r\\n]*", PleaseTypes.COMMENT),
    RegexMatcher("(\n|\r|\r\n)", EOL),
    RegexMatcher(" +", TokenType.WHITE_SPACE),
    RegexMatcher("([a-zA-Z]+|_)([a-zA-Z]|[0-9]|_)*", PleaseTypes.IDENT),
    RegexMatcher("[0-9]+", PleaseTypes.INT_LIT),
    RegexMatcher("('([^'\\\\]|\\\\.)*'|\\\"([^\\\"\\\\]|\\\\.)*\\\")", PleaseTypes.STR_LIT)
)


class PleaseLexer : LexerBase() {
    private lateinit var buffer: CharSequence
    // The offset (from the beginning of the buffer) to stop indexing at
    private var endOffset = 0

    private var pos = 0

    private var currentToken: IElementType? = null
    private var currentTokenStart = 0
    private var currentTokenLength : Int = 0
    private var indentations = mutableListOf(0)

    // longestMatch will try and match any of the token types returning the longest match. If there's a tie, the first
    // match is returned. This means keywords match before identifiers, as keyword appear earlier in the list.
    private fun longestMatch() : TokenMatchResult {
        var match : TokenMatchResult = TokenMatchResult.NoMatch
        for (matcher in MATCHERS) {
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
            // If we reach the end of the file, just return. We handle closing off open blocks later.
            if (indentation == -1) {
                return
            }

            if(indentations.size >= 2 && indentation == indentations[indentations.size-2]) {
                indentations.removeAt(indentations.lastIndex)
                currentToken = PleaseTypes.CLOSE_BLOCK
            }
        } else if(currentToken == PleaseTypes.OPEN_BLOCK) {
            indentations.add(calculateIndent(pos+1)) // +1 to exclude the new line itself
        }
    }

    private fun calculateIndent(from : Int) : Int {
        var i = from
        var indentation = 0
        while (true) {
            if(i == buffer.length || i == endOffset) {
                return -1 // Reached the end of the buffer, no match
            } else if(buffer[i] == ' ') {
                i++ // found another space, increment indentation
                indentation++
            } else if(buffer[i] == '\n' || buffer[i] == '\r') {
                indentation = 0
                i++
            } else {
                return indentation
            }
        }
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.pos = startOffset
        currentToken = null
        currentTokenStart = startOffset
        this.endOffset = if (this.endOffset <= 0) buffer.length else endOffset
        this.buffer = buffer
        indentations = mutableListOf(0)

        locateToken()
    }


    override fun getState(): Int {
        return indentations.size
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
        } else if(indentations.size > 1) {
            currentToken = PleaseTypes.CLOSE_BLOCK
            currentTokenLength = 0
            currentTokenStart = pos
            indentations.removeAt(0)
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

sealed class TokenMatchResult {
    object NoMatch : TokenMatchResult() {
        override fun len() = -1
    }

    class Match(var match: String, var type: IElementType) : TokenMatchResult() {
        override fun len() = match.length
    }

    abstract fun len() : Int
}

interface TokenMatcher {
    fun match(buffer: CharSequence, pos: Int): TokenMatchResult
}

class StringMatcher(private var string: String, private var type: IElementType) : TokenMatcher {
    override fun match(buffer: CharSequence, pos: Int) = when {
        buffer.length < pos + string.length -> {
            TokenMatchResult.NoMatch
        }
        buffer.subSequence(pos, pos+string.length).toString() == string -> {
            TokenMatchResult.Match(string, type)
        }
        else -> {
            TokenMatchResult.NoMatch
        }
    }
}

class RegexMatcher(regex: String, private var type: IElementType) : TokenMatcher {
    private var r = Regex("^$regex")
    override fun match(buffer: CharSequence, pos: Int): TokenMatchResult {
        val res = r.find(buffer.subSequence(pos, buffer.length))
        if (res != null) {
            return TokenMatchResult.Match(res.value, type)
        }
        return TokenMatchResult.NoMatch
    }
}

object DocCommentMatcher : TokenMatcher {
    private const val quotes = "\"\"\""
    private val type = PleaseTypes.DOC_COMMENT //TODO(jpoole): might want to be it's own type
    override fun match(buffer: CharSequence, pos: Int): TokenMatchResult {
        return when {
            buffer.length < pos + quotes.length -> {
                TokenMatchResult.NoMatch
            }
            buffer.subSequence(pos, pos+ quotes.length).toString() == quotes -> {
                readComment(buffer, pos)
            }
            else -> {
                TokenMatchResult.NoMatch
            }
        }
    }

    private fun readComment(buffer: CharSequence, pos: Int) : TokenMatchResult {
        var endPos = pos + quotes.length
        while (endPos + quotes.length < buffer.length) {
            val nextChars = buffer.subSequence(endPos, endPos+ quotes.length)
            if(nextChars.toString() == quotes) {
                return TokenMatchResult.Match(buffer.subSequence(pos, endPos + quotes.length).toString(), type)
            }
            endPos++
        }
        return TokenMatchResult.NoMatch
    }
}

object BlockStartMatcher : TokenMatcher {
    private val type = PleaseTypes.OPEN_BLOCK
    override fun match(buffer: CharSequence, pos: Int): TokenMatchResult {
        var endPos = pos+1
        if(pos < buffer.length && buffer[pos] == ':') {
            while (endPos < buffer.length) {
                when(buffer[endPos]) {
                    ' ' -> endPos++
                    '\n' -> return TokenMatchResult.Match(buffer.substring(pos, endPos + 1), type)
                    else -> return TokenMatchResult.NoMatch
                }
            }
        }
        return TokenMatchResult.NoMatch
    }

}