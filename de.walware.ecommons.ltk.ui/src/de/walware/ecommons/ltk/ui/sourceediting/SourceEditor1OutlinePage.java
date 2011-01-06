/*******************************************************************************
 * Copyright (c) 2007-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.CollapseAllHandler;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import de.walware.ecommons.FastList;
import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.actions.HandlerContributionItem;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.AstInfo;
import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.IModelElementDelta;
import de.walware.ecommons.ltk.ISourceStructElement;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.IModelElement.Filter;
import de.walware.ecommons.ltk.internal.ui.EditingMessages;
import de.walware.ecommons.ltk.ui.IModelElementInputListener;
import de.walware.ecommons.ltk.ui.ISelectionWithElementInfoListener;
import de.walware.ecommons.ltk.ui.LTKInputData;


/**
 * Abstract content outline page for a {@link SourceEditor1} with model info.
 */
public abstract class SourceEditor1OutlinePage extends Page
		implements IContentOutlinePage, IAdaptable, ISourceEditorAssociated,
			IShowInSource, IShowInTargetList, IShowInTarget,
			IPostSelectionProvider, IModelElementInputListener {
	
	
	protected class OutlineContentProvider implements ITreeContentProvider {
		
		public OutlineContentProvider() {
		}
		
		public long getStamp(final Object inputElement) {
			if (inputElement instanceof ISourceUnit) {
				final ISourceUnitModelInfo info = ((ISourceUnit) inputElement).getModelInfo(fMainType, 0, null); 
				if (info != null) {
					return info.getStamp();
				}
			}
			return ISourceUnit.UNKNOWN_MODIFICATION_STAMP;
		}
		
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		}
		
		public Object[] getElements(final Object inputElement) {
			if (inputElement instanceof ISourceUnit) {
				final ISourceUnitModelInfo info = ((ISourceUnit) inputElement).getModelInfo(fMainType, 0, null); 
				if (info != null) {
					fCurrentModelStamp = info.getStamp();
					final List<? extends ISourceStructElement> children = info.getSourceElement().getSourceChildren(getContentFilter());
					return children.toArray(new ISourceStructElement[children.size()]);
				}
			}
			return new Object[0];
		}
		
		public void dispose() {
		}
		
		public Object getParent(final Object element) {
			final ISourceStructElement o = (ISourceStructElement) element;
			return o.getSourceParent();
		}
		
		public boolean hasChildren(final Object element) {
			final ISourceStructElement o = (ISourceStructElement) element;
			return o.hasSourceChildren(getContentFilter());
		}
		
		public Object[] getChildren(final Object parentElement) {
			final ISourceStructElement o = (ISourceStructElement) parentElement;
			final List<? extends ISourceStructElement> children = o.getSourceChildren(getContentFilter());
			return children.toArray(new ISourceStructElement[children.size()]);
		}
	}
	
	public class AstContentProvider extends OutlineContentProvider {
		
		
		public AstContentProvider() {
			super();
		}
		
		@Override
		public long getStamp(final Object inputElement) {
			if (inputElement instanceof ISourceUnit) {
				final AstInfo info = ((ISourceUnit) inputElement).getAstInfo(fMainType, false, null); 
				if (info != null) {
					return info.stamp;
				}
			}
			return ISourceUnit.UNKNOWN_MODIFICATION_STAMP;
		}
		
		@Override
		public Object[] getElements(final Object inputElement) {
			if (inputElement instanceof ISourceUnit) {
				final AstInfo info = ((ISourceUnit) inputElement).getAstInfo(fMainType, false, null); 
				if (info != null) {
					fCurrentModelStamp = info.stamp;
					return new Object[] { info.root };
				}
			}
			return new Object[0];
		}
		
	}
	
	
	private static class SelectionChangeNotify extends SafeRunnable implements ISelectionChangedListener {
		
		
		private final FastList<ISelectionChangedListener> fSelectionListeners;
		
		private SelectionChangedEvent fCurrentEvent;
		private ISelectionChangedListener fCurrentListener;
		
		
		public SelectionChangeNotify(final FastList<ISelectionChangedListener> listenerList) {
			fSelectionListeners = listenerList;
		}
		
		
		public void selectionChanged(final SelectionChangedEvent event) {
			fCurrentEvent = event;
			final ISelectionChangedListener[] listeners = fSelectionListeners.toArray();
			for (int i = 0; i < listeners.length; i++) {
				fCurrentListener = listeners[i];
				SafeRunner.run(this);
			}
		}
		
		public void run() {
			fCurrentListener.selectionChanged(fCurrentEvent);
		}
		
	}
	
	private class DefaultSelectionListener implements ISelectionChangedListener {
		
		public void selectionChanged(final SelectionChangedEvent event) {
			if (!fIgnoreSelection) {
				selectInEditor(event.getSelection());
			}
		}
		
	}
	
	protected abstract class ToggleAction extends Action {
		
		private final String fSettingsKey;
		private final int fTime;
		
		public ToggleAction(final String checkSettingsKey, final boolean checkSettingsDefault, 
				final int expensive) {
			assert (checkSettingsKey != null);
			
			fSettingsKey = checkSettingsKey;
			fTime = expensive;
			
			final IDialogSettings settings = getSettings();
			final boolean on = (settings.get(fSettingsKey) == null) ?
					checkSettingsDefault : getSettings().getBoolean(fSettingsKey);
			setChecked(on);
			configure(on);
		}
		
		protected void init() {
		}
		
		@Override
		public void run() {
			final Runnable runnable = new Runnable() {
				public void run() {
					final boolean on = isChecked();
					configure(on);
					getSettings().put(fSettingsKey, on); 
				}
			};
			if (fTime == 0) {
				runnable.run();
			}
			else {
				BusyIndicator.showWhile(Display.getCurrent(), runnable);
			}
		}
		
		protected abstract void configure(boolean on);
		
	}
	
	private class SyncWithEditorAction extends ToggleAction implements ISelectionWithElementInfoListener {
		
		public SyncWithEditorAction() {
			super("sync.editor", true, 0); //$NON-NLS-1$
			setText(EditingMessages.SyncWithEditor_label);
			setImageDescriptor(SharedUIResources.getImages().getDescriptor(SharedUIResources.LOCTOOL_SYNCHRONIZED_IMAGE_ID));
		}
		
		@Override
		protected void configure(final boolean on) {
			if (on) {
				fEditor.addPostSelectionWithElementInfoListener(this);
			}
			else {
				fEditor.removePostSelectionWithElementInfoListener(this);
			}
		}
		
		public void inputChanged() {
		}
		
		public void stateChanged(final LTKInputData state) {
			if (!state.isStillValid()) {
				return;
			}
			if (fCurrentModelStamp != state.getInputInfo().getStamp()) {
				elementUpdatedInfo(state.getInputElement(), null);
			}
			UIAccess.getDisplay().syncExec(new Runnable() {
				public void run() {
					if (state.isStillValid() && isChecked()) {
						select(state.getModelSelection());
					}
				}
			});
		}
		
	}
	
	
	private final SourceEditor1 fEditor;
	private final String fMainType;
	private OutlineContentProvider fContentProvider;
	
	private TreeViewer fTreeViewer;
	private ISelection fCurrentSelection;
	
	private long fCurrentModelStamp;
	private final FastList<ISelectionChangedListener> fSelectionListeners = new FastList<ISelectionChangedListener>(ISelectionChangedListener.class);
	private final ISelectionChangedListener fSelectionListener = new SelectionChangeNotify(fSelectionListeners);
	private final FastList<ISelectionChangedListener> fPostSelectionListeners = new FastList<ISelectionChangedListener>(ISelectionChangedListener.class);
	private final ISelectionChangedListener fPostSelectionListener = new SelectionChangeNotify(fPostSelectionListeners);
	private boolean fIgnoreSelection;
	
	private IModelElement fInputUnit;
	
	private final String fContextMenuID;
	private Menu fContextMenu;
	
	private FastList<IHandler2> fHandlersToUpdate;
	private SyncWithEditorAction fSyncWithEditorAction;
	
	
	public SourceEditor1OutlinePage(final SourceEditor1 editor, final String mainType, final String contextMenuId) {
		if (editor == null) {
			throw new NullPointerException();
		}
		if (mainType == null) {
			throw new NullPointerException();
		}
		fEditor = editor;
		fMainType = mainType;
		fContextMenuID = contextMenuId;
		fHandlersToUpdate = new FastList<IHandler2>(IHandler2.class);
	}
	
	
	@Override
	public void init(final IPageSite pageSite) {
		super.init(pageSite);
		pageSite.setSelectionProvider(this);
	}
	
	protected abstract IDialogSettings getSettings();
	
	protected IModelElement.Filter<ISourceStructElement> getContentFilter() {
		return null;
	}
	
	@Override
	public void createControl(final Composite parent) {
		final TreeViewer viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		viewer.setUseHashlookup(true);
		configureViewer(viewer);
		ColumnViewerToolTipSupport.enableFor(viewer);
		
		fTreeViewer = viewer;
		fContentProvider = createContentProvider();
		viewer.setContentProvider(fContentProvider);
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				fCurrentSelection = event.getSelection();
			}
		});
		initActions();
		fSelectionListeners.add(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				if (getControl().isVisible()) {
					final EvaluationContext evaluationContext = new EvaluationContext(null, event.getSelection());
					evaluationContext.addVariable(ISources.ACTIVE_SITE_NAME, getSite());
					evaluationContext.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, event.getSelection());
					final IHandler2[] handlers = fHandlersToUpdate.toArray();
					for (final IHandler2 handler : handlers) {
						handler.setEnabled(evaluationContext);
					}
				}
			}
		});
		
		hookContextMenu();
		
		fEditor.getModelInputProvider().addListener(this);
		viewer.setInput(fInputUnit);
	}
	
	private void hookContextMenu() {
		final MenuManager menuManager = new MenuManager(fContextMenuID, fContextMenuID);
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager m) {
				contextMenuAboutToShow(m);
			}
		});
		fContextMenu = menuManager.createContextMenu(fTreeViewer.getTree());
		fTreeViewer.getTree().setMenu(fContextMenu);
		getSite().registerContextMenu(fContextMenuID, menuManager, fTreeViewer);
	}
	
	@Override
	public Control getControl() {
		if (fTreeViewer != null) {
			return fTreeViewer.getControl();
		}
		return null;
	}
	
	@Override
	public void setFocus() {
		final TreeViewer viewer = getViewer();
		if (UIAccess.isOkToUse(viewer)) {
			viewer.getTree().setFocus();
		}
	}
	
	protected OutlineContentProvider createContentProvider() {
		return new OutlineContentProvider();
	}
	
	protected abstract void configureViewer(TreeViewer viewer);
	
	
	protected void initActions() {
		final TreeViewer viewer = getViewer();
		final IPageSite site = getSite();
		fPostSelectionListeners.add(new DefaultSelectionListener());
		viewer.addSelectionChangedListener(fSelectionListener);
		viewer.addPostSelectionChangedListener(fPostSelectionListener);
		
		final IHandlerService handlerSvc = (IHandlerService) site.getService(IHandlerService.class);
		final IActionBars actionBars = getSite().getActionBars();
		actionBars.setGlobalActionHandler(ITextEditorActionConstants.UNDO, fEditor.getAction(ITextEditorActionConstants.UNDO));
		actionBars.setGlobalActionHandler(ITextEditorActionConstants.REDO, fEditor.getAction(ITextEditorActionConstants.REDO));
		
//		actionBars.setGlobalActionHandler(ITextEditorActionConstants.NEXT, fEditor.getAction(ITextEditorActionConstants.NEXT));
		actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_NEXT_ANNOTATION, fEditor.getAction(ITextEditorActionConstants.NEXT));
