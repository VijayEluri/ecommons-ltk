/*=============================================================================#
 # Copyright (c) 2005-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.FastList;
import de.walware.ecommons.templates.TemplateVariableProcessor;
import de.walware.ecommons.templates.WordFinder;
import de.walware.ecommons.text.ICharPairMatcher;
import de.walware.ecommons.text.IIndentSettings;
import de.walware.ecommons.text.IndentUtil;
import de.walware.ecommons.text.core.sections.IDocContentSections;
import de.walware.ecommons.text.ui.DefaultBrowserInformationInput;
import de.walware.ecommons.text.ui.presentation.ITextPresentationConstants;
import de.walware.ecommons.text.ui.settings.AssistPreferences;
import de.walware.ecommons.text.ui.settings.DecorationPreferences;
import de.walware.ecommons.text.ui.settings.TextStyleManager;
import de.walware.ecommons.ui.ISettingsChangedHandler;
import de.walware.ecommons.ui.util.DialogUtil;

import de.walware.ecommons.ltk.internal.ui.LTKUIPlugin;
import de.walware.ecommons.ltk.ui.LTKUIPreferences;
import de.walware.ecommons.ltk.ui.sourceediting.assist.ContentAssist;
import de.walware.ecommons.ltk.ui.sourceediting.assist.InfoHoverDescriptor;
import de.walware.ecommons.ltk.ui.sourceediting.assist.InfoHoverRegistry;
import de.walware.ecommons.ltk.ui.sourceediting.assist.InfoHoverRegistry.EffectiveHovers;


/**
 * Abstract configuration for {@link ISourceEditor}s.
 */
