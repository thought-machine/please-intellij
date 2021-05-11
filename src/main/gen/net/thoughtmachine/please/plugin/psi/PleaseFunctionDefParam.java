// This is a generated file. Not intended for manual editing.
package net.thoughtmachine.please.plugin.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface PleaseFunctionDefParam extends PsiElement {

  @Nullable
  PleaseExpression getExpression();

  @NotNull
  List<PleaseType> getTypeList();

  @NotNull
  PsiElement getIdent();

}
