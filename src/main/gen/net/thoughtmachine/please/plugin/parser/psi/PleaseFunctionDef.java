// This is a generated file. Not intended for manual editing.
package net.thoughtmachine.please.plugin.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface PleaseFunctionDef extends PsiElement {

  @NotNull
  List<PleaseFunctionDefParam> getFunctionDefParamList();

  @NotNull
  List<PleaseStatement> getStatementList();

  @NotNull
  PsiElement getIdent();

}
