// This is a generated file. Not intended for manual editing.
package net.thoughtmachine.please.plugin.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface PleaseIfStatement extends PsiElement {

  @NotNull
  List<PleaseExpression> getExpressionList();

  @NotNull
  List<PleaseStatement> getStatementList();

}
