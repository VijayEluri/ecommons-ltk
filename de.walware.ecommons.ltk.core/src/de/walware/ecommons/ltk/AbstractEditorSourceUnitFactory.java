/*******************************************************************************
 * Copyright (c) 2009-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk;

import org.eclipse.core.filesystem.IFileStore;


/**
 * Abstract factory for {@link LTK#EDITOR_CONTEXT}.
 */
public abstract class AbstractEditorSourceUnitFactory implements ISourceUnitFactory {
	
	
	public String createId(final Object from) {
		if (from instanceof IFileStore) {
			return AbstractFilePersistenceSourceUnitFactory.createResourceId(((IFileStore) from).toURI());
		}
		return null;
	}
	
	public ISourceUnit createSourceUnit(final String id, final Object from) {
		if (from instanceof ISourceUnit) {
			return createSourceUnit(id, (ISourceUnit) from);
		}
		if (from instanceof IFileStore) {
			return createSourceUnit(id, (IFileStore) from);
		}
		return null;
	}
	
	
	protected abstract ISourceUnit createSourceUnit(final String id, final ISourceUnit su);
	
	protected abstract ISourceUnit createSourceUnit(final String id, final IFileStore file);
	
}
