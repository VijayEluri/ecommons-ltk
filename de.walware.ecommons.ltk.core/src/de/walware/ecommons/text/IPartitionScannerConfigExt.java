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

package de.walware.ecommons.text;

import org.eclipse.jface.text.rules.IPartitionTokenScanner;


/**
 * Optional interface for partition scanner providing additional configuration
 * options.
 */
public interface IPartitionScannerConfigExt extends IPartitionTokenScanner {
	
	
	public void setStartPartitionType(final String partitionType);
	
}
