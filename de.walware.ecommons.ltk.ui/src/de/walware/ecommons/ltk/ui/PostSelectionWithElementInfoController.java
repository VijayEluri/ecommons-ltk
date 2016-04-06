/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.FastList;
import de.walware.ecommons.ICommonStatusConstants;

import de.walware.ecommons.ltk.IModelElementDelta;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;
import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;


/**
 * Controller implementation combining {@link IPostSelectionProvider} and 
 * {@link IModelElementInputProvider} to provide support for
 * {@link ISelectionWithElementInfoListener}.
 */
public class PostSelectionWithElementInfoController {
	
	
	private class SelectionTask extends Job {
		
		
		private final class Data extends LTKInputData {
			
			int fStateNr;
			
			Data(final ISourceUnit input, final SelectionChangedEvent currentSelection, final int runNr) {
				super(input, (currentSelection != null) ? currentSelection.getSelection() : null);
				fStateNr = runNr;
			}
			
			@Override
			public boolean isStillValid() {
				return (fCurrentNr == fStateNr);
			}
			
		}
		
		private int fLastNr;
		
		
		public SelectionTask() {
			super("PostSelection with Model Updater"); // //$NON-NLS-1$
			setPriority(Job.SHORT);
			setSystem(true);
			setUser(false);
			
			fLastNr = fCurrentNr = Integer.MIN_VALUE;
		}
		
		
		@Override
		protected synchronized IStatus run(final IProgressMonitor monitor) {
			ISourceUnit input = null;
			try {
				checkNewInput();
				
				final Data run;
				IgnoreActivation[] ignore = null;
				synchronized (fInputLock) {
					run = new Data(fInput, fCurrentSelection, fCurrentNr);
					if (run.fInputElement == null || run.fSelection == null
							|| (fLastNr == run.fStateNr && fNewListeners.isEmpty())) {
						return Status.OK_STATUS;
					}
					
					if (!fIgnoreList.isEmpty()) {
						int num = fIgnoreList.size();
						ignore = fIgnoreList.toArray(new IgnoreActivation[num]);
						for (int i = num-1; i >= 0; i--) {
							if (ignore[i].marked && ignore[i].nr != run.fStateNr) {
								fIgnoreList.remove(i);
								ignore[i] = null;
								num--;
							}
						}
						if (num == 0) {
							ignore = null;
						}
					}
					
					input = run.fInputElement;
					input.connect(monitor);
				}
				if (run.getInputInfo() == null
						|| run.getInputInfo().getStamp().getSourceStamp() != input.getDocument(null).getModificationStamp()) {
					return Status.OK_STATUS;
				}
				
				ISelectionWithElementInfoListener[] listeners = fNewListeners.clear();
				if (run.fStateNr != fLastNr) {
					listeners = fListeners.toArray();
					fLastNr = run.fStateNr;
				}
				ITER_LISTENER : for (int i = 0; i < listeners.length; i++) {
					if (ignore != null) {
						for (int j = 0; j < ignore.length; j++) {
							if (ignore[j] != null && ignore[j].listener == listeners[i]) {
								continue ITER_LISTENER;
							}
						}
					}
					if (!run.isStillValid()) {
						return Status.CANCEL_STATUS;
					}
					try {
						listeners[i].stateChanged(run);
					}
					catch (final Exception e) {
						logListenerError(e);
					}
				}
			}
			finally {
				if (input != null) {
					input.disconnect(monitor);
				}
			}
			
			return Status.OK_STATUS;
		}
		
		private void checkNewInput() {
			if (fInputChanged) {
				synchronized (fInputLock) {
					fInputChanged = false;
				}
				final ISelectionWithElementInfoListener[] listeners = fListeners.toArray();
				for (int i = 0; i < listeners.length; i++) {
					try {
						listeners[i].inputChanged();
					}
					catch (final Exception e) {
						logListenerError(e);
					}
				}
			}
		}
	}
	
	private class SelectionListener implements ISelectionChangedListener {
		
		private boolean active;
		
		@Override
		public void selectionChanged(final SelectionChangedEvent event) {
			if (!active) {
				return;
			}
			synchronized (PostSelectionWithElementInfoController.this) {
				if (fCurrentSelection != null && fCurrentSelection.getSelection().equals(event.getSelection())) {
					return;
				}
				fCurrentNr++;
				fCurrentSelection = event;
				fUpdateJob.schedule();
			}
		}
	};
	
	public class IgnoreActivation {
		
		private final ISelectionWithElementInfoListener listener;
		private boolean marked;
		private int nr;
		
		private IgnoreActivation(final ISelectionWithElementInfoListener listener) {
			this.listener = listener;
		}
		