public abstract class SourceEditorViewerConfiguration extends TextSourceViewerConfiguration
		implements ISettingsChangedHandler {
	
	
	static final boolean DEBUG= true;
	
	private static IInformationControlCreator ASSIST_INFO_CREATOR;
	
	
	private static class AssistInformationControlCreator extends AbstractReusableInformationControlCreator {
		
		@Override
		protected IInformationControl doCreateInformationControl(final Shell parent) {
			if (BrowserInformationControl.isAvailable(parent)) {
				return new BrowserInformationControl(parent, JFaceResources.DIALOG_FONT, false) {
					
					@Override
					public void setInformation(String content) {
						if (content.startsWith("...<br")) { // spell correction change proposal //$NON-NLS-1$
							content= content.replace("\\t", "    "); //$NON-NLS-1$ //$NON-NLS-2$
							final StringBuffer s= new StringBuffer(content.length()+1000);
							s.append("<pre>"); //$NON-NLS-1$
							s.append(content);
							s.append("</pre>"); //$NON-NLS-1$
							setInput(new DefaultBrowserInformationInput(null, "", s.toString(),  //$NON-NLS-1$
									DefaultBrowserInformationInput.FORMAT_HTMLBODY_INPUT));
						}
						else {
							setInput(new DefaultBrowserInformationInput(null, "", content, //$NON-NLS-1$
									DefaultBrowserInformationInput.FORMAT_TEXT_INPUT));
						}
					}
				};
			}
			else {
				return new DefaultInformationControl(parent, new HTMLTextPresenter(false));
			}
		}
		
	};
	
	private static final IInformationControlCreator DEFAULT_INFORMATION_CONTROL_CREATOR=
			new IInformationControlCreator() {
				
				@Override
				public IInformationControl createInformationControl(final Shell parent) {
					return new DefaultInformationControl(parent, true);
				}
				
			};
	
	private static final ITextHover NO_HOVER= new ITextHover() {
		@Override
		public IRegion getHoverRegion(final ITextViewer textViewer, final int offset) {
			return null;
		}
		@Override
		public String getHoverInfo(final ITextViewer textViewer, final IRegion hoverRegion) {
			return null;
		}
	};
	
	
	private final IDocContentSections documentContentInfo;
	
	private final ISourceEditor editor;
	
	private TextStyleManager textStyles;
	private final FastList<ISettingsChangedHandler> settingsHandler= new FastList<>(ISettingsChangedHandler.class);
	
	private Map<String, ITokenScanner> scanners;
	
	private IPresentationReconciler presentationReconciler;
	private ICharPairMatcher pairMatcher;
	private ContentAssist contentAssistant;
	private IQuickAssistAssistant quickAssistant;
	
	private DecorationPreferences decorationPreferences;
	private AssistPreferences assistPreferences;
	
	private EffectiveHovers infoHovers;
	private Map<Integer, ITextHover> infoHoverCache;
	
	
	public SourceEditorViewerConfiguration(final IDocContentSections documentContentInfo,
			final ISourceEditor sourceEditor) {
		if (documentContentInfo == null) {
			throw new NullPointerException("documentContentInfo"); //$NON-NLS-1$
		}
		this.documentContentInfo= documentContentInfo;
		this.editor= sourceEditor;
	}
	
	
	protected void setup(final IPreferenceStore preferenceStore,
			final DecorationPreferences decoPrefs, final AssistPreferences assistPrefs) {
		assert (preferenceStore != null);
		this.fPreferenceStore= preferenceStore;
		this.decorationPreferences= decoPrefs;
		this.assistPreferences= assistPrefs;
	}
	
	
	protected void initTextStyles() {
	}
	
	protected void setTextStyles(final TextStyleManager textStyles) {
		this.textStyles= textStyles;
	}
	
	protected TextStyleManager getTextStyles() {
		if (this.textStyles == null) {
			initTextStyles();
		}
		return this.textStyles;
	}
	
	
	protected void initScanners() {
	}
	
	protected void addScanner(final String contentType, final ITokenScanner scanner) {
		this.scanners.put(contentType, scanner);
		
		if (scanner instanceof ISettingsChangedHandler) {
			this.settingsHandler.add((ISettingsChangedHandler) scanner);
		}
	}
	
	protected ITokenScanner getScanner(final String contentType) {
		if (this.scanners == null) {
			this.scanners= new HashMap<>();
			initScanners();
		}
		final ITokenScanner scanner= this.scanners.get(contentType);
		if (DEBUG && scanner == null) {
			StatusManager.getManager().handle(new Status(IStatus.WARNING, LTKUIPlugin.PLUGIN_ID,
					"No scanner for contentType= " + contentType + "." ));
		}
		return scanner;
	}
	
	
	protected ISourceEditor getSourceEditor() {
		return this.editor;
	}
	
	public final IDocContentSections getDocumentContentInfo() {
		return this.documentContentInfo;
	}
	
	@Override
	public final String getConfiguredDocumentPartitioning(final ISourceViewer sourceViewer) {
		return this.documentContentInfo.getPartitioning();
	}
	
	public IPreferenceStore getPreferences() {
		return this.fPreferenceStore;
	}
	
	
	public DecorationPreferences getDecorationPreferences() {
		return this.decorationPreferences;
	}
	
	public AssistPreferences getAssistPreferences() {
		return this.assistPreferences;
	}
	
	
	@Override
	public void handleSettingsChanged(final Set<String> groupIds, final Map<String, Object> options) {
		if (this.assistPreferences != null
				&& (groupIds.contains(LTKUIPreferences.ASSIST_GROUP_ID) || groupIds.contains(this.assistPreferences.getGroupId())) ) {
			if (this.contentAssistant != null) {
				this.assistPreferences.configure(this.contentAssistant);
			}
			if (this.quickAssistant != null) {
				this.assistPreferences.configure(this.quickAssistant);
			}
		}
		if (this.textStyles != null && this.textStyles.affectsTextPresentation(groupIds)) {
			options.put(ITextPresentationConstants.SETTINGSCHANGE_AFFECTSPRESENTATION_KEY, Boolean.TRUE);
		}
		for (final ISettingsChangedHandler handler : this.settingsHandler.toArray()) {
			handler.handleSettingsChanged(groupIds, options);
		}
	}
	
	@Override
	public IPresentationReconciler getPresentationReconciler(final ISourceViewer sourceViewer) {
		if (this.presentationReconciler == null) {
			this.presentationReconciler= createPresentationReconciler();
		}
		return this.presentationReconciler;
	}
	
	protected IPresentationReconciler createPresentationReconciler() {
		final PresentationReconciler reconciler= new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(null));
		
		initPresentationReconciler(reconciler);
		
		return reconciler;
	}
	
	PresentationReconciler getPresentationReconciler() {
		return (PresentationReconciler) this.presentationReconciler;
	}
	
	protected void initPresentationReconciler(final PresentationReconciler reconciler) {
		final String[] contentTypes= getConfiguredContentTypes(null);
		for (final String contentType : contentTypes) {
			final ITokenScanner scanner= getScanner(contentType);
			if (scanner != null) {
				final DefaultDamagerRepairer dr= new DefaultDamagerRepairer(scanner);
				reconciler.setDamager(dr, contentType);
				reconciler.setRepairer(dr, contentType);
			}
		}
	}
	
	
	public List<ISourceEditorAddon> getAddOns() {
		return new ArrayList<>();
	}
	
	
	public ICharPairMatcher getPairMatcher() {
		if (this.pairMatcher == null) {
			this.pairMatcher= createPairMatcher();
		}
		return this.pairMatcher;
	}
	
	protected ICharPairMatcher createPairMatcher() {
		return null;
	}
	
	
	protected IIndentSettings getIndentSettings() {
		return null;
	}
	
	@Override
	public int getTabWidth(final ISourceViewer sourceViewer) {
		final IIndentSettings settings= getIndentSettings();
		if (settings != null) {
			return settings.getTabSize();
		}
		return super.getTabWidth(sourceViewer);
	}
	
	@Override
	public String[] getIndentPrefixes(final ISourceViewer sourceViewer, final String contentType) {
		final IIndentSettings settings= getIndentSettings();
		if (settings != null) {
			final String[] prefixes= getIndentPrefixesForTab(getTabWidth(sourceViewer));
			if (settings.getIndentDefaultType() == IIndentSettings.IndentationType.SPACES) {
				for (int i= prefixes.length-2; i > 0; i--) {
					prefixes[i]= prefixes[i-1];
				}
				prefixes[0]= new String(IndentUtil.repeat(' ', settings.getIndentSpacesCount()));
			}
			return prefixes;
		}
		else {
			return super.getIndentPrefixes(sourceViewer, contentType);
		}
	}
	
	
	public boolean isSmartInsertSupported() {
		return false;
	}
	
	public boolean isSmartInsertByDefault() {
		return true;
	}
	
	
	@Override
	public IContentAssistant getContentAssistant(final ISourceViewer sourceViewer) {
		if (this.contentAssistant == null) {
			this.contentAssistant= createContentAssistant(sourceViewer);
			if (this.contentAssistant != null) {
				if (this.assistPreferences != null) {
					this.assistPreferences.configure(this.contentAssistant);
				}
				this.contentAssistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
				this.contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
				this.contentAssistant.setInformationControlCreator(getAssistInformationControlCreator(sourceViewer));
			}
		}
		return this.contentAssistant;
	}
	
	public ContentAssist getContentAssist() {
		return this.contentAssistant;
	}
	
	protected ContentAssist createContentAssistant(final ISourceViewer sourceViewer) {
		if (getSourceEditor() != null) {
			final ContentAssist assistant = new ContentAssist();
			assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(null));
			assistant.setRestoreCompletionProposalSize(DialogUtil.getDialogSettings(LTKUIPlugin.getInstance(), "ContentAssist.Proposal.size")); //$NON-NLS-1$
			
			initContentAssist(assistant);
			return assistant;
		}
		return null;
	}
	
	protected void initContentAssist(final ContentAssist assistant) {
	}
	
	
	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(final ISourceViewer sourceViewer) {
		if (this.quickAssistant == null) {
			this.quickAssistant= createQuickAssistant(sourceViewer);
			if (this.quickAssistant != null) {
				if (this.assistPreferences != null) {
					this.assistPreferences.configure(this.quickAssistant);
				}
				this.quickAssistant.setInformationControlCreator(getAssistInformationControlCreator(sourceViewer));
			}
		}
		return this.quickAssistant;
	}
	
	protected IQuickAssistAssistant createQuickAssistant(final ISourceViewer sourceViewer) {
		final IQuickAssistProcessor processor= createQuickAssistProcessor();
		if (processor != null) {
			final QuickAssistAssistant assistent= new QuickAssistAssistant();
			assistent.setQuickAssistProcessor(processor);
			assistent.enableColoredLabels(true);
			return assistent;
		}
		return super.getQuickAssistAssistant(sourceViewer);
	}
	
	protected IQuickAssistProcessor createQuickAssistProcessor() {
		return null;
	}
	
	protected IInformationControlCreator getAssistInformationControlCreator(final ISourceViewer sourceViewer) {
		if (ASSIST_INFO_CREATOR == null) {
			ASSIST_INFO_CREATOR= new AssistInformationControlCreator();
		}
		return ASSIST_INFO_CREATOR;
	}
	
	@Override
	public int[] getConfiguredTextHoverStateMasks(final ISourceViewer sourceViewer, final String contentType) {
		if (this.editor != null) {
			final String[] partitioning= getConfiguredContentTypes(null);
			if (partitioning != null && partitioning.length > 0 && partitioning[0].equals(contentType)) {
				final InfoHoverRegistry registry= getInfoHoverRegistry();
				this.infoHovers= (registry != null) ? registry.getEffectiveHoverDescriptors() : null;
				if (this.infoHoverCache == null) {
					this.infoHoverCache= new HashMap<>();
				}
				else {
					this.infoHoverCache.clear();
				}
			}
			if (this.infoHovers != null && isInfoHoverDefaultContentType(contentType)) {
				return this.infoHovers.getStateMasks();
			}
		}
		return super.getConfiguredTextHoverStateMasks(sourceViewer, contentType);
	}
	
	protected boolean isInfoHoverDefaultContentType(final String contentType) {
		final String[] partitioning= getConfiguredContentTypes(null);
		return (partitioning != null && partitioning.length > 0 && partitioning[0].equals(contentType));
	}
	
	public EffectiveHovers getConfiguredInfoHovers() {
		return this.infoHovers;
	}
	
	@Override
	public ITextHover getTextHover(final ISourceViewer sourceViewer, final String contentType) {
		return getTextHover(sourceViewer, contentType, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
	}
	
	@Override
	public ITextHover getTextHover(final ISourceViewer sourceViewer, final String contentType, final int stateMask) {
		if (this.infoHovers != null && isInfoHoverDefaultContentType(contentType)) {
			final Integer key= Integer.valueOf(stateMask);
			ITextHover hover= this.infoHoverCache.get(key);
			if (hover == null) {
				final InfoHoverDescriptor descriptor= this.infoHovers.getDescriptor(stateMask);
				if (descriptor != null) {
					hover= createInfoHover(descriptor);
					if (hover == null) {
						hover= NO_HOVER;
					}
					this.infoHoverCache.put(key, hover);
				}
			}
			return (hover != NO_HOVER) ? hover : null;
		}
		return null;
	}
	
	protected InfoHoverRegistry getInfoHoverRegistry() {
		return null;
	}
	
	protected ITextHover createInfoHover(final InfoHoverDescriptor descriptor) {
		return null;
	}
	
	
	@Override
	public IInformationPresenter getInformationPresenter(final ISourceViewer sourceViewer) {
		final InformationPresenter presenter= new InformationPresenter(
				DEFAULT_INFORMATION_CONTROL_CREATOR);
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(null));
		
		// Register information provider
		final IInformationProvider provider= getInformationProvider();
		final String[] contentTypes= getConfiguredContentTypes(null);
		for (int i= 0; i < contentTypes.length; i++) {
			presenter.setInformationProvider(provider, contentTypes[i]);
		}
		// sizes: see org.eclipse.jface.text.TextViewer.TEXT_HOVER_*_CHARS
		presenter.setSizeConstraints(100, 12, true, true);
		return presenter;
	}
	
	protected IInformationProvider getInformationProvider() {
		return null;
	}
	
	
	@Override
	protected Map getHyperlinkDetectorTargets(final ISourceViewer sourceViewer) {
		final Map<String, IAdaptable> targets= super.getHyperlinkDetectorTargets(sourceViewer);
		collectHyperlinkDetectorTargets(targets, sourceViewer);
		return targets;
	}
	
	protected void collectHyperlinkDetectorTargets(final Map<String, IAdaptable> targets, final ISourceViewer sourceViewer) {
	}
	
	public IInformationPresenter getQuickPresenter(final ISourceViewer sourceViewer,
			final int operation) {
		final IInformationProvider provider= getQuickInformationProvider(sourceViewer, operation);
		if (provider == null) {
			return null;
		}
		final InformationPresenter presenter= new InformationPresenter(((IInformationProviderExtension2) provider).getInformationPresenterControlCreator());
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(null));
		presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
		presenter.setSizeConstraints(50, 20, true, false);
		
		final String[] contentTypes= getConfiguredContentTypes(null);
		for (int i= 0; i < contentTypes.length; i++) {
			presenter.setInformationProvider(provider, contentTypes[i]);
		}
		
		return presenter;
	}
	
	protected IInformationProvider getQuickInformationProvider(final ISourceViewer sourceViewer,
			final int operation) {
		return null;
	}
	

	
