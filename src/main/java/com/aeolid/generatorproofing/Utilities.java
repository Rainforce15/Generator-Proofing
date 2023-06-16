package com.aeolid.generatorproofing;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.ex.Range;
import com.intellij.openapi.vcs.ex.RangesBuilder;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

public class Utilities {
	private Utilities() {}

	@NotNull
	public static List<Range> getRanges(@NotNull CharSequence docText, @NotNull CharSequence contentFromVcs) {
		return RangesBuilder.createRanges(docText, StringUtilRt.convertLineSeparators(contentFromVcs, "\n"));
	}

	@NotNull
	public static List<TextRange> getChangedTextRanges(
			@NotNull List<? extends Range> gotRanges,
			@Nullable Change change,
			@NotNull PsiFile file,
			@NotNull Document document,
			@Nullable String contentFromVcs
	) {
		if (change != null && change.getType() == Change.Type.NEW) {
			TextRange fileRange = file.getTextRange();
			return ContainerUtil.newArrayList(fileRange);
		}

		return contentFromVcs != null ? getChangedTextRanges(document, gotRanges) : Collections.emptyList();
	}

	@NotNull
	public static List<TextRange> getChangedTextRanges(@NotNull Document document, @NotNull List<? extends Range> changedRanges) {
		final List<TextRange> ranges = new ArrayList<>();

		for (Range range : changedRanges) {
			if (range.hasLines()) {
				int changeStartLine = range.getLine1();
				int changeEndLine = range.getLine2();

				int lineStartOffset = document.getLineStartOffset(changeStartLine);
				int lineEndOffset = document.getLineEndOffset(changeEndLine - 1);

				TextRange changedTextRange = new TextRange(lineStartOffset, lineEndOffset);
				ranges.add(changedTextRange);
			} else if (range.hasVcsLines()) {
				int lineEndOffset = document.getLineStartOffset(range.getVcsLine1());
				TextRange changedTextRange = new TextRange(lineEndOffset, lineEndOffset);
				ranges.add(changedTextRange);
			}
		}

		return ranges;
	}

	@Nullable
	public static String getRevisionedContentFrom(@NotNull Change change) {
		ContentRevision revision = change.getBeforeRevision();
		if (revision == null) {
			return null;
		}

		try {
			return revision.getContent();
		}
		catch (VcsException e) {
			return null;
		}
	}

	@NotNull
	public static List<TextRange> getOutsideRanges(@NotNull String text, @NotNull String startPattern, @NotNull String endPattern) {
		ArrayList<TextRange> ranges = new ArrayList<>();
		int prevSearchEnd = 0;
		int prevLineEnd = 0;

		while (true) {
			int start = text.indexOf(startPattern, prevSearchEnd);
			if (start == -1) {
				ranges.add(new TextRange(prevLineEnd, text.length() - 1)); // to the end of the file
				break;
			}
			int startNextLine = text.indexOf("\n", start) + 1; // skip the start pattern
			ranges.add(new TextRange(prevLineEnd, startNextLine));

			prevSearchEnd = text.indexOf(endPattern, startNextLine);
			if (prevSearchEnd == -1) {
				break;
			}
			prevLineEnd = text.lastIndexOf("\n", prevSearchEnd) + 1; // skip the end pattern indentation
		}

		return Collections.unmodifiableList(ranges);
	}

	@NotNull
	public static List<TextRange> getRelevantRanges(@NotNull List<TextRange> outsideUserSection, @NotNull List<TextRange> changes) {
		ArrayList<TextRange> relevantRanges = new ArrayList<>();

		int outsideIdx = 0;
		int changeIdx = 0;

		while (outsideIdx < outsideUserSection.size() && changeIdx < changes.size()) {
			TextRange outsideRange = outsideUserSection.get(outsideIdx);
			TextRange changeRange = changes.get(changeIdx);

			if (outsideRange.getEndOffset() <= changeRange.getStartOffset()) {
				outsideIdx++;
			} else if (changeRange.getEndOffset() <= outsideRange.getStartOffset()) {
				changeIdx++;
			} else {
				int start = Math.max(outsideRange.getStartOffset(), changeRange.getStartOffset());
				int end = Math.min(outsideRange.getEndOffset(), changeRange.getEndOffset());
				relevantRanges.add(new TextRange(start, end));
				if (outsideRange.getEndOffset() < changeRange.getEndOffset()) {
					outsideIdx++;
				} else {
					changeIdx++;
				}
			}
		}

		return Collections.unmodifiableList(relevantRanges);
	}

	@NotNull
	public static List<TextRange> getAffectedRanges(@NotNull PsiFile file, @NotNull String startPattern, @NotNull String endPattern) {
		Project project = file.getProject();
		Change change = ChangeListManager.getInstance(project).getChange(file.getVirtualFile());
		String contentFromVcs = change != null ? getRevisionedContentFrom(change) : null;
		Document document = PsiDocumentManager.getInstance(project).getDocument(file);

		if (document != null) {
			String docText = document.getCharsSequence().toString();

			List<TextRange> outsideUserSection = getOutsideRanges(docText, startPattern, endPattern);

			List<Range> changedLines = contentFromVcs != null ? getRanges(docText, contentFromVcs) : emptyList();
			List<TextRange> changed = getChangedTextRanges(changedLines, change, file, document, contentFromVcs);

			return getRelevantRanges(outsideUserSection, changed);
		} else {
			return emptyList();
		}
	}
}
