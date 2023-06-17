package com.aeolid.generatorproofing

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class InspectionImpl: AbstractBaseJavaLocalInspectionTool() {

	override fun runForWholeFile(): Boolean {
		return true
	}

	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
		return GeneratorPatternVisitor(holder)
	}
}
