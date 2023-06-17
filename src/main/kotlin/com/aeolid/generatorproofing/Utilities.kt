@file:JvmName("Utilities")
package com.aeolid.generatorproofing

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.ex.Range
import com.intellij.openapi.vcs.ex.createRanges
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

fun getRanges(docText: CharSequence, contentFromVcs: CharSequence): List<Range> {
	return createRanges(docText, StringUtilRt.convertLineSeparators(contentFromVcs, "\n"))
}

fun getChangedTextRanges(
	gotRanges: List<Range>,
	change: Change?,
	file: PsiFile,
	document: Document,
	contentFromVcs: String?
): List<TextRange> {
	return if (change != null && change.type == Change.Type.NEW) {
		listOf(file.textRange)
	} else if (contentFromVcs != null) {
		getChangedTextRanges(document, gotRanges)
	} else {
		emptyList()
	}
}

fun getChangedTextRanges(document: Document, changedRanges: List<Range>): List<TextRange> {
	val ranges = ArrayList<TextRange>()

	for (range in changedRanges) {
		if (range.hasLines()) {
			ranges.add(TextRange(
				document.getLineStartOffset(range.line1),
				document.getLineEndOffset(range.line2 - 1))
			)
		} else if (range.hasVcsLines()) {
			val lineEndOffset = document.getLineStartOffset(range.vcsLine1)
			ranges.add(TextRange(lineEndOffset, lineEndOffset))
		}
	}

	return ranges
}

fun getRevisionedContentFrom(change: Change): String? {
	val revision = change.beforeRevision ?: return null

	return try {
		revision.content
	} catch (e: VcsException) {
		null
	}
}

fun getOutsideRanges(text: String, startPattern: String, endPattern: String): List<TextRange> {
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
	val contentFromVcs = if (change != null) getRevisionedContentFrom(change) else null
	val document = PsiDocumentManager.getInstance(project).getDocument(file)

	return if (document != null) {
		val docText = document.charsSequence.toString()
		val changedLines = if (contentFromVcs != null) getRanges(docText, contentFromVcs) else emptyList()

		getRelevantRanges(
			getOutsideRanges(docText, startPattern, endPattern),
			getChangedTextRanges(changedLines, change, file, document, contentFromVcs)
		)
	} else {
		emptyList()
	}
}