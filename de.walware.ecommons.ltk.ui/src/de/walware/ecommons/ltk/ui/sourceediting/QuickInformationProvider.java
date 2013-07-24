/*******************************************************************************
 * Copyright (c) 2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;

import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.LTKUtil;
import de.walware.ecommons.ltk.ui.LTKUI;


public abstract class QuickInformationProvider implements IInformationProvider,
		IInformationProviderExtension, IInformationProviderExtension2 {
	
	// see IJavaEditorActionDefinitionIds
	
	public static String viewerOperation2commandId(final int operation) {
		switch (operation) {
		case SourceEditorViewer.SHOW_SOURCE_OUTLINE:
			return LTKUI.SHOW_QUICK_SOURCE_OUTLINE_COMMAND_ID;
		case SourceEditorViewer.SHOW_ELEMENT_OUTLINE:
			return LTKUI.SHOW_QUICK_ELEMENT_OUTLINE_COMMAND_ID;
		case SourceEditorViewer.SHOW_ELEMENT_HIERARCHY:
			return LTKUI.SHOW_QUICK_ELEMENT_HIERARCHY_COMMAND_ID;
		default:
			throw new UnsupportedOperationException(Integer.toString(operation));
		}
	}
	
	
	private final ISourceEditor editor;
	
	private final String modelType;
	
	private final String commandId;
	
	private IInformationControlCreator creator;
	
	
	public QuickInformationProvider(final ISourceEditor editor, final String modelType,
			final int viewerOperation) {
		this(editor, modelType, viewerOperation2commandId(viewerOperation));
	}
	
	public QuickInformationProvider(final ISourceEditor editor, final String modelType,
			final String commandId) {
		this.editor = editor;
		this.modelType = modelType;
		this.commandId = commandId;
	}
	
	
	public ISourceEditor getEditor() {
		return this.editor;
	}
	
	public final String getCommandId() {
		return this.commandId;
	}
	
	public final String getModelType() {
		return this.modelType;
	}
	
	
	@Override
	public String getInformation(final ITextViewer textViewer, final IRegion subject) {
		return null;
	}
	
	@Override
	public Object getInformation2(final ITextViewer textViewer, final IRegion subject) {
		final ISourceUnit su = this.editor.getSourceUnit();
		if (su == null) {
			return null;
		}
		final ISourceUnitModelInfo modelInfo = su.getModelInfo(this.modelType, IModelManager.MODEL_FILE, null);
		if (modelInfo == null) {
			return null;
		}
		return LTKUtil.getCoveringSourceElement(modelInfo.getSourceElement(), subject);
	}
	
	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (this.creator == null) {
			this.creator = createInformationPresenterControlCreator();
		}
		return this.creator;
	}
	
	protected abstract IInformationControlCreator createInformationPresenterControlCreator();
	
}
