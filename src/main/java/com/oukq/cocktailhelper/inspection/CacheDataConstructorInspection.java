package com.oukq.cocktailhelper.inspection;

import com.intellij.codeInsight.daemon.impl.quickfix.AddDefaultConstructorFix;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.impl.source.PsiClassImpl;
import org.jetbrains.annotations.NotNull;

public class CacheDataConstructorInspection extends AbstractBaseJavaLocalInspectionTool {

	@NotNull
	@Override
	public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
		PsiClass cacheClass = JavaPsiFacade.getInstance(holder.getProject())
				.findClass("com.bt.game.common.cache.CacheData", holder.getFile()
						.getResolveScope());

		return new JavaElementVisitor() {

			@Override
			public void visitClass(PsiClass aClass) {
				if (aClass.getQualifiedName() == null) {
					return;
				}
				if (!aClass.getQualifiedName().startsWith(Constants.SCAN_PACKAGE)) {
					return;
				}
				if (cacheClass == null) {
					return;
				}
				if (!isCacheDataCls(aClass, cacheClass)) {
					return;
				}

				if (aClass.getConstructors().length == 0) {
					return;
				}

				for (PsiMethod constructor : aClass.getConstructors()) {
					if (!constructor.hasParameters()) {
						return;
					}
				}
				holder.registerProblem(aClass,
						InspectionBundle.message("inspection.common.cachedata.tips"),
						new AddDefaultConstructorFix(aClass, PsiModifier.PRIVATE));
			}

		};
	}

	public static boolean isCacheDataCls(PsiClass cls, PsiClass cacheClass) {
		if (!cls.isInheritor(cacheClass, true)) {
			return false;
		}
		if (!(cls instanceof PsiClassImpl)) {
			return false;
		}
		if (cls.isEnum() || cls.isInterface() || cls.isRecord() || cls.isAnnotationType()) {
			return false;
		}
		if (cls.hasModifierProperty(PsiModifier.ABSTRACT)) {
			return false;
		}
		return true;
	}

}