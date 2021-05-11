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

public class PleaseOperatorImpl extends ASTWrapperPsiElement implements PleaseOperator {

  public PleaseOperatorImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PleaseVisitor visitor) {
    visitor.visitOperator(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PleaseVisitor) accept((PleaseVisitor)visitor);
    else super.accept(visitor);
  }

}
