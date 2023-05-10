package com.oukq.cocktailhelper.inspection;

import com.google.common.collect.Sets;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * 引擎提供的线程池请使用有DistrubuteKey的方法
 */
public class MsScheduledExecutorServiceInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Set<String> ACCEPT_TYPES = Sets.newHashSet("int", "java.lang.Integer", "long",
            "java.lang.Long");
    private static final Set<String> REFERENCE_NAMES = Sets.newHashSet("submit", "execute", "schedule",
            "scheduleAtFixedRate", "scheduleWithFixedDelay");

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                if (expression.getContainingFile().getName().contains("AqmMsScheduledDelegation")) {
                    return;
                }
                PsiReferenceExpression methodExpression = expression.getMethodExpression();
                String referenceName = methodExpression.getReferenceName();
                if (!REFERENCE_NAMES.contains(referenceName)) {
                    return;
                }
                boolean usingMsScedulers = methodExpression.getQualifiedName().startsWith("MsSchedulers");
                if (!usingMsScedulers) {
                    return;
                }
                PsiExpressionList argumentList = expression.getArgumentList();
                if (argumentList.isEmpty()) {
                    return;
                }
                PsiType[] types = argumentList.getExpressionTypes();
                String firstTypeName = types[0].getCanonicalText();
                if (ACCEPT_TYPES.contains(firstTypeName)) {
                    return;
                }
                holder.registerProblem(expression,
                        InspectionBundle.message("inspection.server.msschedules.tips"));
            }

        };
    }

}
