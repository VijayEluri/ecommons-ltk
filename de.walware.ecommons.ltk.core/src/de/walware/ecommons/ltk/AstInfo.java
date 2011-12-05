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

package de.walware.ecommons.ltk;

import de.walware.ecommons.ltk.ast.IAstNode;


/**
 * 
 */
public class AstInfo {
	
	
	public static final int DEFAULT_LEVEL_MASK =            0xf;
	
	/**
	 * AST without any text informations.
	 */
	public static final int LEVEL_MINIMAL =                 0x1;
	
	/**
	 * AST ready for model processing.
	 */
	public static final int LEVEL_MODEL_DEFAULT =           0x4;
	
	
	public final int level;
	public final long stamp;
	public final IAstNode root;
	
	
	public AstInfo(final int level, final long stamp, final IAstNode root) {
		this.level = level;
		this.stamp = stamp;
		this.root = root;
	}
	
	public AstInfo(final int level, final AstInfo ast) {
		this.level = level;
		this.stamp = ast.stamp;
		this.root = ast.root;
	}
	
	
}
