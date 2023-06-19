package com.aeolid.generatorproofing

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.ui.DocumentAdapter
import java.text.ParseException
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class SingleTextField(private val owner: InspectionProfileEntry, private val property: String): JTextField() {
	init {
		text = getPropertyValue()
		val instance = this
		document.addDocumentListener(object : DocumentAdapter() {
			public override fun textChanged(e: DocumentEvent) {
				try {
					setPropertyValue(instance.text)
				} catch (e1: ParseException) {
					// No luck this time
				}
			}
		})
	}

	private fun setPropertyValue(value: String) {
		try {
			owner.javaClass.getField(property)[owner] = value
		} catch (e: Exception) {
			// OK
		}
	}

	private fun getPropertyValue(): String {
		return try {
			owner.javaClass.getField(property)[owner] as String
		} catch (e: Exception) {
			"Loading error"
		}
	}
}
