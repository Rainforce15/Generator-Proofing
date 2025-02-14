@file:JvmName("Utilities")
package com.aeolid.generatorproofing

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.ex.Range
import com.intellij.openapi.vcs.ex.createRanges
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

fun getChangedTextRanges(document: Document, changedRanges: List<Range>): List<TextRange> {
	val ranges = ArrayList<TextRange>()

	for (range in changedRanges) {
		if (range.hasLines()) {
			ranges.add(TextRange(
				document.getLineStartOffset(range.line1),
				document.getLineEndOffset(range.line2 - 1))
			)
		} else if (range.hasVcsLines()) {
			val lineEndOffset = document.getLineStartOffset(range.line1)
			ranges.add(TextRange(lineEndOffset, lineEndOffset))
		}
	}

	return ranges
}

fun getOutsideRanges(text: String, startPattern: String, endPattern: String): List<TextRange> {
	if (startPattern == "" || endPattern == "") {
		return listOf(TextRange(0, text.length - 1))
	}

	val ranges = ArrayList<TextRange>()
	var prevSearchEnd = 0
	var prevLineEnd = 0

	while (true) {
		val start = text.indexOf(startPattern, prevSearchEnd)
		if (start == -1) {
			ranges.add(TextRange(prevLineEnd, text.length - 1)) // to the end of the file
			break
		}
		val startNextLine = text.indexOf("\n", start) + 1 // skip the start pattern
		ranges.add(TextRange(prevLineEnd, startNextLine))

		prevSearchEnd = text.indexOf(endPattern, startNextLine)
		if (prevSearchEnd == -1) {
			break
		}
		prevLineEnd = text.lastIndexOf("\n", prevSearchEnd) + 1 // skip the end pattern indentation
	}

	return ranges
}

fun getRelevantRanges(outsideUserSection: List<TextRange>, changes: List<TextRange>): List<TextRange> {
	val relevantRanges = ArrayList<TextRange>()

	var outsideIdx = 0
	var changeIdx = 0

	while (outsideIdx < outsideUserSection.size && changeIdx < changes.size) {
		val outsideRange = outsideUserSection[outsideIdx]
		val changeRange = changes[changeIdx]

		if (outsideRange.endOffset <= changeRange.startOffset) {
			outsideIdx++
		} else if (changeRange.endOffset <= outsideRange.startOffset) {
			changeIdx++
		} else {
			relevantRanges.add(TextRange(
				outsideRange.startOffset.coerceAtLeast(changeRange.startOffset),
				outsideRange.endOffset.coerceAtMost(changeRange.endOffset)
			))
			if (outsideRange.endOffset < changeRange.endOffset) {
				outsideIdx++
			} else {
				changeIdx++
			}
		}
	}

	return relevantRanges
}

fun getAffectedRanges(file: PsiFile, startPattern: String, endPattern: String): List<TextRange> {
	val project = file.project
	val change = ChangeListManager.getInstance(project).getChange(file.virtualFile)
	val document = PsiDocumentManager.getInstance(project).getDocument(file)

	return if (document != null && change != null) {
		val docText = document.charsSequence.toString()

		if (change.type == Change.Type.NEW) {
			getRelevantRanges(getOutsideRanges(docText, startPattern, endPattern), listOf(file.textRange))
		} else {
			val contentFromVcs = change.beforeRevision?.content ?: ""
			val changedLines = createRanges(docText, StringUtilRt.convertLineSeparators(contentFromVcs, "\n"))

			getRelevantRanges(
				getOutsideRanges(docText, startPattern, endPattern),
				getChangedTextRanges(document, changedLines)
			)
		}
	} else {
		emptyList()
	}
}
