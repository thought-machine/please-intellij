// This is a generated file. Not intended for manual editing.
package net.thoughtmachine.please.plugin.parser.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static net.thoughtmachine.please.plugin.parser.psi.PleaseTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import net.thoughtmachine.please.plugin.parser.psi.*;

public class PleaseValueImpl extends ASTWrapperPsiElement implements PleaseValue {

  public PleaseValueImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PleaseVisitor visitor) {
    visitor.visitValue(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PleaseVisitor) accept((PleaseVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PleaseBoolLit getBoolLit() {
    return findChildByClass(PleaseBoolLit.class);
  }

  @Override
  @Nullable
  public PleaseDictLit getDictLit() {
    return findChildByClass(PleaseDictLit.class);
  }

  @Override
  @Nullable
  public PleaseFunctionCall getFunctionCall() {
    return findChildByClass(PleaseFunctionCall.class);
  }

  @Override
  @Nullable
  public PleaseListLit getListLit() {
    return findChildByClass(PleaseListLit.class);
  }

  @Override
  @Nullable
  public PsiElement getIntLit() {
    return findChildByType(INT_LIT);
  }

  @Override
  @Nullable
  public PsiElement getStrLit() {
    return findChildByType(STR_LIT);
  }

}
