/*******************************************************************************
 * Copyright (c) 2000-2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Wahlbrink
 *******************************************************************************/

// ORG: org.eclipse.jdt.internal.ui.preferences.JavaSourcePreviewerUpdater

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;


/**
 * Handles editor font and properties changes for source viewers.
 * <p>
 * It disposes itself automatically.</p>
 */
public class SourceViewerJFaceUpdater {
	
	
	private IPreferenceStore fPreferenceStore;
	private IPropertyChangeListener fFontChangeListener;
	
	
	/**
	 * Creates a source preview updater for the given viewer, configuration and preference store from configuration.
	 * 
	 * @param viewer the viewer
	 * @param configuration the configuration
	 */
	public SourceViewerJFaceUpdater(final SourceViewer viewer, final SourceEditorViewerConfiguration configuration) {
		this(viewer, configuration, configuration.getPreferences());
	}
	
	/**
	 * Creates a source preview updater for the given viewer, configuration and preference store.
	 * 
	 * @param viewer the viewer
	 * @param configuration the configuration
	 * @param preferenceStore the preference store
	 */
	public SourceViewerJFaceUpdater(final SourceViewer viewer, final SourceEditorViewerConfiguration configuration, final IPreferenceStore preferenceStore) {
		this(viewer, configuration, preferenceStore, JFaceResources.TEXT_FONT);
	}
	
	public SourceViewerJFaceUpdater(final SourceViewer viewer, final SourceEditorViewerConfiguration configuration,
			final String symbolicFontName) {
		this(viewer, configuration, configuration.getPreferences(), symbolicFontName);
	}
	
	public SourceViewerJFaceUpdater(final SourceViewer viewer, final SourceEditorViewerConfiguration configuration, final IPreferenceStore preferenceStore,
			final String symbolicFontName) {
		assert (viewer != null);
		assert (configuration != null);
		assert (preferenceStore != null);
		
		fPreferenceStore = preferenceStore;
		
		fFontChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				if (event.getProperty().equals(symbolicFontName)) {
					final Font font = JFaceResources.getFont(symbolicFontName);
					if (font != null) {
						updateFont(viewer, font);
					}
				}
			}
		};
		viewer.getTextWidget().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				unregister();
			}
		});
		
		JFaceResources.getFontRegistry().addListener(fFontChangeListener);
	}
	
	
	protected void updateFont(final SourceViewer viewer, final Font font) {
		viewer.getTextWidget().setFont(font);
	}
	
	
	public synchronized void unregister() {
		if (fPreferenceStore != null) {
			JFaceResources.getFontRegistry().removeListener(fFontChangeListener);
		}
		fPreferenceStore = null;
		fFontChangeListener = null;
	}
	
}
