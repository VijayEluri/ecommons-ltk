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

package de.walware.ecommons.ltk.internal.ui.refactoring;

import java.util.Iterator;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.viewers.IStructuredSelection;

import de.walware.ecommons.ltk.LTKUtil;
import de.walware.ecommons.ltk.core.model.IModelElement;
import de.walware.ecommons.ltk.core.model.ISourceUnit;


public class ModelElementTester extends PropertyTester {
	
	
	public static final String IS_ELEMENT_SELECTION = "isElementSelection"; //$NON-NLS-1$
	public static final String IS_ELEMENT_C1_TYPE_SELECTION = "isElementC1TypeSelection"; //$NON-NLS-1$
	public static final String IS_ELEMENT_C2_TYPE_SELECTION = "isElementC2TypeSelection"; //$NON-NLS-1$
	
	
	public ModelElementTester() {
	}
	
	
	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (!(receiver instanceof IStructuredSelection)) {
			return false;
		}
		final IStructuredSelection selection = (IStructuredSelection) receiver;
		int mask = 0;
		if (IS_ELEMENT_C1_TYPE_SELECTION.equals(property)) {
			mask = IModelElement.MASK_C1;
		}
		else if (IS_ELEMENT_C2_TYPE_SELECTION.equals(property)) {
			mask = IModelElement.MASK_C2;
		}
		int numSu = 1;
		String modelType = (String) expectedValue;
		if (modelType.length() > 0 && modelType.charAt(modelType.length()-1) == '*') {
			modelType = modelType.substring(0, modelType.length() - 1);
			numSu = -1;
		}
		
		if (selection.isEmpty()) {
			return false;
		}
		
		final int[] types = parseInts(args);
		final Iterator iter = selection.iterator();
		boolean first = true;
		ISourceUnit su = null;
		ITER_ELEMENTS : while (iter.hasNext()) {
			final Object obj = iter.next();
			if (obj instanceof IModelElement) {
				final IModelElement element = (IModelElement) obj;
				if (numSu == 1) {
					if (first) {
						first = false;
						if ((su = LTKUtil.getSourceUnit(element)) == null) {
							return false;
						}
					}
					else {
						if (su != LTKUtil.getSourceUnit(element)) {
							return false;
						}
					}
				}
				if (element.getModelTypeId().equals(modelType)) {
					if (mask == 0) {
						continue ITER_ELEMENTS;
					}
					for (int i = 0; i < types.length; i++) {
						if ((element.getElementType() & mask) == types[i]) {
							continue ITER_ELEMENTS;
						}
					}
				}
				return false;
			}
			else {
				return false;
			}
		}
		return true;
	}
	
	private int[] parseInts(final Object[] args) {
		final int[] ints = new int[args.length];
		for (int i = 0; i < args.length; i++) {
			ints[i] = ((Integer) args[i]).intValue();
		}
		return ints;
	}
	
}
