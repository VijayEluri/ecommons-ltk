/*******************************************************************************
 * Copyright (c) 2005-2013 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.templates;

import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import de.walware.ecommons.text.TextUtil;

import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;


public class SourceEditorTemplateContext extends DocumentTemplateContext implements IWorkbenchTemplateContext {
	
	
	private final ISourceEditor fEditor;
	
	
	public SourceEditorTemplateContext(final TemplateContextType type, final IDocument document, final int offset, final int length,
			final ISourceEditor editor) {
		super(type, document, offset, length);
		fEditor = editor;
	}
	
	
	@Override
	public ISourceEditor getEditor() {
		return fEditor;
	}
	
	@Override
	public ISourceUnit getSourceUnit() {
		return fEditor.getSourceUnit();
	}
	
	@Override
	public String evaluateInfo(final Template template) throws BadLocationException, TemplateException {
		final TemplateBuffer buffer = super.evaluate(template);
		if (buffer != null) {
			return buffer.getString();
		}
		return null;
	}
	
	@Override
	public TemplateBuffer evaluate(final Template template) throws BadLocationException, TemplateException {
		if (!canEvaluate(template)) {
			return null;
		}
		
		final String ln = TextUtilities.getDefaultLineDelimiter(getDocument());
		final TemplateTranslator translator = new TemplateTranslator();
		String pattern = template.getPattern();
		// correct line delimiter
		final Matcher matcher = TextUtil.LINE_DELIMITER_PATTERN.matcher(pattern);
		if (matcher.find()) {
			pattern = matcher.replaceAll(ln);
		}
		
		// default, see super
		final TemplateBuffer buffer = translator.translate(pattern);
		getContextType().resolve(buffer, this);
		
		indent(buffer);
		final String selection = getVariable("selection"); //$NON-NLS-1$
		if (selection != null && TextUtilities.indexOf(getDocument().getLegalLineDelimiters(), selection, 0)[0] != -1) {
			buffer.setContent(buffer.getString()+ln, buffer.getVariables());
		}
		
		return buffer;
	}
	
	private void indent(final TemplateBuffer buffer) throws BadLocationException {
		final TemplateVariable[] variables = buffer.getVariables();
		final List<TextEdit> positions = TemplatesUtil.variablesToPositions(variables);
		final IDocument baseDoc = getDocument();
		
		final IDocument templateDoc = new Document(buffer.getString());
		final MultiTextEdit root = new MultiTextEdit(0, templateDoc.getLength());
		root.addChildren(positions.toArray(new TextEdit[positions.size()]));
		
		String indentation = getVariable("indentation"); //$NON-NLS-1$
		
		// first line
		int offset = templateDoc.getLineOffset(0);
		if (indentation != null) {
			final TextEdit edit = new InsertEdit(offset, indentation);
			root.addChild(edit);
			root.apply(templateDoc, TextEdit.UPDATE_REGIONS);
			root.removeChild(edit);
		}
		else {
			indentation = TemplatesUtil.searchIndentation(baseDoc, getStart());
		}
		
		// following lines
		for (int line = 1; line < templateDoc.getNumberOfLines(); line++) {
			final IRegion region = templateDoc.getLineInformation(line);
			offset = region.getOffset();
			
			final TextEdit edit = new InsertEdit(offset, indentation);
			root.addChild(edit);
			root.apply(templateDoc, TextEdit.UPDATE_REGIONS);
			root.removeChild(edit);
		}
		
		TemplatesUtil.positionsToVariables(positions, variables);
		buffer.setContent(templateDoc.get(), variables);
	}
	
}
