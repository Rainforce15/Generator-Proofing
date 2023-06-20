package com.aeolid.generatorproofing

import com.aeolid.generatorproofing.InspectionBundle.getMessage
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType.ERROR
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.options.OptPane
import com.intellij.codeInspection.options.OptString
import com.intellij.codeInspection.options.PlainMessage
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiJavaFile
import org.intellij.lang.annotations.Language

class InspectionImpl: AbstractBaseJavaLocalInspectionTool() {
	var headerPattern: String = ""
	var beginPattern: String = ""
	var endPattern: String = ""

	override fun runForWholeFile(): Boolean {
		return true
	}

	private fun getOptString(@Language("jvm-field-name") bindId: String, labelId: String, infoTextId: String): OptString {
		return OptString(bindId, PlainMessage(getMessage(labelId)), null, -1, HtmlChunk.raw(getMessage(infoTextId)))
	}

	override fun getOptionsPane(): OptPane {
		return OptPane.pane(
			getOptString("headerPattern", "inspection.generatedCodePattern.display.headerPattern", "inspection.generatedCodePattern.display.headerPatternInfo"),
			getOptString("beginPattern", "inspection.generatedCodePattern.display.beginPattern", "inspection.generatedCodePattern.display.beginPatternInfo"),
			getOptString("endPattern", "inspection.generatedCodePattern.display.endPattern", "inspection.generatedCodePattern.display.endPatternInfo")
		)
	}

	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
		return GeneratorPatternVisitor(holder)
	}

	inner class GeneratorPatternVisitor(private val holder: ProblemsHolder): JavaElementVisitor() {
		private val _errorText = getMessage("inspection.generatedCodePattern.display.name")

		override fun visitJavaFile(file: PsiJavaFile) {
			val firstChild = file.firstChild
			if (
				headerPattern.isNotBlank() &&
				beginPattern.isNotBlank() &&
				endPattern.isNotBlank() &&
				firstChild is PsiComment &&
				firstChild.text.contains(headerPattern)
			) {
				for (changedRange in getAffectedRanges(file, beginPattern, endPattern)) {
					holder.registerProblem(
							ProblemDescriptorBase(
							file,
							file,
							_errorText,
							null,
							ERROR,
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