//		actionBars.setGlobalActionHandler(ITextEditorActionConstants.PREVIOUS, fEditor.getAction(ITextEditorActionConstants.PREVIOUS));
		actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_PREVIOUS_ANNOTATION, fEditor.getAction(ITextEditorActionConstants.PREVIOUS));
		
		fSyncWithEditorAction = new SyncWithEditorAction();
		
		final CollapseAllHandler collapseAllHandler = new CollapseAllHandler(getViewer()) {
			@Override
			public Object execute(final ExecutionEvent event) {
				final TreeViewer viewer = getViewer();
				if (UIAccess.isOkToUse(viewer)) {
					fIgnoreSelection = true;
					final Object result = super.execute(event);
					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {
							fIgnoreSelection = false;
						};
					});
					return result;
				}
				return null;
			}
		};
		handlerSvc.activateHandler(CollapseAllHandler.COMMAND_ID, collapseAllHandler);
		
		final IToolBarManager toolBarManager = site.getActionBars().getToolBarManager();
		final IMenuManager menuManager = site.getActionBars().getMenuManager();
		
		toolBarManager.add(new Separator(SharedUIResources.VIEW_EXPAND_MENU_ID)); 
		toolBarManager.add(new HandlerContributionItem(new CommandContributionItemParameter(
				site, null, CollapseAllHandler.COMMAND_ID, HandlerContributionItem.STYLE_PUSH), collapseAllHandler));
		toolBarManager.add(new Separator(SharedUIResources.VIEW_SORT_MENU_ID)); 
		final Separator viewFilter = new Separator(SharedUIResources.VIEW_FILTER_MENU_ID); 
		viewFilter.setVisible(false);
		toolBarManager.add(viewFilter);
		
		menuManager.add(fSyncWithEditorAction);
	}
	
	protected void contextMenuAboutToShow(final IMenuManager m) {
	}
	
	
	public void elementChanged(final IModelElement element) {
		fInputUnit = element;
		fCurrentModelStamp = ISourceUnit.UNKNOWN_MODIFICATION_STAMP;
		final TreeViewer viewer = getViewer();
		if (UIAccess.isOkToUse(viewer)) {
			viewer.setInput(fInputUnit);
		}
	}
	
	public void elementInitialInfo(final IModelElement element) {
		elementUpdatedInfo(element, null);
	}
	
	public void elementUpdatedInfo(final IModelElement element, final IModelElementDelta delta) {
		if (element != fInputUnit || (element == null && fInputUnit == null)) {
			return;
		}
		final Display display = UIAccess.getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				final TreeViewer viewer = getViewer();
				
				if (element != fInputUnit 
						|| !UIAccess.isOkToUse(viewer)
						|| (fCurrentModelStamp != ISourceUnit.UNKNOWN_MODIFICATION_STAMP && fContentProvider.getStamp(element) == fCurrentModelStamp)) {
					return;
				}
				fIgnoreSelection = true;
				viewer.refresh(true);
				fIgnoreSelection = false;
			}
		});
	}
	
	@Override
	public void dispose() {
		if (fHandlersToUpdate != null) {
			fHandlersToUpdate.clear();
			fHandlersToUpdate = null;
		}
		fEditor.getModelInputProvider().removeListener(this);
		fEditor.handleOutlinePageClosed();
		fPostSelectionListeners.clear();
		
		if (fContextMenu != null && !fContextMenu.isDisposed()) {
			fContextMenu.dispose();
			fContextMenu = null;
		}
	}
	
	
	protected TreeViewer getViewer() {
		return fTreeViewer;
	}
	
	protected void selectInEditor(final ISelection selection) {
		fEditor.setSelection(selection, fSyncWithEditorAction);
	}
	
	protected void select(ISourceStructElement element) {
		final TreeViewer viewer = fTreeViewer;
		if (UIAccess.isOkToUse(viewer)) {
			fIgnoreSelection = true;
			try {
				final Filter filter = getContentFilter();
				Object selectedElement = null;
				final IStructuredSelection currentSelection = ((IStructuredSelection) viewer.getSelection());
				if (currentSelection.size() == 1) {
					selectedElement = currentSelection.getFirstElement();
				}
				while (element != null 
						&& (element.getElementType() & IModelElement.MASK_C2) != IModelElement.C2_SOURCE_FILE) {
					if (selectedElement != null && element.equals(selectedElement)) {
						return;
					}
					if (filter == null || filter.include(element)) {
						selectedElement = null;
						viewer.setSelection(new StructuredSelection(element), true);
						if (!viewer.getSelection().isEmpty()) {
							return;
						}
					}
					final IModelElement parent = element.getSourceParent();
					if (parent instanceof ISourceStructElement) {
						element = (ISourceStructElement) parent;
						continue;
					}
					else {
						break;
					}
				}
				if (!viewer.getSelection().isEmpty()) {
					viewer.setSelection(StructuredSelection.EMPTY);
				}
			}
			finally {
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						fIgnoreSelection = false;
					}
				});
			}
		}
	}
	
	protected void registerHandlerToUpdate(final IHandler2 handler) {
		fHandlersToUpdate.add(handler);
	}
	
	
	public void setSelection(final ISelection selection) {
		if (fTreeViewer != null) {
			fTreeViewer.setSelection(selection);
		}
	}
	
	public ISelection getSelection() {
		final ISelection selection = fCurrentSelection;
		if (selection != null) {
			return selection;
		}
		if (fTreeViewer != null) {
			return fTreeViewer.getSelection();
		}
		return null;
	}
	
	
	public void addSelectionChangedListener(final ISelectionChangedListener listener) {
		fSelectionListeners.add(listener);
	}
	
	public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
		fSelectionListeners.remove(listener);
	}
	
	public void addPostSelectionChangedListener(final ISelectionChangedListener listener) {
		fPostSelectionListeners.add(listener);
	}
	
	public void removePostSelectionChangedListener(final ISelectionChangedListener listener) {
		fPostSelectionListeners.remove(listener);
	}
	
	public ShowInContext getShowInContext() {
		return new ShowInContext(fEditor.getEditorInput(), null);
	}
	
	public String[] getShowInTargetIds() {
		return new String[] { IPageLayout.ID_PROJECT_EXPLORER };
	}
	
	public boolean show(final ShowInContext context) {
		final IModelElement inputUnit = fInputUnit;
		final ISelection selection = context.getSelection();
		if (selection instanceof LTKInputData) {
			final LTKInputData data = (LTKInputData) selection;
			data.update();
			if (inputUnit.equals(data.getInputElement())) {
				select(data.getModelSelection());
				return true;
			}
		}
		return false;
	}
	
	
	public ISourceEditor getSourceEditor() {
		return fEditor;
	}
	
	public Object getAdapter(final Class required) {
		if (ISourceEditorAssociated.class.equals(required)) {
			return this;
		}
		return null;
	}
	
}
