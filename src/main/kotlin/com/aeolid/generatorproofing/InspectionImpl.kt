package com.aeolid.generatorproofing

import com.aeolid.generatorproofing.InspectionBundle.getMessage
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ProblemHighlightType.ERROR
import com.intellij.codeInspection.ui.InspectionOptionsPanel
import com.intellij.ide.DataManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiJavaFile
import javax.swing.JComponent
import javax.swing.JLabel

class InspectionImpl : AbstractBaseJavaLocalInspectionTool() {

	private val headerPatternKey = "com.aeolid.generatorproofing.headerPattern"
	private val beginPatternKey = "com.aeolid.generatorproofing.beginPattern"
	private val endPatternKey = "com.aeolid.generatorproofing.endPattern"

	val ignoredFiles = HashSet<String>()

	private val properties: PropertiesComponent = PropertiesComponent.getInstance()

	// observed fields
	@JvmField var headerPattern = properties.getValue(headerPatternKey, "")
	@JvmField var beginPattern = properties.getValue(beginPatternKey, "")
	@JvmField var endPattern = properties.getValue(endPatternKey, "")


	override fun runForWholeFile() = true

	override fun createOptionsPanel(): JComponent {
		val panel = InspectionOptionsPanel()

		DataManager.getInstance().dataContextFromFocusAsync.then {
			panel.add(JLabel(getMessage("inspection.generatedCodePattern.headerPattern")), "cell 0 0")
			panel.add(TextField(this, "headerPattern", headerPatternKey, getMessage("inspection.generatedCodePattern.headerPatternInfo")), "cell 1 0, growx, pushx")

			panel.add(JLabel(getMessage("inspection.generatedCodePattern.beginPattern")), "cell 0 1")
			panel.add(TextField(this, "beginPattern", beginPatternKey, getMessage("inspection.generatedCodePattern.beginPatternInfo")), "cell 1 1, growx, pushx")

			panel.add(JLabel(getMessage("inspection.generatedCodePattern.endPattern")), "cell 0 2")
			panel.add(TextField(this, "endPattern", endPatternKey, getMessage("inspection.generatedCodePattern.endPatternInfo")), "cell 1 2, growx, pushx")
		}

		return panel
	}

	fun areNotBlank(header: String, begin: String, end: String) = header.isNotBlank() && begin.isNotBlank() && end.isNotBlank()

	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object: JavaElementVisitor() {
		override fun visitJavaFile(file: PsiJavaFile) {
			val firstChild = file.firstChild
			if (
				areNotBlank(headerPattern, beginPattern, endPattern) &&
				firstChild is PsiComment &&
				firstChild.text.contains(headerPattern)
			) {
				val affectedRanges = getAffectedRanges(file, beginPattern, endPattern)

				if (affectedRanges.isEmpty()) {
					ignoredFiles.remove(file.virtualFile.path)
				}

				if(!ignoredFiles.contains(file.virtualFile.path)) {
					for (changedRange in affectedRanges) {
						holder.registerProblem(ProblemDescriptorBase(
							file,
							file,
							getMessage("inspection.generatedCodePattern.name"),
							arrayOf(object: LocalQuickFix {
								override fun getFamilyName() = getMessage("inspection.generatedCodePattern.IgnoreFileForNow")
								override fun applyFix(p: Project, d: ProblemDescriptor) { ignoredFiles.add(file.virtualFile.path) }
							}),
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