/* For TemplateEditors ********************************************************/
	
	protected static class TemplateVariableTextHover implements ITextHover {
		
		private final TemplateVariableProcessor processor;
		
		/**
		 * @param processor the template variable processor
		 */
		public TemplateVariableTextHover(final TemplateVariableProcessor processor) {
			this.processor= processor;
		}
		
		@Override
		public String getHoverInfo(final ITextViewer textViewer, final IRegion subject) {
			try {
				final IDocument doc= textViewer.getDocument();
				final int offset= subject.getOffset();
				if (offset >= 2 && "${".equals(doc.get(offset-2, 2))) { //$NON-NLS-1$
					final String varName= doc.get(offset, subject.getLength());
					final TemplateContextType contextType= this.processor.getContextType();
					if (contextType != null) {
						final Iterator iter= contextType.resolvers();
						while (iter.hasNext()) {
							final TemplateVariableResolver var= (TemplateVariableResolver) iter.next();
							if (varName.equals(var.getType())) {
								return var.getDescription();
							}
						}
					}
				}
			} catch (final BadLocationException e) {
			}
			return null;
		}
		
		@Override
		public IRegion getHoverRegion(final ITextViewer textViewer, final int offset) {
			if (textViewer != null) {
				return WordFinder.findWord(textViewer.getDocument(), offset);
			}
			return null;
		}
		
	}
	
	protected ContentAssist createTemplateVariableContentAssistant(final ISourceViewer sourceViewer,
			final TemplateVariableProcessor processor) {
		final ContentAssist assistant= new ContentAssist();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(null));
		
		for (final String contentType : getConfiguredContentTypes(null)) {
			assistant.setContentAssistProcessor(processor, contentType);
		}
		
		return assistant;
	}
	
}
