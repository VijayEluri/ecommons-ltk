/*******************************************************************************
 * Copyright (c) 2000-2010 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.core.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.UndoEdit;

import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.internal.core.LTKCorePlugin;


/**
 * A {@link TextChange} for LTK {@link ISourceUnit}s.
 */
public final class SourceUnitChange extends TextFileChange {
	
	
	private final ISourceUnit fSourceUnit;
	
	/** The (optional) refactoring descriptor */
//	private ChangeDescriptor fDescriptor;
	
	
	/**
	 * Creates a new <code>CompilationUnitChange</code>.
	 * 
	 * @param name the change's name mainly used to render the change in the UI
	 * @param su the compilation unit this text change works on
	 */
	public SourceUnitChange(final ISourceUnit su) {
		super(su.getElementName().getDisplayName(), (IFile) su.getResource());
		assert (su != null);
		fSourceUnit = su;
		try {
			setTextType(getFile().getContentDescription().getContentType()
					.getFileSpecs(IContentType.FILE_EXTENSION_SPEC | IContentType.IGNORE_USER_DEFINED)[0]);
		} catch (final Exception e) {}
//		setTextType(ECommonsLTK.getExtContentTypeManager().getContentTypeForModelType(su.getModelTypeId()));
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getModifiedElement(){
		return fSourceUnit;
	}
	
	/**
	 * Returns the source unit this change works on.
	 * 
	 * @return the source unit this change works on
	 */
	public ISourceUnit getSourceUnit() {
		return fSourceUnit;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected IDocument acquireDocument(final IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, 3);
		fSourceUnit.connect(progress.newChild(1));
		return super.acquireDocument(progress.newChild(2));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void releaseDocument(final IDocument document, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, 3);
		super.releaseDocument(document, progress.newChild(2));
		fSourceUnit.disconnect(progress.newChild(1));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Change createUndoChange(final UndoEdit edit, final ContentStamp stampToRestore) {
		try {
			return new UndoSourceUnitChange(getName(), fSourceUnit, (IFile) super.getModifiedElement(), edit, stampToRestore, getSaveMode());
		}
		catch (final CoreException e) {
			LTKCorePlugin.getDefault().getLog().log(new Status(IStatus.ERROR, LTKCorePlugin.PLUGIN_ID, -1,
					"Failed to create Refactoring Undo", e)); //$NON-NLS-1$
			return null;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getAdapter(final Class required) {
		if (ISourceUnit.class.equals(required) || IModelElement.class.equals(required)) {
			return fSourceUnit;
		}
		return super.getAdapter(required);
	}
	
//	/**
//	 * Sets the refactoring descriptor for this change
//	 * 
//	 * @param descriptor the descriptor to set
//	 */
//	public void setDescriptor(final ChangeDescriptor descriptor) {
//		fDescriptor= descriptor;
//	}
//	
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public ChangeDescriptor getDescriptor() {
//		return fDescriptor;
//	}

}
