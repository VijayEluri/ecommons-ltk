/*=============================================================================#
 # Copyright (c) 2007-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk;

import de.walware.ecommons.ltk.ast.IAstNode;
import de.walware.ecommons.ltk.core.ISourceModelStamp;


/**
 * Container for AST.
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
	
	
	private final ISourceModelStamp stamp;
	
	private final int level;
	
	public final IAstNode root;
	
	
	public AstInfo(final int level, final ISourceModelStamp stamp, final IAstNode root) {
		this.level= level;
		this.stamp= stamp;
		this.root= root;
	}
	
	public AstInfo(final int level, final AstInfo ast) {
		this.level= level;
		this.stamp= ast.stamp;
		this.root= ast.root;
	}
	
	
	/**
	 * Returns the stamp of the AST.
	 * 
	 * @return the stamp
	 */
	public ISourceModelStamp getStamp() {
		return this.stamp;
	}
	
	/**
	 * Returns the level of detail of the AST.
	 * 
	 * @return
	 */
	public final int getLevel() {
		return this.level;
	}
	
}
