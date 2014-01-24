/*=============================================================================#
 # Copyright (c) 2000-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     IBM Corporation - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.core.refactoring;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import de.walware.ecommons.ltk.internal.core.refactoring.Messages;


public class DynamicValidationChange extends CompositeChange implements IResourceChangeListener {
	
	// 30 minutes
	private static final long LIFE_TIME = 30L * 60L * 1000000000L;
	
	
	private RefactoringStatus fValidationState = null;
	private long fTimeStamp;
	
	
	public DynamicValidationChange(final Change change) {
		super(change.getName());
		add(change);
		markAsSynthetic();
	}
	
	public DynamicValidationChange(final String name) {
		super(name);
		markAsSynthetic();
	}
	
	public DynamicValidationChange(final String name, final Change[] changes) {
		super(name, changes);
		markAsSynthetic();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initializeValidationData(final IProgressMonitor pm) {
		super.initializeValidationData(pm);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		fTimeStamp = System.nanoTime();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RefactoringStatus isValid(final IProgressMonitor pm) throws CoreException {
		if (fValidationState == null) {
			return super.isValid(pm);
		}
		return fValidationState;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Change createUndoChange(final Change[] childUndos) {
		final DynamicValidationChange result= new DynamicValidationChange(getName());
		for (int i= 0; i < childUndos.length; i++) {
			result.add(childUndos[i]);
		}
		return result;
	}
	
	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		if (System.nanoTime() - fTimeStamp < LIFE_TIME) {
			return;
		}
		fValidationState = RefactoringStatus.createFatalErrorStatus(
				Messages.DynamicValidationState_WorkspaceChanged_message);
		
		// remove listener from workspace tracker
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		
		// clear up the children to not hang onto too much memory
		final Change[] children = clear();
		for (int i= 0; i < children.length; i++) {
			final Change change = children[i];
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					change.dispose();
				}
				@Override
				public void handleException(final Throwable exception) {
				}
			});
		}
	}
	
}
