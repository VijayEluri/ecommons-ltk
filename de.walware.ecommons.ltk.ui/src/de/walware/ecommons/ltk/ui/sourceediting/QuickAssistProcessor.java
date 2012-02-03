/*******************************************************************************
 * Copyright (c) 2008-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

import de.walware.ecommons.ltk.IModelManager;


/**
 * LTK quick assistant processor.
 */
public class QuickAssistProcessor implements IQuickAssistProcessor {
	
	
	private final ISourceEditor fEditor;
	private String fErrorMessage;
	
	
	public QuickAssistProcessor() {
		this(null);
	}
	
	public QuickAssistProcessor(final ISourceEditor editor) {
		fEditor = editor;
	}
	
	
	/**
	 * @return the editor
	 */
	public ISourceEditor getEditor() {
		return fEditor;
	}
	
	
	@Override
	public boolean canAssist(final IQuickAssistInvocationContext invocationContext) {
		return false;
	}
	
	@Override
	public boolean canFix(final Annotation annotation) {
		if (annotation.isMarkedDeleted()) {
			return false;
		}
		final String type = annotation.getType();
		if (type.equals(SpellingAnnotation.TYPE)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Creates the context that is passed to the completion proposal
	 * computers.
	 * 
	 * @param invocationContext the original invocation context
	 * 
	 * @return the context to be passed to the computers
	 */
	protected AssistInvocationContext createContext(final IQuickAssistInvocationContext invocationContext,
			final IProgressMonitor monitor) {
		return new AssistInvocationContext(getEditor(), invocationContext.getOffset(), IModelManager.MODEL_FILE);
	}
	
	/**
	 * Creates the context that is passed to the completion proposal
	 * computers.
	 * 
	 * @return the context to be passed to the computers
	 * @deprecated implement {@link #createContext(IQuickAssistInvocationContext, IProgressMonitor)}
	 */
	@Deprecated
	protected AssistInvocationContext createContext() {
		return null;
	}
	
	
	@Override
	public ICompletionProposal[] computeQuickAssistProposals(final IQuickAssistInvocationContext invocationContext) {
		fErrorMessage = null;
		
		AssistInvocationContext context = createContext();
		if (context == null) {
			context = createContext(invocationContext, new NullProgressMonitor());
		}
		if (context == null) {
			return null;
		}
		final ISourceViewer viewer = context.getSourceViewer();
		if (viewer == null) {
			return null;
		}
		final List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		final IAnnotationModel model = viewer.getAnnotationModel();
		if (model != null) {
			addAnnotationProposals(proposals, context, model);
		}
		if (context.getModelInfo() != null) {
			addModelAssistProposals(proposals, context);
		}
		
		if (proposals.isEmpty()) {
			return null;
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}
	
	protected boolean isMatchingPosition(final Position pos, final int offset) {
		return (pos != null)
				&& (offset >= pos.getOffset())
				&& (offset <= pos.getOffset()+pos.getLength());
	}
	
	private void addAnnotationProposals(final List<ICompletionProposal> proposals,
			final IQuickAssistInvocationContext invocationContext,
			final IAnnotationModel model) {
		final int offset = invocationContext.getOffset();
		final Iterator<Annotation> iter = model.getAnnotationIterator();
		while (iter.hasNext()) {
			final Annotation annotation = iter.next();
			if (annotation.isMarkedDeleted()) {
				continue;
			}
			final String type = annotation.getType();
			if (type.equals(SpellingAnnotation.TYPE)) {
				if (!isMatchingPosition(model.getPosition(annotation), offset)) {
					continue;
				}
				if (annotation instanceof SpellingAnnotation) {
					final SpellingProblem problem = ((SpellingAnnotation) annotation).getSpellingProblem();
					final ICompletionProposal[] annotationProposals = problem.getProposals(invocationContext);
					if (annotationProposals != null && annotationProposals.length > 0) {
						proposals.addAll(Arrays.asList(annotationProposals));
					}
				}
			}
		}
	}
	
	protected void addModelAssistProposals(final List<ICompletionProposal> proposals, final AssistInvocationContext context) {
	}
	
	@Override
	public String getErrorMessage() {
		return fErrorMessage;
	}
	
}
