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

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.services.IServiceLocator;

import de.walware.ecommons.text.core.sections.IDocContentSections;

import de.walware.ecommons.ltk.core.model.ISourceUnit;


/**
 * A interface for source editors independent of IEditorPart
 */
public interface ISourceEditor extends IAdaptable {
	
	
	/**
	 * Returns the content type the editor is intended for.
	 * 
	 * @return the content type or <code>null</code>
	 */
	IContentType getContentType();
	
	/**
	 * Returns the source unit of editor input, if exists.
	 * 
	 * @return model element or <code>null</code>
	 */
	ISourceUnit getSourceUnit();
	
	/**
	 * Returns the part the editor belongs to
	 * 
	 * @return the part or <code>null</code>, if not in part
	 */
	IWorkbenchPart getWorkbenchPart();
	
	/**
	 * Returns the service locator for the editor
	 * 
	 * @return service locator responsible for editor
	 */
	IServiceLocator getServiceLocator();
	
	/**
	 * Allows access to the SourceViewer
	 * 
	 * @return the source viewer of the editor.
	 */
	SourceViewer getViewer();
	
	/**
	 * Returns the information about partitioning and content sections types of the document for 
	 * the content type the editor is configured for.
	 * 
	 * @return the document content information
	 */
	IDocContentSections getDocumentContentInfo();
	
	/**
	 * Returns whether the text in this text editor (SourceViewer) can be changed by the user
	 * 
	 * @param validate causes final validation if editor input is editable
	 * @return <code>true</code> if it can be edited, and <code>false</code> if it is read-only
	 */
	boolean isEditable(boolean validate);
	
	/**
	 * Selects and reveals the specified range in this text editor
	 *
	 * @param offset the offset of the selection
	 * @param length the length of the selection
	 */
	void selectAndReveal(int offset, int length);
	
	ITextEditToolSynchronizer getTextEditToolSynchronizer();
	
}
