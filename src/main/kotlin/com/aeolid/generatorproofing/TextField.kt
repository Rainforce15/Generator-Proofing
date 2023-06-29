package com.aeolid.generatorproofing

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ui.DocumentAdapter
import org.intellij.lang.annotations.Language
import java.text.ParseException
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class TextField(
	private val owner: InspectionProfileEntry,
	@Language("jvm-field-name") private val field: String,
	private val name: String,
	private val hint: String?,
	private val properties: PropertiesComponent
): JTextField() {
	constructor(
		owner: InspectionProfileEntry,
		@Language("jvm-field-name") field: String,
		name: String,
		properties: PropertiesComponent
	): this(owner, field, name, null, properties)

	init {
		text = getPropertyValue()
		toolTipText = hint

		document.addDocumentListener(object : DocumentAdapter() {
			public override fun textChanged(e: DocumentEvent) {
				try {
					setPropertyValue(this@TextField.text)
				} catch (e1: ParseException) {
					// No luck this time
				}
			}
		})
	}

	private fun setPropertyValue(value: String) {
		try {
			owner.javaClass.getField(field)[owner] = value
			properties.setValue(name, value)
		} catch (e: Exception) {
			// OK
		}
	}

	private fun getPropertyValue(): String {
		return try {
			properties.getValue(name, "")
		} catch (e: Exception) {
			"Loading error"
		}
	}
}
