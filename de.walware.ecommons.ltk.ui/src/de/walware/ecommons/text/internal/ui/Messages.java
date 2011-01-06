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

package de.walware.ecommons.text.internal.ui;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {
	
	
	public static String Editors_link;
	public static String Editors_Appearance;
	public static String Editors_HighlightMatchingBrackets;
	public static String Editors_AppearanceColors;
	public static String Editors_Color;
	public static String Editors_MatchingBracketsHighlightColor;
	public static String Editors_CodeAssistProposalsForegroundColor;
	public static String Editors_CodeAssistProposalsBackgroundColor;
	public static String Editors_CodeAssistParametersForegrondColor;
	public static String Editors_CodeAssistParametersBackgroundColor;
	public static String Editors_CodeAssistReplacementForegroundColor;
	public static String Editors_CodeAssistReplacementBackgroundColor;
	public static String Editors_CodeAssist;
	public static String Editors_CodeAssist_AutoInsertSingle;
	public static String Editors_CodeAssist_AutoInsertCommon;
	public static String Editors_CodeAssist_AutoTriggerDelay_label;
	public static String Editors_CodeAssist_AutoTriggerDelay_error_message;
	
	public static String SyntaxColoring_link;
	public static String SyntaxColoring_List_label;
	public static String SyntaxColoring_MindExtraStyle_tooltip;
	public static String SyntaxColoring_Use_CustomStyle_label;
	public static String SyntaxColoring_Use_NoExtraStyle_label;
	public static String SyntaxColoring_Use_OtherStyle_label;
	public static String SyntaxColoring_Color;
	public static String SyntaxColoring_Bold;
	public static String SyntaxColoring_Italic;
	public static String SyntaxColoring_Underline;
	public static String SyntaxColoring_Strikethrough;
	public static String SyntaxColoring_Preview;
	
	public static String CodeTemplates_title;
	public static String CodeTemplates_label;
	public static String CodeTemplates_EditButton_label;
	public static String CodeTemplates_ImportButton_label;
	public static String CodeTemplates_ExportButton_label;
	public static String CodeTemplates_ExportAllButton_label;
	public static String CodeTemplates_Preview_label;
	
	public static String CodeTemplates_error_title;
	public static String CodeTemplates_error_Parse_message;
	public static String CodeTemplates_error_Read_message;
	public static String CodeTemplates_error_Write_message;
	
	public static String CodeTemplates_Import_title;
	public static String CodeTemplates_Import_extension;
	public static String CodeTemplates_Export_title;
	public static String CodeTemplates_Export_extension;
	public static String CodeTemplates_Export_filename;
	public static String CodeTemplates_Export_Error_title;
	public static String CodeTemplates_Export_Error_Hidden_message;
	public static String CodeTemplates_Export_Error_CanNotWrite_message;
	public static String CodeTemplates_Export_Exists_title;
	public static String CodeTemplates_Export_Exists_message;
	
	
	static {
		NLS.initializeMessages(Messages.class.getName(), Messages.class);
	}
	private Messages() {}
	
}
