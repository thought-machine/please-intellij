// This is a generated file. Not intended for manual editing.
package org.intellij.plugin.please.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.intellij.plugin.please.psi.impl.*;

public interface PleaseTypes {

  IElementType ASSIGNMENT = new PleaseElementType("ASSIGNMENT");
  IElementType BOOL_LIT = new PleaseElementType("BOOL_LIT");
  IElementType DICT_LIT = new PleaseElementType("DICT_LIT");
  IElementType EXPRESSION = new PleaseElementType("EXPRESSION");
  IElementType FUNCTION_CALL = new PleaseElementType("FUNCTION_CALL");
  IElementType FUNCTION_CALL_PARAM = new PleaseElementType("FUNCTION_CALL_PARAM");
  IElementType FUNCTION_DEF = new PleaseElementType("FUNCTION_DEF");
  IElementType FUNCTION_DEF_PARAM = new PleaseElementType("FUNCTION_DEF_PARAM");
  IElementType LIST_LIT = new PleaseElementType("LIST_LIT");
  IElementType OPERATOR = new PleaseElementType("OPERATOR");
  IElementType STATEMENT = new PleaseElementType("STATEMENT");
  IElementType TYPE = new PleaseElementType("TYPE");
  IElementType VALUE = new PleaseElementType("VALUE");

  IElementType AND = new PleaseTokenType("AND");
  IElementType CLOSE_BLOCK = new PleaseTokenType("CLOSE_BLOCK");
  IElementType COLON = new PleaseTokenType(":");
  IElementType COMMA = new PleaseTokenType(",");
  IElementType COMMENT = new PleaseTokenType("COMMENT");
  IElementType CONTINUE = new PleaseTokenType("continue");
  IElementType DEF = new PleaseTokenType("def");
  IElementType DIVIDE = new PleaseTokenType("DIVIDE");
  IElementType DOC_COMMENT = new PleaseTokenType("DOC_COMMENT");
  IElementType ELSE = new PleaseTokenType("ELSE");
  IElementType EQ = new PleaseTokenType("=");
  IElementType EQUALS = new PleaseTokenType("EQUALS");
  IElementType FALSE_LIT = new PleaseTokenType("False");
  IElementType GTE = new PleaseTokenType("GTE");
  IElementType IDENT = new PleaseTokenType("IDENT");
  IElementType IF = new PleaseTokenType("IF");
  IElementType IN = new PleaseTokenType("IN");
  IElementType INT_LIT = new PleaseTokenType("INT_LIT");
  IElementType IS = new PleaseTokenType("IS");
  IElementType LBRACE = new PleaseTokenType("{");
  IElementType LBRACK = new PleaseTokenType("[");
  IElementType LEFT_CHEV = new PleaseTokenType("LEFT_CHEV");
  IElementType LPAREN = new PleaseTokenType("(");
  IElementType LTE = new PleaseTokenType("LTE");
  IElementType MINUS = new PleaseTokenType("-");
  IElementType NOT = new PleaseTokenType("NOT");
  IElementType NOT_EQUALS = new PleaseTokenType("NOT_EQUALS");
  IElementType OPEN_BLOCK = new PleaseTokenType("OPEN_BLOCK");
  IElementType OR = new PleaseTokenType("OR");
  IElementType PASS = new PleaseTokenType("pass");
  IElementType PERCENT = new PleaseTokenType("PERCENT");
  IElementType PIPE = new PleaseTokenType("|");
  IElementType PLUS = new PleaseTokenType("+");
  IElementType RBRACE = new PleaseTokenType("}");
  IElementType RBRACK = new PleaseTokenType("]");
  IElementType RIGHT_CHEV = new PleaseTokenType("RIGHT_CHEV");
  IElementType RPAREN = new PleaseTokenType(")");
  IElementType SPACE = new PleaseTokenType("space");
  IElementType STR_LIT = new PleaseTokenType("STR_LIT");
  IElementType TIMES = new PleaseTokenType("TIMES");
  IElementType TRUE_LIT = new PleaseTokenType("True");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ASSIGNMENT) {
        return new PleaseAssignmentImpl(node);
      }
      else if (type == BOOL_LIT) {
        return new PleaseBoolLitImpl(node);
      }
      else if (type == DICT_LIT) {
        return new PleaseDictLitImpl(node);
      }
      else if (type == EXPRESSION) {
        return new PleaseExpressionImpl(node);
      }
      else if (type == FUNCTION_CALL) {
        return new PleaseFunctionCallImpl(node);
      }
      else if (type == FUNCTION_CALL_PARAM) {
        return new PleaseFunctionCallParamImpl(node);
      }
      else if (type == FUNCTION_DEF) {
        return new PleaseFunctionDefImpl(node);
      }
      else if (type == FUNCTION_DEF_PARAM) {
        return new PleaseFunctionDefParamImpl(node);
      }
      else if (type == LIST_LIT) {
        return new PleaseListLitImpl(node);
      }
      else if (type == OPERATOR) {
        return new PleaseOperatorImpl(node);
      }
      else if (type == STATEMENT) {
        return new PleaseStatementImpl(node);
      }
      else if (type == TYPE) {
        return new PleaseTypeImpl(node);
      }
      else if (type == VALUE) {
        return new PleaseValueImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
