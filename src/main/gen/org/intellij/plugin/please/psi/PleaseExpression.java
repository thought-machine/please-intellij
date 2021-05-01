// This is a generated file. Not intended for manual editing.
package org.intellij.plugin.please.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface PleaseExpression extends PsiElement {

  @NotNull
  List<PleaseExpression> getExpressionList();

  @Nullable
  PleaseOperator getOperator();

  @NotNull
  PleaseValue getValue();

}
