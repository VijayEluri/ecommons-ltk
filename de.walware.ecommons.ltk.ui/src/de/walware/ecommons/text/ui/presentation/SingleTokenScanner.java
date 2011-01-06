/*******************************************************************************
 * Copyright (c) 2005-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.text.ui.presentation;

import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;

import de.walware.ecommons.ui.ColorManager;


/**
 * Scanner which handles complete partitions as single token.
 */
public class SingleTokenScanner extends AbstractRuleBasedScanner {
	
	
	private final String fDefaultTokenKey;
	
	
	public SingleTokenScanner (final ColorManager colorManager, final IPreferenceStore preferenceStore, 
			final String stylesGroupId, final String defaultTokenKey) {
		super(colorManager, preferenceStore, stylesGroupId);
		
		fDefaultTokenKey = defaultTokenKey;
		initialize();
	}
	
	
	@Override
	protected List<IRule> createRules() {
		final IToken defaultToken = getToken(fDefaultTokenKey);
		setDefaultReturnToken(defaultToken);
		return null;
	}
	
}
