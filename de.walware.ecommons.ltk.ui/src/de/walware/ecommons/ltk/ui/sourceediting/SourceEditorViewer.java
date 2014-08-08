/*=============================================================================#
 # Copyright (c) 2013-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;


public class SourceEditorViewer extends ProjectionViewer {
	
	
	/**
	 * Text operation code for requesting the outline for the current input.
	 */
	public static final int SHOW_SOURCE_OUTLINE = 51;
	
	/**
	 * Text operation code for requesting the outline for the element at the current position.
	 */
	public static final int SHOW_ELEMENT_OUTLINE = 52;
	
	/**
	 * Text operation code for requesting the hierarchy for the current input.
	 */
	public static final int SHOW_ELEMENT_HIERARCHY = 53;
	
	
	private static final int QUICK_PRESENTER_START = SHOW_SOURCE_OUTLINE;
	private static final int QUICK_PRESENTER_END = SHOW_ELEMENT_HIERARCHY;
	
	
	private final Point lastSentSelection= new Point(-1, -1);
	
	private IInformationPresenter sourceOutlinePresenter;
	private IInformationPresenter elementOutlinePresenter;
	private IInformationPresenter elementHierarchyPresenter;
	
	
	public SourceEditorViewer(final Composite parent, final IVerticalRuler ruler,
			final IOverviewRuler overviewRuler, final boolean showsAnnotationOverview, final int styles) {
		super(parent, ruler, overviewRuler, showsAnnotationOverview, styles);
		
		addPostSelectionChangedListener(new ISelectionChangedListener() {
			/** 
			 * By default source viewers do not caret changes to selection change listeners, only 
			 * to post selection change listeners.  This sents these post selection changes after
			 * validation to the selection change listeners too.
			 */
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				final ITextSelection selection= (ITextSelection) event.getSelection();
				if (lastSentSelection.x != selection.getOffset() || lastSentSelection.y != selection.getLength()) {
					final Point currentSelection= getSelectedRange();
					if (currentSelection.x == selection.getOffset() && currentSelection.y == selection.getLength()) {
						fireSelectionChanged(currentSelection.x, currentSelection.y);
					}
				}
			}
		});
	}
	
	
	@Override
	protected void fireSelectionChanged(SelectionChangedEvent event) {
		final ITextSelection selection= (ITextSelection) event.getSelection();
		lastSentSelection.x= selection.getOffset();
		lastSentSelection.y= selection.getLength();
		
		super.fireSelectionChanged(event);
	}
	
	
	private IInformationPresenter getPresenter(final int operation) {
		switch (operation) {
		case SHOW_SOURCE_OUTLINE:
			return this.sourceOutlinePresenter;
		case SHOW_ELEMENT_OUTLINE:
			return this.elementOutlinePresenter;
		case SHOW_ELEMENT_HIERARCHY:
			return this.elementHierarchyPresenter;
		default:
			return null;
		}
	}
	
	private void setPresenter(final int operation, final IInformationPresenter presenter) {
		switch (operation) {
		case SHOW_SOURCE_OUTLINE:
			this.sourceOutlinePresenter = presenter;
			return;
		case SHOW_ELEMENT_OUTLINE:
			this.elementOutlinePresenter = presenter;
			return;
		case SHOW_ELEMENT_HIERARCHY:
			this.elementHierarchyPresenter = presenter;
			return;
		default:
			if (presenter != null) {
				presenter.uninstall();
			}
			return;
		}
	}
	
	@Override
	public boolean canDoOperation(final int operation) {
		switch (operation) {
		case SHOW_SOURCE_OUTLINE:
		case SHOW_ELEMENT_OUTLINE:
		case SHOW_ELEMENT_HIERARCHY:
			return (getPresenter(operation) != null);
		default:
			return super.canDoOperation(operation);
		}
	}
	
	@Override
	public void doOperation(final int operation) {
		switch (operation) {
		case SHOW_SOURCE_OUTLINE:
		case SHOW_ELEMENT_OUTLINE:
		case SHOW_ELEMENT_HIERARCHY: {
			final IInformationPresenter presenter = getPresenter(operation);
			if (presenter != null) {
				presenter.showInformation();
			}
			return; }
		default:
			super.doOperation(operation);
			return;
		}
	}
	
	@Override
	public void configure(final SourceViewerConfiguration configuration) {
		super.configure(configuration);
		
		if (configuration instanceof SourceEditorViewerConfiguration) {
			for (int operation = QUICK_PRESENTER_START; operation < QUICK_PRESENTER_END; operation++) {
				final IInformationPresenter presenter = ((SourceEditorViewerConfiguration) configuration).getQuickPresenter(this, operation);
				if (presenter != null) {
					presenter.install(this);
				}
				setPresenter(operation, presenter);
			}
		}
	}
	
	@Override
	public void unconfigure() {
		for (int operation = QUICK_PRESENTER_START; operation < QUICK_PRESENTER_END; operation++) {
			final IInformationPresenter presenter = getPresenter(operation);
			if (presenter != null) {
				presenter.uninstall();
				setPresenter(operation, null);
			}
		}
		
		super.unconfigure();
	}
	
}
