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

public class PleaseFunctionCallParamImpl extends ASTWrapperPsiElement implements PleaseFunctionCallParam {

  public PleaseFunctionCallParamImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PleaseVisitor visitor) {
    visitor.visitFunctionCallParam(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PleaseVisitor) accept((PleaseVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PleaseExpression getExpression() {
    return findNotNullChildByClass(PleaseExpression.class);
  }

  @Override
  @Nullable
  public PsiElement getIdent() {
    return findChildByType(IDENT);
  }

}
