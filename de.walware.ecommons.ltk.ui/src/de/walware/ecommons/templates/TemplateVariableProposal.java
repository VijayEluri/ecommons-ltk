/*=============================================================================#
 # Copyright (c) 2000-2016 IBM Corporation and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.templates;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;


/**
 * A proposal for insertion of template variables.
 */
public final class TemplateVariableProposal implements ICompletionProposal {
	
	
	private final TemplateVariableResolver fVariable;
	private final int fOffset;
	private final int fLength;
	private final ITextViewer fViewer;
	
	private Point fSelection;
	
	
	/**
	 * Creates a template variable proposal.
	 * 
	 * @param variable the template variable
	 * @param offset the offset to replace
	 * @param length the length to replace
	 * @param viewer the viewer
	 */
	public TemplateVariableProposal(final TemplateVariableResolver variable, final int offset, final int length, final ITextViewer viewer) {
		fVariable = variable;
		fOffset = offset;
		fLength = length;
		fViewer = viewer;
	}
	
	
	@Override
	public void apply(final IDocument document) {
		try {
			final String variable = fVariable.getType().equals("dollar") ? "$$" : "${" + fVariable.getType() + '}'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			document.replace(fOffset, fLength, variable);
			fSelection = new Point(fOffset + variable.length(), 0);
		}
		catch (final BadLocationException e) {
			final Shell shell = fViewer.getTextWidget().getShell();
			MessageDialog.openError(shell, TemplateMessages.TemplateVariableProposal_error_title, e.getMessage());
		}
	}
	
	@Override
	public Point getSelection(final IDocument document) {
		return fSelection;
	}
	
	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}
	
	@Override
	public String getDisplayString() {
		return fVariable.getType() + " - " + fVariable.getDescription(); //$NON-NLS-1$
	}
	
	@Override
	public Image getImage() {
		return null;
	}
	
	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
	
}
