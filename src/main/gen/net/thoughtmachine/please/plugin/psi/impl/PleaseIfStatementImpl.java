// This is a generated file. Not intended for manual editing.
package net.thoughtmachine.please.plugin.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static net.thoughtmachine.please.plugin.psi.PleaseTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import net.thoughtmachine.please.plugin.psi.*;

public class PleaseIfStatementImpl extends ASTWrapperPsiElement implements PleaseIfStatement {

  public PleaseIfStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PleaseVisitor visitor) {
    visitor.visitIfStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PleaseVisitor) accept((PleaseVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<PleaseExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, PleaseExpression.class);
  }

  @Override
  @NotNull
  public List<PleaseStatement> getStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, PleaseStatement.class);
  }

}
