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

public class PleaseStatementImpl extends ASTWrapperPsiElement implements PleaseStatement {

  public PleaseStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PleaseVisitor visitor) {
    visitor.visitStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PleaseVisitor) accept((PleaseVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PleaseAssignment getAssignment() {
    return findChildByClass(PleaseAssignment.class);
  }

  @Override
  @Nullable
  public PleaseExpression getExpression() {
    return findChildByClass(PleaseExpression.class);
  }

  @Override
  @Nullable
  public PleaseFunctionDef getFunctionDef() {
    return findChildByClass(PleaseFunctionDef.class);
  }

  @Override
  @Nullable
  public PleaseIfStatement getIfStatement() {
    return findChildByClass(PleaseIfStatement.class);
  }

}
