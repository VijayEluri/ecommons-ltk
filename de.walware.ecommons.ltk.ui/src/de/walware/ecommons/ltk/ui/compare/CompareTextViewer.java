/*******************************************************************************
 * Copyright (c) 2008-2013 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import de.walware.ecommons.preferences.ui.SettingsUpdater;
import de.walware.ecommons.text.ui.TextViewerJFaceUpdater;

import de.walware.ecommons.ltk.ui.sourceediting.SnippetEditor;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfigurator;
import de.walware.ecommons.ltk.ui.sourceediting.ViewerSourceEditorAdapter;


/**
 * Content viewer for source code using a {@link SourceEditorViewerConfigurator}
 * to setup the text viewer.
 */
public class CompareTextViewer extends Viewer {
	
	
	private final SourceViewer fSourceViewer;
	private final SourceEditorViewerConfigurator fConfigurator;
	
	private Object fInput;
	
	
	public CompareTextViewer(final Composite parent, final CompareConfiguration compareConfig,
			final SourceEditorViewerConfigurator viewerConfig) {
		fConfigurator = viewerConfig;
		fSourceViewer = new SourceViewer(parent, null, SnippetEditor.DEFAULT_MULTI_LINE_STYLE);
		fSourceViewer.setEditable(false);
	}
	
	
	protected void initSourceViewer() {
		fConfigurator.setTarget(new ViewerSourceEditorAdapter(fSourceViewer, fConfigurator) {
			@Override
			public boolean isEditable(final boolean validate) {
				return (fInput instanceof IEditableContent && ((IEditableContent) fInput).isEditable());
			}
		});
		new TextViewerJFaceUpdater(fSourceViewer,
				fConfigurator.getSourceViewerConfiguration().getPreferences() );
		new SettingsUpdater(fConfigurator, fSourceViewer.getControl());
		
		fSourceViewer.activatePlugins();
	}
	
	@Override
	public Control getControl() {
		return fSourceViewer.getControl();
	}
	
	@Override
	public void setInput(final Object input) {
		fInput = input;
		if (input instanceof IStreamContentAccessor) {
			final Document document = new Document(
					CompareUtilities.readString((IStreamContentAccessor) input));
			fConfigurator.getDocumentSetupParticipant().setup(document);
			fSourceViewer.setDocument(document);
		}
	}
	
	@Override
	public Object getInput() {
		return fInput;
	}
	
	@Override
	public void setSelection(final ISelection selection, final boolean reveal) {
		fSourceViewer.setSelection(selection, reveal);
	}
	
	@Override
	public ISelection getSelection() {
		return fSourceViewer.getSelection();
	}
	
	@Override
	public void refresh() {
	}
	
}
