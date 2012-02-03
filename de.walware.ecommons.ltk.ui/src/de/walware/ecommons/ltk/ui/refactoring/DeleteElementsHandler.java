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

package de.walware.ecommons.ltk.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.participants.DeleteProcessor;
import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ltk.ISourceStructElement;
import de.walware.ecommons.ltk.core.refactoring.CommonRefactoringFactory;
import de.walware.ecommons.ltk.core.refactoring.RefactoringAdapter;
import de.walware.ecommons.ltk.internal.ui.refactoring.Messages;
import de.walware.ecommons.ltk.ui.util.LTKSelectionUtil;
import de.walware.ecommons.ltk.ui.util.WorkbenchUIUtil;


/**
 * Command handler deleting selected elements.
 */
public class DeleteElementsHandler extends AbstractElementsHandler {
	
	
	private final CommonRefactoringFactory fRefactoringFactory;
	
	
	public DeleteElementsHandler(final RefactoringAdapter refactoringAdapter, final CommonRefactoringFactory refactoringFactory) {
		super(refactoringAdapter);
		
		fRefactoringFactory = refactoringFactory;
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
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISelection selection = WorkbenchUIUtil.getCurrentSelection(event.getApplicationContext());
		if (selection == null || selection.isEmpty()) {
			return null;
		}
		final ISourceStructElement[] sourceElements = LTKSelectionUtil.getSelectedSourceStructElements(selection);
		if (sourceElements != null) {
			final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
			final IWorkbenchPartSite site = activePart.getSite();
			final Shell shell = site.getShell();
			final IProgressService progressService = (IProgressService) site.getService(IProgressService.class);
			try {
				startCutRefactoring(sourceElements, shell, progressService);
			}
			catch (final InvocationTargetException e) {
				StatusManager.getManager().handle(new Status(
						IStatus.ERROR, getRefactoringAdapter().getPluginIdentifier(), -1,
						Messages.DeleteElements_error_message, 
						e.getCause()),
						StatusManager.LOG | StatusManager.SHOW);
			}
			catch (final InterruptedException e) {
			}
		}
		return null;
	}
	
	private void startCutRefactoring(final Object[] elements, final Shell shell, final IProgressService context) throws InvocationTargetException, InterruptedException {
		final DeleteProcessor processor = fRefactoringFactory.createDeleteProcessor(elements, getRefactoringAdapter());
		final Refactoring refactoring = new DeleteRefactoring(processor);
		final RefactoringExecutionHelper helper = new RefactoringExecutionHelper(refactoring, 
				RefactoringCore.getConditionCheckingFailedSeverity(), 
				shell, context);
		helper.perform(false, false);
	}
	
}
