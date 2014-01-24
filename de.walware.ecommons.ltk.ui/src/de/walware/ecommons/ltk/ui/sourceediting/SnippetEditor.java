/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IUpdate;

import de.walware.ecommons.preferences.ui.SettingsUpdater;
import de.walware.ecommons.text.ui.TextViewerAction;
import de.walware.ecommons.text.ui.TextViewerEditorColorUpdater;
import de.walware.ecommons.text.ui.TextViewerJFaceUpdater;
import de.walware.ecommons.ui.actions.ControlServicesUtil;
import de.walware.ecommons.ui.components.WidgetToolsButton;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.UIAccess;


/**
 * Text snippet editor (no Eclipse editor) supporting {@link SourceEditorViewerConfigurator}.
 */
public class SnippetEditor extends Object {
	
	public static final int DEFAULT_SINGLE_LINE_STYLE = SWT.BORDER | SWT.SINGLE | SWT.LEFT_TO_RIGHT;
	public static final int DEFAULT_MULTI_LINE_STYLE = SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.LEFT_TO_RIGHT;
	
	
	private static class ExtStyledText extends StyledText {
		
		
		private Color fSavedColor;
		
		
		public ExtStyledText(final Composite parent, final int style) {
			super(parent, style);
		}
		
		
		@Override
		public void setBackground(final Color color) {
			fSavedColor = color;
			if (isEnabled()) {
				super.setBackground(color);
			}
		}
		
		@Override
		public void setEnabled(final boolean enabled) {
			super.setEnabled(enabled);
			if (enabled) {
				super.setBackground(fSavedColor);
			}
			else {
				super.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
		}
		
	}
	
	
	private class Updater implements ISelectionChangedListener, IDocumentListener, Runnable {
		
		private boolean fActionUpdateScheduled = false;
		
		
		@Override
		public void selectionChanged(final SelectionChangedEvent event) {
			schedule();
		}
		
		@Override
		public void documentAboutToBeChanged(final DocumentEvent event) {
		}
		@Override
		public void documentChanged(final DocumentEvent event) {
			schedule();
		}
		
		
		public void schedule() {
			if (fActionUpdateScheduled) {
				return;
			}
			Display.getCurrent().asyncExec(this);
			fActionUpdateScheduled = true;
		}
		
		@Override
		public void run() {
			fActionUpdateScheduled = false;
			if (UIAccess.isOkToUse(fSourceViewer)) {
				for (final Action action : fGlobalActions.values()) {
					if (action instanceof IUpdate) {
						((IUpdate) action).update();
					}
				}
			}
		}
	}
	
	
	private final boolean fWithToolButton;
	
	private Composite fComposite;
	
	private final Document fDocument;
	private SourceViewer fSourceViewer;
	
	private final SourceEditorViewerConfigurator fConfigurator;
	private Map<String, Action> fGlobalActions;
	private Updater fUpdater;
	
	private final IServiceLocator fServiceLocator;
	private ControlServicesUtil fServiceUtil;
	
	
	/**
	 * Creates snippet editor with empty document.
	 */
	public SnippetEditor(final SourceEditorViewerConfigurator configurator) {
		this(configurator, null, null);
	}
	
	/**
	 * Creates snippet editor with initial content.
	 */
	public SnippetEditor(final SourceEditorViewerConfigurator configurator, final String initialContent,
			final IServiceLocator serviceParent) {
		this(configurator, initialContent, serviceParent, false);
	}
	
	/**
	 * Creates snippet editor with initial content.
	 */
	public SnippetEditor(final SourceEditorViewerConfigurator configurator, final String initialContent,
			final IServiceLocator serviceParent, final boolean withToolButton) {
		fConfigurator = configurator;
		fDocument = (initialContent != null) ? new Document(initialContent) : new Document();
		fConfigurator.getDocumentSetupParticipant().setup(fDocument);
		fServiceLocator = serviceParent;
		fWithToolButton = withToolButton;
	}
	
	
	public void create(final Composite parent, final int style) {
		if (fWithToolButton) {
			fComposite = new Composite(parent, SWT.NONE) {
				@Override
				public boolean setFocus() {
					return fSourceViewer.getTextWidget().setFocus();
				}
			};
			fComposite.setLayout(LayoutUtil.applySashDefaults(new GridLayout(), 2));
			
			createSourceViewer(fComposite, style);
			fSourceViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			final WidgetToolsButton button = new WidgetToolsButton(fSourceViewer.getTextWidget()) {
				@Override
				protected void fillMenu(final Menu menu) {
					SnippetEditor.this.fillToolMenu(menu);
				}
			}; 
			button.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		}
		else {
			createSourceViewer(parent, style);
			fComposite = fSourceViewer.getTextWidget();
		}
	}
	
