package com.aeolid.generatorproofing

import com.aeolid.generatorproofing.InspectionBundle.getMessage
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.InspectionOptionsPanel
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiJavaFile
import javax.swing.JComponent
import javax.swing.JLabel

class InspectionImpl : AbstractBaseJavaLocalInspectionTool() {
	@JvmField var headerPattern = "/* Generated File */"
	@JvmField var beginPattern = "// IMPLEMENTATION BEGIN"
	@JvmField var endPattern = "// IMPLEMENTATION END"

	override fun runForWholeFile(): Boolean {
		return true
	}

	override fun createOptionsPanel(): JComponent {
		val panel = InspectionOptionsPanel(this)

		panel.add(JLabel(getMessage("inspection.generatedCodePattern.display.headerPattern")), "cell 0 0")
		panel.add(SingleTextField(this, "headerPattern"), "cell 1 0, growx, pushx")

		panel.add(JLabel(getMessage("inspection.generatedCodePattern.display.beginPattern")), "cell 0 1")
		panel.add(SingleTextField(this, "beginPattern"), "cell 1 1, growx, pushx")

		panel.add(JLabel(getMessage("inspection.generatedCodePattern.display.endPattern")), "cell 0 2")
		panel.add(SingleTextField(this, "endPattern"), "cell 1 2, growx, pushx")

		return panel
	}

	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
		return GeneratorPatternVisitor(holder)
	}

	private inner class GeneratorPatternVisitor(var holder: ProblemsHolder) : JavaElementVisitor() {
		private val _errorText = getMessage("inspection.generatedCodePattern.display.name")
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
