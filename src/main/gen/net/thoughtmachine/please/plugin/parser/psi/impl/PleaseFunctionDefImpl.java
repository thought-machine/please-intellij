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

public class PleaseFunctionDefImpl extends ASTWrapperPsiElement implements PleaseFunctionDef {

  public PleaseFunctionDefImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PleaseVisitor visitor) {
    visitor.visitFunctionDef(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PleaseVisitor) accept((PleaseVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<PleaseFunctionDefParam> getFunctionDefParamList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, PleaseFunctionDefParam.class);
  }

  @Override
  @NotNull
  public List<PleaseStatement> getStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, PleaseStatement.class);
  }

  @Override
  @NotNull
  public PsiElement getIdent() {
    return findNotNullChildByType(IDENT);
  }

}
