package com.aeolid.generatorproofing

import com.aeolid.generatorproofing.InspectionBundle.getMessage
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ProblemHighlightType.ERROR
import com.intellij.codeInspection.ui.InspectionOptionsPanel
import com.intellij.ide.DataManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.PlatformDataKeys
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

	private val globalProperties: PropertiesComponent = PropertiesComponent.getInstance()
	private var projectProperties: PropertiesComponent? = null

	init {
		DataManager.getInstance().dataContextFromFocusAsync.then {
			val project = it.getData(PlatformDataKeys.PROJECT)

			if (project != null) {
				val props = PropertiesComponent.getInstance(project)
				projectProperties = props

				headerPattern = props.getValue(headerPatternKey, "")
				beginPattern = props.getValue(beginPatternKey, "")
				endPattern = props.getValue(endPatternKey, "")
			}
		}
	}

	// observed fields
	@JvmField var headerPattern = ""
	@JvmField var beginPattern = ""
	@JvmField var endPattern = ""
	@JvmField var headerPatternGlobal = globalProperties.getValue(headerPatternKey, "")
	@JvmField var beginPatternGlobal = globalProperties.getValue(beginPatternKey, "")
	@JvmField var endPatternGlobal = globalProperties.getValue(endPatternKey, "")


	override fun runForWholeFile() = true

	override fun createOptionsPanel(): JComponent {
		val panel = InspectionOptionsPanel()

		DataManager.getInstance().dataContextFromFocusAsync.then {
			panel.add(JLabel(getMessage("inspection.generatedCodePattern.headerPatternGlobal")), "cell 0 0")
			panel.add(TextField(this, "headerPatternGlobal", headerPatternKey, getMessage("inspection.generatedCodePattern.headerPatternInfo"), globalProperties), "cell 1 0, growx, pushx")

			panel.add(JLabel(getMessage("inspection.generatedCodePattern.beginPatternGlobal")), "cell 0 1")
			panel.add(TextField(this, "beginPatternGlobal", beginPatternKey, getMessage("inspection.generatedCodePattern.beginPatternInfo"), globalProperties), "cell 1 1, growx, pushx")

			panel.add(JLabel(getMessage("inspection.generatedCodePattern.endPatternGlobal")), "cell 0 2")
			panel.add(TextField(this, "endPatternGlobal", endPatternKey, getMessage("inspection.generatedCodePattern.endPatternInfo"), globalProperties), "cell 1 2, growx, pushx")

			val props: PropertiesComponent = projectProperties ?: return@then
			panel.add(JLabel(getMessage("inspection.generatedCodePattern.headerPattern")), "cell 0 3")
			panel.add(TextField(this, "headerPattern", headerPatternKey, props), "cell 1 3, growx, pushx")

			panel.add(JLabel(getMessage("inspection.generatedCodePattern.beginPattern")), "cell 0 4")
			panel.add(TextField(this, "beginPattern", beginPatternKey, props), "cell 1 4, growx, pushx")

			panel.add(JLabel(getMessage("inspection.generatedCodePattern.endPattern")), "cell 0 5")
			panel.add(TextField(this, "endPattern", endPatternKey, props), "cell 1 5, growx, pushx")
		}

		return panel
	}

	fun areNotBlank(header: String, begin: String, end: String) = header.isNotBlank() && begin.isNotBlank() && end.isNotBlank()

	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object: JavaElementVisitor() {
		override fun visitJavaFile(file: PsiJavaFile) {
			val usedHeaderPattern: String
			val usedBeginPattern: String
			val usedEndPattern: String

			if (areNotBlank(headerPattern, beginPattern, endPattern)) {
				usedHeaderPattern = headerPattern
				usedBeginPattern = beginPattern
				usedEndPattern = endPattern
			} else {
				usedHeaderPattern = headerPatternGlobal
				usedBeginPattern = beginPatternGlobal
				usedEndPattern = endPatternGlobal
			}

			val firstChild = file.firstChild
			if (
				areNotBlank(usedHeaderPattern, usedBeginPattern, usedEndPattern) &&
				firstChild is PsiComment &&
				firstChild.text.contains(usedHeaderPattern)
			) {
				val affectedRanges = getAffectedRanges(file, usedBeginPattern, usedEndPattern)

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
