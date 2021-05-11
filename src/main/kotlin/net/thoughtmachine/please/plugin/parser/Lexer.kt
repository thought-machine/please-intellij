package net.thoughtmachine.please.plugin.parser

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import net.thoughtmachine.please.plugin.PleaseLanguage
import net.thoughtmachine.please.plugin.psi.PleaseTypes

// The parser doesn't need to worry about new lines. This is just used by the lexer and is later translated into
// open/close block, or whitespace tokens by the lexer.
object EOL : IElementType("EOL", PleaseLanguage)

// A set of matchers in no particular order.
private val MATCHERS = setOf(
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

/**
 * This is the lexer for the ASP language. Because we care about indentation this is a little more complex than your
 * typical lexer. It keeps a stack of indentations, and emits open and close block tokens when we move between
 * indentation levels.
 *
 * The size of this indentation stack makes up the state of the lexer although this is mostly just so that intellij
 * knows that the lexer hasn't gotten stuck when we're emitting close blocks when we reach the end of the file.
 *
 * The lexer uses the longest match to resolve ambiguity when parsing tokens.
 */
class PleaseLexer : LexerBase() {
    private lateinit var buffer: CharSequence

    // The offset (from the beginning of the buffer) to stop indexing at
    private var endOffset = 0

    private var pos = 0

    private var currentToken: IElementType? = null
    private var currentTokenStart = 0
    private var currentTokenLength : Int = 0

    /**
     * The stack of indentations. We start off at indentation level 0. We push onto this stack when we see a block start
     * i.e. `:\n`.
     *
     * For more information on what we push onto this stack, see calculateIdent() below.
     */
    private var indentations = mutableListOf(0)

    /**
     * longestMatch will try and match any of the token types returning the longest match. If there's a tie, the first
     * match is returned. This means keywords match before identifiers, as keyword appear earlier in the list.
     */
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

    /**
     * locateToken finds a token at the current position, and sets currentToken, currentTokenStart and
     * currentTokenLength accordingly. If currentToken is not null, then this will simply return as the token is already
     * located.
     */
    private fun locateToken() {
        if(currentToken != null) {
            return // Don't relocate the token if we've already found one.
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
            // Otherwise if we encountered an open block token, we need to push the new indentation to the stack.
            indentations.add(calculateIndent(pos+1)) // +1 to exclude the new line itself
        }
    }

    /**
     * calculateIdent will calculate the indentation level at a given position. This is called after we see newlines
     * to figure out if we should emit a close block token, as well as after `:\n` to emit the open block token as well
     * as update the ident stack.
     *
     * The indentation is calculated by counting the number of spaces until the first non-whitespace character on the
     * line. If there's no non-whitespace characters on the line, then we try again on the next line until we find a
     * non-blank line.
     */
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
        // We emit close blocks for all open blocks when we read EOF. This makes sure that intellij is aware something
        // is changing and the lexer hasn't just locked up.
        return indentations.size
    }

    override fun getBufferEnd(): Int {
        return endOffset
    }

    override fun getBufferSequence(): CharSequence {
        return buffer
    }

    /**
     * advance will find the next token in the stream, and set currentToken, currentTokenLength, currentTokenStart and
     * pos accordingly.
     *
     * This function will also emit close blocks when we reach the end of the buffer for each block we currently have
     * open.
     */
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

/**
 * TokenMatchResult contains the possible outcomes of trying to match a token.
 */
sealed class TokenMatchResult {
    object NoMatch : TokenMatchResult() {
        override fun len() = -1
    }

    class Match(var match: String, var type: IElementType) : TokenMatchResult() {
        override fun len() = match.length
    }

    abstract fun len() : Int
}

/**
 * A TokenMatcher matches a token and produces a TokenResult (which can be TokenResult.NoMatch if there's no match)
 */
interface TokenMatcher {
    fun match(buffer: CharSequence, pos: Int): TokenMatchResult
}

/**
 * Matches strings literally
 */
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

/**
 * Matches based on a regex pattern
 */
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

/**
 * Matches asp "doc comments" i.e. strings that start with `"""`
 */
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

/**
 * Matches block starts i.e. a comma followed by a newline
 */
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