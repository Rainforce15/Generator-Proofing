package com.aeolid.generatorproofing

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.options.OptPane
import com.intellij.codeInspection.options.OptPane.*
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiJavaFile

class InspectionImpl: AbstractBaseJavaLocalInspectionTool() {
	var headerPattern: String = "/* Generated File */"
	var beginPattern: String = "// IMPLEMENTATION BEGIN"
	var endPattern: String = "// IMPLEMENTATION END"

	override fun runForWholeFile(): Boolean {
		return true
	}

	override fun getOptionsPane(): OptPane {
		return pane(
			string("headerPattern", InspectionBundle.getMessage("inspection.generatedCodePattern.display.headerPattern")),
			string("beginPattern", InspectionBundle.getMessage("inspection.generatedCodePattern.display.beginPattern")),
			string("endPattern", InspectionBundle.getMessage("inspection.generatedCodePattern.display.endPattern"))
		)
	}

	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
		return GeneratorPatternVisitor(holder)
	}

	inner class GeneratorPatternVisitor(private val holder: ProblemsHolder): JavaElementVisitor() {
		private val _errorText = InspectionBundle.getMessage("inspection.generatedCodePattern.display.name")

		override fun visitJavaFile(file: PsiJavaFile) {
			if (file.firstChild.text.contains(headerPattern)) {
				for (changedRange in getAffectedRanges(file, beginPattern, endPattern)) {
					holder.registerProblem(
						ProblemDescriptorBase(
						file,
						file,
						_errorText,
						null,
						ProblemHighlightType.ERROR,
						false,
						changedRange,
						true,
						false
					)
					)
				}
			}
			super.visitJavaFile(file)
		}
	}
}
