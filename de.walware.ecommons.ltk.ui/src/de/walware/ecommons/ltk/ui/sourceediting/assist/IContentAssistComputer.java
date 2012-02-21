/*******************************************************************************
 * Copyright (c) 2000-2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Wahlbrink - adapted API and improvements
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting.assist;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


/**
 * Computes completions and context information displayed by the editor content assistant.
 * Contributions to the extension point <code>de.walware.ecommons.ltk.advancedContentAssist</code>
 * must implement this interface.
 */
public interface IContentAssistComputer {
	
	
	public static final int COMBINED_MODE = 0x1;
	public static final int SPECIFIC_MODE = 0x2;
	public static final int INFORMATION_MODE = 0x4;
	
	
	/**
	 * Informs the computer that a content assist session has started. This call will always be
	 * followed by a {@link #sessionEnded()} call, but not necessarily by calls to
	 * {@linkplain #computeCompletionProposals(ContentAssistInvocationContext, IProgressMonitor) computeCompletionProposals}
	 * or
	 * {@linkplain #computeContextInformation(ContentAssistInvocationContext, IProgressMonitor) computeContextInformation}.
	 */
	public void sessionStarted(ISourceEditor editor);
	
	/**
	 * Returns a list of completion proposals valid at the given invocation context.
	 * 
	 * @param context the context of the content assist invocation
	 * @param mode one of the mode constant defined in {@link IContentAssistComputer}
	 * @param proposals a set collecting the completion proposals
	 * @param monitor a progress monitor to report progress. The monitor is private to this
	 *     invocation, i.e. there is no need for the receiver to spawn a sub monitor.
	 */
	public IStatus computeCompletionProposals(AssistInvocationContext context, int mode, AssistProposalCollector<IAssistCompletionProposal> proposals, IProgressMonitor monitor);
	
	/**
	 * Returns context information objects valid at the given invocation context.
	 * 
	 * @param context the context of the content assist invocation
	 * @param proposals a set collecting the context information objects
	 * @param monitor a progress monitor to report progress. The monitor is private to this
	 *     invocation, i.e. there is no need for the receiver to spawn a sub monitor.
	 */
	public IStatus computeContextInformation(AssistInvocationContext context, AssistProposalCollector<IAssistInformationProposal> proposals, IProgressMonitor monitor);
	
	/**
	 * Informs the computer that a content assist session has ended. This call will always be after
	 * any calls to
	 * {@linkplain #computeCompletionProposals(ContentAssistInvocationContext, IProgressMonitor) computeCompletionProposals}
	 * and
	 * {@linkplain #computeContextInformation(ContentAssistInvocationContext, IProgressMonitor) computeContextInformation}.
	 */
	public void sessionEnded();
	
}
