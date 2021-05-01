// This is a generated file. Not intended for manual editing.
package org.intellij.plugin.please.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface PleaseStatement extends PsiElement {

  @Nullable
  PleaseAssignment getAssignment();

  @Nullable
  PleaseExpression getExpression();

  @Nullable
  PleaseFunctionDef getFunctionDef();

}
