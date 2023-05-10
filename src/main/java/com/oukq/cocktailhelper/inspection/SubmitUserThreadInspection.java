package com.oukq.cocktailhelper.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.NotNull;

/**
 * 递交用户线程任务 请使用ServerUtils.submitToUserThread
 */
public class SubmitUserThreadInspection extends AbstractBaseJavaLocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                if (expression.getContainingFile().getName().contains("ServerUtils")) {
                    return;
                }
                PsiReferenceExpression methodExpression = expression.getMethodExpression();
                String methodExpressionStr = methodExpression.getCanonicalText();
                if (!methodExpressionStr.equalsIgnoreCase("ExtensionHelper.submitToUserThread")
                        && !methodExpressionStr.equalsIgnoreCase("InMsgWorkerFacade.acceptTask")
                ) {
                    return;
                }
                holder.registerProblem(expression,
                        InspectionBundle.message("inspection.server.submit.user.thread.tips"));
            }

        };
    }

}
