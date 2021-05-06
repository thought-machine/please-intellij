package org.intellij.plugin.please

import com.intellij.psi.TokenType
import org.intellij.plugin.please.psi.PleaseTypes
import org.junit.Assert.*
import org.junit.Test


class AppTest {
    @Test
    fun testBasicMatch() {
        val res = RegexMatcher("def", PleaseTypes.DEF).match("foo = def foo():\npass", 6)
        if(res is TokenMatchResult.Match) {
            assertEquals("def", res.match)
        } else {
            fail("Failed to match ")
        }
    }

    @Test
    fun testRegexMatch() {
        val res = RegexMatcher("def", PleaseTypes.IDENT).match("foo = def _foo_bar1():\npass", 6)
        assertTrue(res is TokenMatchResult.Match)
    }

    @Test
    fun testLexer() {
        val lexer = PleaseLexer()
        val program  = "foo = def _foo_bar1():\npass"
        lexer.start(program, 0, program.length)

        assertEquals("foo", lexer.tokenText)
        assertEquals(lexer.tokenText, program.subSequence(lexer.tokenStart, lexer.tokenEnd))

        lexer.advance()
        lexer.advance()
        assertEquals("=", lexer.tokenText)
        assertEquals(lexer.tokenText, program.subSequence(lexer.tokenStart, lexer.tokenEnd))
    }

    @Test
    fun testIndent() {
        val lexer = PleaseLexer()
        val program  = "foo:\n    bar\nfoo:    \n    bar"
        lexer.start(program, 0, program.length)
        assertEquals("foo", lexer.tokenText)


        lexer.advance()
        assertEquals(PleaseTypes.OPEN_BLOCK, lexer.tokenType)
        assertEquals(":\n", lexer.tokenText)

        lexer.advance()
        assertEquals(TokenType.WHITE_SPACE, lexer.tokenType)

        lexer.advance()
        assertEquals(PleaseTypes.IDENT, lexer.tokenType)
        assertEquals("bar", lexer.tokenText)

        lexer.advance()
        assertEquals(PleaseTypes.CLOSE_BLOCK, lexer.tokenType)
        assertEquals("\n", lexer.tokenText)

        lexer.advance()
        assertEquals(PleaseTypes.IDENT, lexer.tokenType)
        assertEquals("foo", lexer.tokenText)

        lexer.advance()
        assertEquals(PleaseTypes.OPEN_BLOCK, lexer.tokenType)
        assertEquals(":\n", lexer.tokenText)

        lexer.advance()
        assertEquals(PleaseTypes.IDENT, lexer.tokenType)
        assertEquals("bar", lexer.tokenText)

        lexer.advance()
        assertEquals(PleaseTypes.CLOSE_BLOCK, lexer.tokenType)
    }

    @Test
    fun testStringMatcher() {
        val lexer = PleaseLexer()
        val program  = "def ="
        lexer.start(program, 0, program.length)
        assertEquals("def", lexer.tokenText)
        assertEquals(PleaseTypes.DEF, lexer.tokenType)

        lexer.advance()
        assertEquals(TokenType.WHITE_SPACE, lexer.tokenType)

        lexer.advance()
        assertEquals(PleaseTypes.EQ, lexer.tokenType)
        assertEquals("=", lexer.tokenText)
    }
}