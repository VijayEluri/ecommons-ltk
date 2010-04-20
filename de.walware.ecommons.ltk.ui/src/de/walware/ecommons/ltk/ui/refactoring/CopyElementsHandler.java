/*******************************************************************************
 * Copyright (c) 2008-2010 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.refactoring;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ltk.ISourceStructElement;
import de.walware.ecommons.ltk.core.refactoring.RefactoringAdapter;
import de.walware.ecommons.ltk.internal.ui.refactoring.Messages;
import de.walware.ecommons.ltk.ui.util.LTKSelectionUtil;
import de.walware.ecommons.ltk.ui.util.WorkbenchUIUtil;


/**
 * Command handler copying selected elements to clipboard.
 */
public class CopyElementsHandler extends AbstractElementsHandler {
	
	
	public CopyElementsHandler(final RefactoringAdapter ltk) {
		super(ltk);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEnabled(final Object context) {
		final ISelection selection = WorkbenchUIUtil.getCurrentSelection(context);
		if (selection != null) {
			setBaseEnabled(!selection.isEmpty());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISelection selection = WorkbenchUIUtil.getCurrentSelection(event.getApplicationContext());
		if (selection == null || selection.isEmpty()) {
			return null;
		}
		final ISourceStructElement[] sourceElements = LTKSelectionUtil.getSelectedSourceStructElements(selection);
		if (sourceElements != null) {
			try {
				final String code = getRefactoringAdapter().getSourceCodeStringedTogether(sourceElements, null);
				copyToClipboard(event, code);
			}
			catch (final CoreException e) {
				StatusManager.getManager().handle(new Status(
						IStatus.ERROR, getRefactoringAdapter().getPluginIdentifier(), -1,
						Messages.CopyElements_error_message, e),
						StatusManager.LOG | StatusManager.SHOW);
			}
		}
		return null;
	}
	
}
