// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugin.please;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import org.intellij.plugin.please.psi.PleaseTypes;
import com.intellij.psi.TokenType;

%%

%class PleaseLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

PASS="pass"
CONTINUE="continue"
DEF="def"
PIPE="|"
LPAREN="("
RPAREN=")"
COMMA=","
COLON=":"
WHITE_SPACE=[\ \t\f]
LINE_COMMENT=("#")[^\r\n]*
IDENT=([a-zA-Z]+|_) ([a-zA-Z] | [0-9] | "_")*
EOL=[\r|\n|\r\n]
INT_LIT=[0-9]+
STR_LIT=('([^'\\]|\\.)*'|\"([^\"\\]|\\.)*\")

%%

{LINE_COMMENT}                              { }
{EOL}                                       { return PleaseTypes.EOL; }
{WHITE_SPACE}+                              { }

// keywords
{PASS}                                      { return PleaseTypes.PASS; }
{CONTINUE}                                  { return PleaseTypes.CONTINUE; }
{DEF}                                       { return PleaseTypes.DEF; }

// Syntax
{LPAREN}                                    { return PleaseTypes.LPAREN; }
{RPAREN}                                    { return PleaseTypes.RPAREN; }
{COLON}                                     { return PleaseTypes.COLON; }
{COMMA}                                     { return PleaseTypes.COMMA; }
{PIPE}                                      { return PleaseTypes.PIPE; }

// expressions
{IDENT}                                     { return PleaseTypes.IDENT; }
{INT_LIT}                                   { return PleaseTypes.INT_LIT; }
{STR_LIT}                                   { return PleaseTypes.STR_LIT; }

[^]                                         { return TokenType.BAD_CHARACTER; }