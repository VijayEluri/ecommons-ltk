/*******************************************************************************
 * Copyright (c) 2009-2012 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.ui;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ui.SharedUIResources;
import de.walware.ecommons.ui.util.UIAccess;

import de.walware.ecommons.ltk.internal.ui.EditingMessages;


/**
 * Hyperlink opening an editor for a {@link IFileStore}.
 */
public class OpenFileHyperlink implements IHyperlink {
	
	
	private final IRegion fRegion;
	private final IFileStore fStore;
	
	
	public OpenFileHyperlink(final IRegion region, final IFileStore store) {
		fRegion = region;
		fStore = store;
	}
	
	
	@Override
	public String getTypeLabel() {
		return null;
	}
	
	@Override
	public IRegion getHyperlinkRegion() {
		return fRegion;
	}
	
	@Override
	public String getHyperlinkText() {
		return NLS.bind(EditingMessages.OpenFileHyperlink_label, fStore.toString());
	}
	
	@Override
	public void open() {
		try {
			IDE.openEditorOnFileStore(UIAccess.getActiveWorkbenchPage(true), fStore);
		}
		catch (final PartInitException e) {
			Display.getCurrent().beep();
			StatusManager.getManager().handle(new Status(IStatus.INFO, SharedUIResources.PLUGIN_ID, -1,
					NLS.bind("An error occurred when following the hyperlink and opening the file ''{0}''", fStore.toString()), e));
		}
	}
	
}
