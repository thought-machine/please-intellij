// This is a generated file. Not intended for manual editing.
package net.thoughtmachine.please.plugin.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static net.thoughtmachine.please.plugin.psi.PleaseTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class PleaseParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return pleaseFile(b, l + 1);
  }

  /* ********************************************************** */
  // IDENT (COMMA IDENT)* EQ expression
  public static boolean assignment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignment")) return false;
    if (!nextTokenIs(b, IDENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENT);
    r = r && assignment_1(b, l + 1);
    r = r && consumeToken(b, EQ);
    r = r && expression(b, l + 1);
    exit_section_(b, m, ASSIGNMENT, r);
    return r;
  }

  // (COMMA IDENT)*
  private static boolean assignment_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignment_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!assignment_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "assignment_1", c)) break;
    }
    return true;
  }

  // COMMA IDENT
  private static boolean assignment_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignment_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, COMMA, IDENT);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // TRUE_LIT | FALSE_LIT
  public static boolean boolLit(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "boolLit")) return false;
    if (!nextTokenIs(b, "<bool lit>", FALSE_LIT, TRUE_LIT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BOOL_LIT, "<bool lit>");
    r = consumeToken(b, TRUE_LIT);
    if (!r) r = consumeToken(b, FALSE_LIT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LBRACE [STR_LIT COLON expression [COMMA STR_LIT COLON expression]] COMMA? CLOSE_BLOCK* RBRACE
  public static boolean dictLit(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dictLit")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && dictLit_1(b, l + 1);
    r = r && dictLit_2(b, l + 1);
    r = r && dictLit_3(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, DICT_LIT, r);
    return r;
  }

  // [STR_LIT COLON expression [COMMA STR_LIT COLON expression]]
  private static boolean dictLit_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dictLit_1")) return false;
    dictLit_1_0(b, l + 1);
    return true;
  }

  // STR_LIT COLON expression [COMMA STR_LIT COLON expression]
  private static boolean dictLit_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dictLit_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, STR_LIT, COLON);
    r = r && expression(b, l + 1);
    r = r && dictLit_1_0_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [COMMA STR_LIT COLON expression]
  private static boolean dictLit_1_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dictLit_1_0_3")) return false;
    dictLit_1_0_3_0(b, l + 1);
    return true;
  }

  // COMMA STR_LIT COLON expression
  private static boolean dictLit_1_0_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dictLit_1_0_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, COMMA, STR_LIT, COLON);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean dictLit_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dictLit_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // CLOSE_BLOCK*
  private static boolean dictLit_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dictLit_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CLOSE_BLOCK)) break;
      if (!empty_element_parsed_guard_(b, "dictLit_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // [ MINUS | NOT ] value [operator expression] [ IF expression ELSE expression ]
  public static boolean expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPRESSION, "<expression>");
    r = expression_0(b, l + 1);
    r = r && value(b, l + 1);
    r = r && expression_2(b, l + 1);
    r = r && expression_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // [ MINUS | NOT ]
  private static boolean expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_0")) return false;
    expression_0_0(b, l + 1);
    return true;
  }

  // MINUS | NOT
  private static boolean expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_0_0")) return false;
    boolean r;
    r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, NOT);
    return r;
  }

  // [operator expression]
  private static boolean expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_2")) return false;
    expression_2_0(b, l + 1);
    return true;
  }

  // operator expression
  private static boolean expression_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = operator(b, l + 1);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [ IF expression ELSE expression ]
  private static boolean expression_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_3")) return false;
    expression_3_0(b, l + 1);
    return true;
  }

  // IF expression ELSE expression
  private static boolean expression_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IF);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, ELSE);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENT LPAREN [ functionCallParam (COMMA functionCallParam)* ] COMMA? CLOSE_BLOCK* RPAREN
  public static boolean functionCall(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCall")) return false;
    if (!nextTokenIs(b, IDENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENT, LPAREN);
    r = r && functionCall_2(b, l + 1);
    r = r && functionCall_3(b, l + 1);
    r = r && functionCall_4(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, FUNCTION_CALL, r);
    return r;
  }

  // [ functionCallParam (COMMA functionCallParam)* ]
  private static boolean functionCall_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCall_2")) return false;
    functionCall_2_0(b, l + 1);
    return true;
  }

  // functionCallParam (COMMA functionCallParam)*
  private static boolean functionCall_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCall_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = functionCallParam(b, l + 1);
    r = r && functionCall_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA functionCallParam)*
  private static boolean functionCall_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCall_2_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!functionCall_2_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "functionCall_2_0_1", c)) break;
    }
    return true;
  }

  // COMMA functionCallParam
  private static boolean functionCall_2_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCall_2_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && functionCallParam(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean functionCall_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCall_3")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // CLOSE_BLOCK*
  private static boolean functionCall_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCall_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CLOSE_BLOCK)) break;
      if (!empty_element_parsed_guard_(b, "functionCall_4", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // expression | IDENT "=" expression
  public static boolean functionCallParam(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCallParam")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION_CALL_PARAM, "<function call param>");
    r = expression(b, l + 1);
    if (!r) r = functionCallParam_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // IDENT "=" expression
  private static boolean functionCallParam_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCallParam_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENT, EQ);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // DEF IDENT LPAREN [ functionDefParam (COMMA functionDefParam)* ] COMMA? CLOSE_BLOCK* RPAREN OPEN_BLOCK statement+ CLOSE_BLOCK
  public static boolean functionDef(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDef")) return false;
    if (!nextTokenIs(b, DEF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, DEF, IDENT, LPAREN);
    r = r && functionDef_3(b, l + 1);
    r = r && functionDef_4(b, l + 1);
    r = r && functionDef_5(b, l + 1);
    r = r && consumeTokens(b, 0, RPAREN, OPEN_BLOCK);
    r = r && functionDef_8(b, l + 1);
    r = r && consumeToken(b, CLOSE_BLOCK);
    exit_section_(b, m, FUNCTION_DEF, r);
    return r;
  }

  // [ functionDefParam (COMMA functionDefParam)* ]
  private static boolean functionDef_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDef_3")) return false;
    functionDef_3_0(b, l + 1);
    return true;
  }

  // functionDefParam (COMMA functionDefParam)*
  private static boolean functionDef_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDef_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = functionDefParam(b, l + 1);
    r = r && functionDef_3_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA functionDefParam)*
  private static boolean functionDef_3_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDef_3_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!functionDef_3_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "functionDef_3_0_1", c)) break;
    }
    return true;
  }

  // COMMA functionDefParam
  private static boolean functionDef_3_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDef_3_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && functionDefParam(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean functionDef_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDef_4")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // CLOSE_BLOCK*
  private static boolean functionDef_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDef_5")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CLOSE_BLOCK)) break;
      if (!empty_element_parsed_guard_(b, "functionDef_5", c)) break;
    }
    return true;
  }

  // statement+
  private static boolean functionDef_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDef_8")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statement(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "functionDef_8", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENT [COLON type [PIPE type+] ] [ EQ expression]
  public static boolean functionDefParam(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDefParam")) return false;
    if (!nextTokenIs(b, IDENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENT);
    r = r && functionDefParam_1(b, l + 1);
    r = r && functionDefParam_2(b, l + 1);
    exit_section_(b, m, FUNCTION_DEF_PARAM, r);
    return r;
  }

  // [COLON type [PIPE type+] ]
  private static boolean functionDefParam_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDefParam_1")) return false;
    functionDefParam_1_0(b, l + 1);
    return true;
  }

  // COLON type [PIPE type+]
  private static boolean functionDefParam_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDefParam_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && type(b, l + 1);
    r = r && functionDefParam_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [PIPE type+]
  private static boolean functionDefParam_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDefParam_1_0_2")) return false;
    functionDefParam_1_0_2_0(b, l + 1);
    return true;
  }

  // PIPE type+
  private static boolean functionDefParam_1_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDefParam_1_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PIPE);
    r = r && functionDefParam_1_0_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // type+
  private static boolean functionDefParam_1_0_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDefParam_1_0_2_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = type(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!type(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "functionDefParam_1_0_2_0_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // [ EQ expression]
  private static boolean functionDefParam_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDefParam_2")) return false;
    functionDefParam_2_0(b, l + 1);
    return true;
  }

  // EQ expression
  private static boolean functionDefParam_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDefParam_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EQ);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IF expression OPEN_BLOCK statement+ ( ELIF expression OPEN_BLOCK statement+ )* [ELSE expression OPEN_BLOCK statement+ ]
  public static boolean ifStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement")) return false;
    if (!nextTokenIs(b, IF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IF);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, OPEN_BLOCK);
    r = r && ifStatement_3(b, l + 1);
    r = r && ifStatement_4(b, l + 1);
    r = r && ifStatement_5(b, l + 1);
    exit_section_(b, m, IF_STATEMENT, r);
    return r;
  }

  // statement+
  private static boolean ifStatement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statement(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ifStatement_3", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // ( ELIF expression OPEN_BLOCK statement+ )*
  private static boolean ifStatement_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ifStatement_4_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ifStatement_4", c)) break;
    }
    return true;
  }

  // ELIF expression OPEN_BLOCK statement+
  private static boolean ifStatement_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ELIF);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, OPEN_BLOCK);
    r = r && ifStatement_4_0_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // statement+
  private static boolean ifStatement_4_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_4_0_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statement(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ifStatement_4_0_3", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // [ELSE expression OPEN_BLOCK statement+ ]
  private static boolean ifStatement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_5")) return false;
    ifStatement_5_0(b, l + 1);
    return true;
  }

  // ELSE expression OPEN_BLOCK statement+
  private static boolean ifStatement_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ELSE);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, OPEN_BLOCK);
    r = r && ifStatement_5_0_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // statement+
  private static boolean ifStatement_5_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_5_0_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statement(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ifStatement_5_0_3", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LBRACK [expression [ (COMMA expression)* ] ] COMMA? CLOSE_BLOCK* RBRACK
  public static boolean listLit(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "listLit")) return false;
    if (!nextTokenIs(b, LBRACK)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACK);
    r = r && listLit_1(b, l + 1);
    r = r && listLit_2(b, l + 1);
    r = r && listLit_3(b, l + 1);
    r = r && consumeToken(b, RBRACK);
    exit_section_(b, m, LIST_LIT, r);
    return r;
  }

  // [expression [ (COMMA expression)* ] ]
  private static boolean listLit_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "listLit_1")) return false;
    listLit_1_0(b, l + 1);
    return true;
  }

  // expression [ (COMMA expression)* ]
  private static boolean listLit_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "listLit_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1);
    r = r && listLit_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [ (COMMA expression)* ]
  private static boolean listLit_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "listLit_1_0_1")) return false;
    listLit_1_0_1_0(b, l + 1);
    return true;
  }

  // (COMMA expression)*
  private static boolean listLit_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "listLit_1_0_1_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!listLit_1_0_1_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "listLit_1_0_1_0", c)) break;
    }
    return true;
  }

  // COMMA expression
  private static boolean listLit_1_0_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "listLit_1_0_1_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMA?
  private static boolean listLit_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "listLit_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  // CLOSE_BLOCK*
  private static boolean listLit_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "listLit_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CLOSE_BLOCK)) break;
      if (!empty_element_parsed_guard_(b, "listLit_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // PLUS | MINUS | TIMES | DIVIDE | PERCENT | LEFT_CHEV | RIGHT_CHEV | AND | OR |
  //             IS | IS NOT | IN | NOT IN | EQUALS | NOT_EQUALS | GTE | LTE | PIPE
  public static boolean operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "operator")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, OPERATOR, "<operator>");
    r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, TIMES);
    if (!r) r = consumeToken(b, DIVIDE);
    if (!r) r = consumeToken(b, PERCENT);
    if (!r) r = consumeToken(b, LEFT_CHEV);
    if (!r) r = consumeToken(b, RIGHT_CHEV);
    if (!r) r = consumeToken(b, AND);
    if (!r) r = consumeToken(b, OR);
    if (!r) r = consumeToken(b, IS);
    if (!r) r = parseTokens(b, 0, IS, NOT);
    if (!r) r = consumeToken(b, IN);
    if (!r) r = parseTokens(b, 0, NOT, IN);
    if (!r) r = consumeToken(b, EQUALS);
    if (!r) r = consumeToken(b, NOT_EQUALS);
    if (!r) r = consumeToken(b, GTE);
    if (!r) r = consumeToken(b, LTE);
    if (!r) r = consumeToken(b, PIPE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (statement)*
  static boolean pleaseFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pleaseFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (!pleaseFile_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "pleaseFile", c)) break;
    }
    return true;
  }

  // (statement)
  private static boolean pleaseFile_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pleaseFile_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // functionDef | expression | assignment | ifStatement | PASS | CONTINUE
  public static boolean statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STATEMENT, "<statement>");
    r = functionDef(b, l + 1);
    if (!r) r = expression(b, l + 1);
    if (!r) r = assignment(b, l + 1);
    if (!r) r = ifStatement(b, l + 1);
    if (!r) r = consumeToken(b, PASS);
    if (!r) r = consumeToken(b, CONTINUE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // IDENT
  public static boolean type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type")) return false;
    if (!nextTokenIs(b, IDENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENT);
    exit_section_(b, m, TYPE, r);
    return r;
  }

  /* ********************************************************** */
  // functionCall | INT_LIT | STR_LIT | listLit | dictLit | boolLit
  public static boolean value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VALUE, "<value>");
    r = functionCall(b, l + 1);
    if (!r) r = consumeToken(b, INT_LIT);
    if (!r) r = consumeToken(b, STR_LIT);
    if (!r) r = listLit(b, l + 1);
    if (!r) r = dictLit(b, l + 1);
    if (!r) r = boolLit(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
