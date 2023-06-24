package com.aeolid.generatorproofing

import com.aeolid.generatorproofing.InspectionBundle.getMessage
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ProblemHighlightType.ERROR
import com.intellij.codeInspection.options.OptPane
import com.intellij.codeInspection.options.OptString
import com.intellij.codeInspection.options.PlainMessage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.psi.*
import org.intellij.lang.annotations.Language

class InspectionImpl: AbstractBaseJavaLocalInspectionTool() {
	var headerPattern = ""
	var beginPattern = ""
	var endPattern = ""

	val ignoredFiles = HashSet<String>()

	override fun runForWholeFile() = true

	private fun getOptString(@Language("jvm-field-name") bindId: String, labelId: String, infoTextId: String) =
		OptString(bindId, PlainMessage(getMessage(labelId)), null, -1, HtmlChunk.raw(getMessage(infoTextId)))

	override fun getOptionsPane() = OptPane.pane(
		getOptString("headerPattern", "inspection.generatedCodePattern.headerPattern", "inspection.generatedCodePattern.headerPatternInfo"),
		getOptString("beginPattern", "inspection.generatedCodePattern.beginPattern", "inspection.generatedCodePattern.beginPatternInfo"),
		getOptString("endPattern", "inspection.generatedCodePattern.endPattern", "inspection.generatedCodePattern.endPatternInfo")
	)

	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object: JavaElementVisitor() {
		private val _errorText = getMessage("inspection.generatedCodePattern.name")

		override fun visitJavaFile(file: PsiJavaFile) {
			val firstChild = file.firstChild
			if (
				headerPattern.isNotBlank() &&
				beginPattern.isNotBlank() &&
				endPattern.isNotBlank() &&
				firstChild is PsiComment &&
				firstChild.text.contains(headerPattern)
			) {
				val ignoreTempFix = object: LocalQuickFix {
					override fun getFamilyName() = getMessage("inspection.generatedCodePattern.IgnoreFileForNow")
					override fun applyFix(p: Project, d: ProblemDescriptor) { ignoredFiles.add(file.virtualFile.path) }
				}

				val affectedRanges = getAffectedRanges(file, beginPattern, endPattern)

				if (affectedRanges.isEmpty()) {
					ignoredFiles.remove(file.virtualFile.path)
				}

				if(!ignoredFiles.contains(file.virtualFile.path)) {
					for (changedRange in affectedRanges) {
						holder.registerProblem(ProblemDescriptorBase(
							file,
							file,
							_errorText,
							arrayOf(ignoreTempFix),
							ERROR,
							false,
							changedRange,
							true,
							false
						))
					}
				}
			}
			super.visitJavaFile(file)
		}
	}
}
