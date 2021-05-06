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

public class PleaseFunctionCallImpl extends ASTWrapperPsiElement implements PleaseFunctionCall {

  public PleaseFunctionCallImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PleaseVisitor visitor) {
    visitor.visitFunctionCall(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PleaseVisitor) accept((PleaseVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<PleaseFunctionCallParam> getFunctionCallParamList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, PleaseFunctionCallParam.class);
  }

  @Override
  @NotNull
  public PsiElement getIdent() {
    return findNotNullChildByType(IDENT);
  }

}
