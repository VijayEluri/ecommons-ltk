/*******************************************************************************
 * Copyright (c) 2009-2013 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;


public abstract class CompletionProposalWithOverwrite implements IAssistCompletionProposal,
		ICompletionProposalExtension4 {
	
	
	protected final AssistInvocationContext fContext;
	
	/** The replacement offset. */
	private final int fReplacementOffset;
	
//	private StyleRange fRememberedStyleRange;
	private Annotation fRememberedOverwriteAnnotation;
	
	
	protected CompletionProposalWithOverwrite(final AssistInvocationContext context, final int startOffset) {
		fContext = context;
		fReplacementOffset = startOffset;
	}
	
	
	public final int getReplacementOffset() {
		return fReplacementOffset;
	}
	
	protected int computeReplacementLength(final int replacementOffset, final Point selection, final int caretOffset, final boolean overwrite) throws BadLocationException {
		final int end = Math.max(caretOffset, selection.x + selection.y);
		return (end - replacementOffset);
	}
	
	private boolean isInOverwriteMode(final boolean toggle) {
		return toggle;
	}
	
	protected abstract String getPluginId();
	
	
	@Override
	public final void selected(final ITextViewer viewer, final boolean smartToggle) {
		if (isInOverwriteMode(smartToggle)) {
			addOverwriteStyle();
		} else {
			repairPresentation();
		}
	}
	
	@Override
	public final void unselected(final ITextViewer viewer) {
		repairPresentation();
	}
	
	/**
	 * not supported, use {@link #apply(ITextViewer, char, int, int)}
	 */
	@Override
	public void apply(final IDocument document) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void apply(final ITextViewer viewer, final char trigger, final int stateMask, final int offset) {
		assert (fContext.getSourceViewer() == viewer);
		
		final boolean smartToggle = (stateMask & SWT.CTRL) != 0;
		try {
			final int replacementOffset = getReplacementOffset();
			final int replacementLength = computeReplacementLength(replacementOffset, viewer.getSelectedRange(), offset, isInOverwriteMode(smartToggle));
			
			if (validate(viewer.getDocument(), offset, null)) {
				doApply(trigger, stateMask, offset, replacementOffset, replacementLength);
				return;
			}
		}
		catch (final BadLocationException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, getPluginId(), "Failed to apply completion proposal.", e));
		}
		Display.getCurrent().beep();
	}
	
	protected abstract void doApply(final char trigger, final int stateMask,
			final int caretOffset, final int replacementOffset, final int replacementLength) throws BadLocationException;
	
	
	protected void reinvokeAssist(final ITextViewer viewer) {
		if (viewer instanceof ITextOperationTarget) {
			final ITextOperationTarget target = (ITextOperationTarget) viewer;
			Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (target.canDoOperation(ISourceViewer.CONTENTASSIST_PROPOSALS)) {
						target.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
					}
				}
			});
		}
	}
	
	
	private void addOverwriteStyle() {
		final SourceViewer viewer = fContext.getSourceViewer();
		final StyledText text = viewer.getTextWidget();
		if (text == null || text.isDisposed()) {
			return;
		}
		
		final int widgetCaret = viewer.getTextWidget().getCaretOffset();
		final int modelCaret = viewer.widgetOffset2ModelOffset(widgetCaret);
		final int replacementOffset = getReplacementOffset();
		int replacementLength;
		try {
			replacementLength = computeReplacementLength(replacementOffset, viewer.getSelectedRange(), modelCaret, true);
		}
		catch (final BadLocationException e) {
			replacementLength = -1;
		}
		if (replacementLength < 0 || modelCaret >= replacementOffset + replacementLength) {
			repairPresentation();
			return;
		}
		
		final int offset = widgetCaret;
		final int length = replacementOffset + replacementLength - modelCaret;
		
		repairPresentation();
		fRememberedOverwriteAnnotation = new Annotation("de.walware.ecommons.text.editorAnnotations.ContentAssistOverwrite", false, "");
		viewer.getAnnotationModel().addAnnotation(fRememberedOverwriteAnnotation, new Position(offset, length));
		
//		final Color background = getOverwriteBackgroundColor();
//		final Color foreground = getOverwriteForegroundColor();
//		if (foreground == null && background == null) {
//			repairPresentation();
//			return;
//		}
//		
//		final StyleRange currentStyle = text.getStyleRangeAtOffset(offset);
//		
//		final StyleRange style = new StyleRange(offset, length, foreground, background);
//		if (currentStyle != null) {
//			style.fontStyle = currentStyle.fontStyle;
//			style.strikeout = currentStyle.strikeout;
//			style.underline = currentStyle.underline;
//		}
//		
//		repairPresentation();
//		// http://dev.eclipse.org/bugs/show_bug.cgi?id=34754
//		try {
//			text.setStyleRange(style);
//			fRememberedStyleRange = style;
//		}
//		catch (final IllegalArgumentException x) {
//		}
	}
	
	private void repairPresentation() {
		final SourceViewer viewer = fContext.getSourceViewer();
		if (fRememberedOverwriteAnnotation != null) {
			viewer.getAnnotationModel().removeAnnotation(fRememberedOverwriteAnnotation);
			fRememberedOverwriteAnnotation = null;
		}
//		if (fRememberedStyleRange != null) {
//			final IRegion modelRange = viewer.widgetRange2ModelRange(new Region(fRememberedStyleRange.start, fRememberedStyleRange.length));
//			if (modelRange != null) {
//				viewer.invalidateTextPresentation(modelRange.getOffset(), modelRange.getLength());
//			}
//			fRememberedStyleRange = null;
//		}
	}
	
}