		public void deleteNext() {
			nr = fCurrentNr;
			marked = true;
		}
		
		public void delete() {
			nr = fCurrentNr-1;
			marked = true;
			synchronized (fInputLock) {
				fIgnoreList.remove(this);
			}
		}
		
	}
	
	private final IPostSelectionProvider fSelectionProvider;
	private final IModelElementInputProvider fModelProvider;
	private final FastList<ISelectionWithElementInfoListener> fListeners = new FastList<>(ISelectionWithElementInfoListener.class);
	private final FastList<ISelectionWithElementInfoListener> fNewListeners = new FastList<>(ISelectionWithElementInfoListener.class);
	private final Object fInputLock = new Object();
	
	private final IModelElementInputListener fElementChangeListener;
	private final SelectionListener fSelectionListener;
	private final SelectionListener fPostSelectionListener;
	private PostSelectionCancelExtension fCancelExtension;
	
	private final List<IgnoreActivation> fIgnoreList = new ArrayList<>();
	
	private ISourceUnit fInput; // current input
	private boolean fInputChanged;
	private SelectionChangedEvent fCurrentSelection; // current selection
	private volatile int fCurrentNr; // stamp to check, if information still up-to-date
	
	private final SelectionTask fUpdateJob = new SelectionTask();
	
	
	public PostSelectionWithElementInfoController(final IModelElementInputProvider modelProvider,
			final IPostSelectionProvider selectionProvider, final PostSelectionCancelExtension cancelExt) {
		fSelectionProvider = selectionProvider;
		
		fModelProvider = modelProvider;
		
		fElementChangeListener = new IModelElementInputListener() {
			@Override
			public void elementChanged(final IModelElement element) {
				synchronized (fInputLock) {
					if (fUpdateJob.getState() == Job.WAITING) {
						fUpdateJob.cancel();
					}
					fInput = (ISourceUnit) element;
					fInputChanged = true;
					fCurrentNr++;
					fCurrentSelection = null;
					fUpdateJob.schedule();
				}
			}
			@Override
			public void elementInitialInfo(final IModelElement element) {
				checkUpdate(element);
			}
			@Override
			public void elementUpdatedInfo(final IModelElement element, final IModelElementDelta delta) {
				checkUpdate(element);
			}
			private void checkUpdate(final IModelElement element) {
				synchronized (fInputLock) {
					fCurrentNr++;
					if (fCurrentSelection == null) {
						return;
					}
				}
				fUpdateJob.run(null);
			}
		};
		fSelectionListener = new SelectionListener();
		fSelectionListener.active = false;
		fSelectionProvider.addSelectionChangedListener(fSelectionListener);
		
		fPostSelectionListener = new SelectionListener();
		fPostSelectionListener.active = true;
		fSelectionProvider.addPostSelectionChangedListener(fPostSelectionListener);
		
		fModelProvider.addListener(fElementChangeListener);
		if (cancelExt != null) {
			fCancelExtension = cancelExt;
			fCancelExtension.fController = this;
			fCancelExtension.init();
		}
	}
	
	
	public void setUpdateOnSelection(final boolean active) {
		fSelectionListener.active = active;
	}
	
	public void setUpdateOnPostSelection(final boolean active) {
		fPostSelectionListener.active = active;
	}
	
	public void cancel() {
		synchronized (fInputLock) {
			fCurrentNr++;
			fCurrentSelection = null;
		}
	}
	
	public void dispose() {
		cancel();
		fModelProvider.removeListener(fElementChangeListener);
		fSelectionProvider.removeSelectionChangedListener(fSelectionListener);
		fSelectionProvider.removePostSelectionChangedListener(fPostSelectionListener);
		if (fCancelExtension != null) {
			fCancelExtension.dispose();
		}
		fNewListeners.clear();
		fListeners.clear();
	}
	
	
	public void addListener(final ISelectionWithElementInfoListener listener) {
		fListeners.add(listener);
		fNewListeners.add(listener);
		fUpdateJob.schedule();
	}
	
	public void removeListener(final ISelectionWithElementInfoListener listener) {
		fNewListeners.remove(listener);
		fListeners.remove(listener);
	}
	
	public IgnoreActivation ignoreNext(final ISelectionWithElementInfoListener listener) {
		final IgnoreActivation control = new IgnoreActivation(listener);
		fIgnoreList.add(control);
		return control;
	}
	
	
	private void logListenerError(final Throwable e) {
		StatusManager.getManager().handle(new Status(
				IStatus.ERROR, LTKUIPlugin.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN,
				"An error occurred when calling a registered listener.", e)); //$NON-NLS-1$
	}
	
}
