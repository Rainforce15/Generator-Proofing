package com.aeolid.generatorproofing

import com.aeolid.generatorproofing.InspectionBundle.getMessage
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType.ERROR
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.InspectionOptionsPanel
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiJavaFile
import javax.swing.JComponent
import javax.swing.JLabel

class InspectionImpl : AbstractBaseJavaLocalInspectionTool() {
	@JvmField var headerPattern = ""
	@JvmField var beginPattern = ""
	@JvmField var endPattern = ""

	override fun runForWholeFile(): Boolean {
		return true
	}

	override fun createOptionsPanel(): JComponent {
		val panel = InspectionOptionsPanel(this)

		panel.add(JLabel(getMessage("inspection.generatedCodePattern.display.headerPattern")), "cell 0 0")
		panel.add(SingleTextField(this, "headerPattern", getMessage("inspection.generatedCodePattern.display.headerPatternInfo")), "cell 1 0, growx, pushx")

		panel.add(JLabel(getMessage("inspection.generatedCodePattern.display.beginPattern")), "cell 0 1")
		panel.add(SingleTextField(this, "beginPattern", getMessage("inspection.generatedCodePattern.display.beginPatternInfo")), "cell 1 1, growx, pushx")

		panel.add(JLabel(getMessage("inspection.generatedCodePattern.display.endPattern")), "cell 0 2")
		panel.add(SingleTextField(this, "endPattern", getMessage("inspection.generatedCodePattern.display.endPatternInfo")), "cell 1 2, growx, pushx")

		return panel
	}

	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
		return GeneratorPatternVisitor(holder)
	}

	private inner class GeneratorPatternVisitor(var holder: ProblemsHolder) : JavaElementVisitor() {
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
