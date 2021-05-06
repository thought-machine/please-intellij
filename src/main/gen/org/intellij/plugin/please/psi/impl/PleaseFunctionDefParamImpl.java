// This is a generated file. Not intended for manual editing.
package org.intellij.plugin.please.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.intellij.plugin.please.psi.PleaseTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import org.intellij.plugin.please.psi.*;

public class PleaseFunctionDefParamImpl extends ASTWrapperPsiElement implements PleaseFunctionDefParam {

  public PleaseFunctionDefParamImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PleaseVisitor visitor) {
    visitor.visitFunctionDefParam(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PleaseVisitor) accept((PleaseVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PleaseExpression getExpression() {
    return findChildByClass(PleaseExpression.class);
  }

  @Override
  @NotNull
  public List<PleaseType> getTypeList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, PleaseType.class);
  }

  @Override
  @NotNull
  public PsiElement getIdent() {
    return findNotNullChildByType(IDENT);
  }

}
