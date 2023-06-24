package com.aeolid.generatorproofing

import com.aeolid.generatorproofing.InspectionBundle.getMessage
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ProblemHighlightType.ERROR
import com.intellij.codeInspection.ui.InspectionOptionsPanel
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiJavaFile
import javax.swing.JComponent
import javax.swing.JLabel

class InspectionImpl : AbstractBaseJavaLocalInspectionTool() {
	@JvmField var headerPattern = ""
	@JvmField var beginPattern = ""
	@JvmField var endPattern = ""

	val ignoredFiles = HashSet<String>()

	override fun runForWholeFile() = true

	override fun createOptionsPanel(): JComponent {
		val panel = InspectionOptionsPanel(this)

		panel.add(JLabel(getMessage("inspection.generatedCodePattern.headerPattern")), "cell 0 0")
		panel.add(SingleTextField(this, "headerPattern", getMessage("inspection.generatedCodePattern.headerPatternInfo")), "cell 1 0, growx, pushx")

		panel.add(JLabel(getMessage("inspection.generatedCodePattern.beginPattern")), "cell 0 1")
		panel.add(SingleTextField(this, "beginPattern", getMessage("inspection.generatedCodePattern.beginPatternInfo")), "cell 1 1, growx, pushx")

		panel.add(JLabel(getMessage("inspection.generatedCodePattern.endPattern")), "cell 0 2")
		panel.add(SingleTextField(this, "endPattern", getMessage("inspection.generatedCodePattern.endPatternInfo")), "cell 1 2, growx, pushx")

		return panel
	}

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