	private void createSourceViewer(final Composite composite, final int style) {
		fSourceViewer = new SourceViewer(composite, null, style) {
			
			@Override
			protected StyledText createTextWidget(final Composite parent, final int styles) {
				final StyledText styledText = new ExtStyledText(parent, styles);
				styledText.setLeftMargin(Math.max(styledText.getLeftMargin(), 2));
				return styledText;
			}
			
		};
		fSourceViewer.setEditable(true);
		
		fSourceViewer.setDocument(fDocument);
		
		final ViewerSourceEditorAdapter adapter = new ViewerSourceEditorAdapter(fSourceViewer, fConfigurator);
		fConfigurator.setTarget(adapter);
		new TextViewerJFaceUpdater(fSourceViewer,
				fConfigurator.getSourceViewerConfiguration().getPreferences() );
		new TextViewerEditorColorUpdater(fSourceViewer,
				fConfigurator.getSourceViewerConfiguration().getPreferences() );
		new SettingsUpdater(fConfigurator, fSourceViewer.getControl());
		
		initActions();
		fSourceViewer.activatePlugins();
		fUpdater = new Updater();
		fSourceViewer.addSelectionChangedListener(fUpdater);
		fSourceViewer.getDocument().addDocumentListener(fUpdater);
	}
	
	public void addAction(final Action action) {
		fGlobalActions.put(action.getId(), action);
		final String commandId = action.getActionDefinitionId();
		if (fServiceUtil != null && commandId != null) {
			fServiceUtil.activateHandler(commandId, new ActionHandler(action));
		}
	}
	
	public Action getAction(final String id) {
		return fGlobalActions.get(id);
	}
	
	private void initActions() {
		fGlobalActions = new HashMap<String, Action>(10);
		
		if (fServiceLocator != null) {
			fServiceUtil = new ControlServicesUtil(fServiceLocator, getClass().getName()+'#'+hashCode(), getSourceViewer().getControl());
			fServiceUtil.addControl(getSourceViewer().getControl());
		}
		
		// default actions
		addAction(TextViewerAction.createUndoAction(fSourceViewer));
		addAction(TextViewerAction.createRedoAction(fSourceViewer));
		addAction(TextViewerAction.createCutAction(fSourceViewer));
		addAction(TextViewerAction.createCopyAction(fSourceViewer));
		addAction(TextViewerAction.createPasteAction(fSourceViewer));
		addAction(TextViewerAction.createSelectAllAction(fSourceViewer));
		
		// create context menu
		final MenuManager manager = new MenuManager(null, null);
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager mgr) {
				fillContextMenu(mgr);
			}
		});
		
		final StyledText text = fSourceViewer.getTextWidget();
		final Menu menu = manager.createContextMenu(text);
		text.setMenu(menu);
	}
	
	protected void fillContextMenu(final IMenuManager menu) {
		menu.add(new GroupMarker(ITextEditorActionConstants.GROUP_UNDO));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_UNDO, getAction(ITextEditorActionConstants.UNDO));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_UNDO, getAction(ITextEditorActionConstants.REDO));
		
		menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, getAction(ITextEditorActionConstants.CUT));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, getAction(ITextEditorActionConstants.COPY));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, getAction(ITextEditorActionConstants.PASTE));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, getAction(ITextEditorActionConstants.SELECT_ALL));
		
		menu.add(new Separator("assist")); //$NON-NLS-1$
		final Action action = fGlobalActions.get("ContentAssistProposal"); //$NON-NLS-1$
		if (action != null && action.getText() != null) {
			menu.appendToGroup("assist", action); //$NON-NLS-1$
		}
	}
	
	protected void fillToolMenu(final Menu menu) {
	}
	
	
	public Document getDocument() {
		return fDocument;
	}
	
	public SourceViewer getSourceViewer() {
		return fSourceViewer;
	}
	
	public Composite getControl() {
		return fComposite;
	}
	
	public StyledText getTextControl() {
		return fSourceViewer.getTextWidget();
	}
	
	public void reset() {
		fSourceViewer.resetPlugins();
		fUpdater.run();
	}
	
}
