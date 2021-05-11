// This is a generated file. Not intended for manual editing.
package net.thoughtmachine.please.plugin.parser.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface PleaseValue extends PsiElement {

  @Nullable
  PleaseBoolLit getBoolLit();

  @Nullable
  PleaseDictLit getDictLit();

  @Nullable
  PleaseFunctionCall getFunctionCall();

  @Nullable
  PleaseListLit getListLit();

  @Nullable
  PsiElement getIntLit();

  @Nullable
  PsiElement getStrLit();

}
