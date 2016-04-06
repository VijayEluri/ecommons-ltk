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

package de.walware.ecommons.ltk.internal.core.refactoring;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {
	
	
	public static String Common_error_CannotCreateFromDescr_message;
	public static String Common_error_AnalyzingSourceDocument_message;
	public static String Common_error_CreatingElementChange_message;
	
	public static String Check_FileUnsavedChanges_message;
	public static String Check_ElementNotInWS_message;
	
	public static String DynamicValidationState_WorkspaceChanged_message;
	
	public static String DeleteRefactoring_label;
	public static String DeleteRefactoring_description_singular;
	public static String DeleteRefactoring_description_plural;
	
	public static String MoveRefactoring_label;
	public static String MoveRefactoring_description_singular;
	public static String MoveRefactoring_description_plural;
	
	public static String CopyRefactoring_label;
	public static String CopyRefactoring_description_singular;
	public static String CopyRefactoring_description_plural;
	
	public static String PasteRefactoring_label;
	public static String PasteRefactoring_Code_description;
	
	
	static {
		initializeMessages(Messages.class.getName(), Messages.class);
	}
	private Messages() {}
	
}
