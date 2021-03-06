/*=============================================================================#
 # Copyright (c) 2010-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TypedRegion;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.text.core.util.TextUtils;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.sourceediting.assist.AssistInvocationContext;
import de.walware.ecommons.ltk.ui.sourceediting.assist.CombinedHover;
import de.walware.ecommons.ltk.ui.sourceediting.assist.IInfoHover;
import de.walware.ecommons.ltk.ui.sourceediting.assist.InfoHoverDescriptor;
import de.walware.ecommons.ltk.ui.sourceediting.assist.InfoHoverRegistry.EffectiveHovers;


/**
 * Wraps an LTK {@link IInfoHover} to an editor text hover.
 */
public abstract class EditorTextInfoHoverProxy implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {
	
	
	private final InfoHoverDescriptor descriptor;
	
	private IInfoHover hover;
	
	private final SourceEditorViewerConfiguration sourceEditorConfig;
	
	
	public EditorTextInfoHoverProxy(final InfoHoverDescriptor descriptor,
			final SourceEditorViewerConfiguration config) {
		this.descriptor= descriptor;
		this.sourceEditorConfig= config;
	}
	
	
	protected ISourceEditor getEditor() {
		return this.sourceEditorConfig.getSourceEditor();
	}
	
	protected boolean ensureHover() {
		if (this.hover == null) {
			this.hover= this.descriptor.createHover();
			if (this.hover instanceof CombinedHover) {
				final EffectiveHovers effectiveHovers= this.sourceEditorConfig.getConfiguredInfoHovers();
				if (effectiveHovers != null) {
					((CombinedHover) this.hover).setHovers(effectiveHovers.getDescriptorsForCombined());
				}
				else {
					this.hover= null;
				}
			}
		}
		return (this.hover != null);
	}
	
	@Override
	public IRegion getHoverRegion(final ITextViewer textViewer, final int offset) {
		return null;
	}
	
	@Override
	public String getHoverInfo(final ITextViewer textViewer, final IRegion hoverRegion) {
		return null;
	}
	
	@Override
	public Object getHoverInfo2(final ITextViewer textViewer, final IRegion hoverRegion) {
		final ISourceEditor editor= getEditor();
		if (editor != null && ensureHover()) {
			try {
				final String contentType= (hoverRegion instanceof TypedRegion) ?
						((TypedRegion) hoverRegion).getType() :
						TextUtils.getContentType(editor.getViewer().getDocument(),
								editor.getDocumentContentInfo(), hoverRegion.getOffset(),
								hoverRegion.getLength() == 0 );
				
				final AssistInvocationContext context= createContext(hoverRegion, contentType,
						new NullProgressMonitor() );
				if (context != null) {
					return this.hover.getHoverInfo(context);
				}
			}
			catch (final Exception e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, LTKUIPlugin.PLUGIN_ID,
						NLS.bind("An error occurred when preparing the information hover ''{0}'' (mouse).",
								this.descriptor.getName() ), e ));
			}
		}
		return null;
	}
	
	protected abstract AssistInvocationContext createContext(IRegion region, String contentType,
			IProgressMonitor monitor );
	
	@Override
	public IInformationControlCreator getHoverControlCreator() {
		if (ensureHover()) {
			return this.hover.getHoverControlCreator();
		}
		return null;
	}
	
}
