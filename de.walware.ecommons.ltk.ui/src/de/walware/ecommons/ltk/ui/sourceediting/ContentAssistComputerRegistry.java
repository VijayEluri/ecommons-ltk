/*******************************************************************************
 * Copyright (c) 2008-2010 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

import de.walware.ecommons.ICommonStatusConstants;
import de.walware.ecommons.IDisposable;
import de.walware.ecommons.preferences.IPreferenceAccess;
import de.walware.ecommons.preferences.Preference;
import de.walware.ecommons.preferences.PreferencesUtil;
import de.walware.ecommons.preferences.SettingsChangeNotifier;
import de.walware.ecommons.preferences.Preference.StringArrayPref;
import de.walware.ecommons.preferences.Preference.StringSetPref;
import de.walware.ecommons.ui.SharedUIResources;

import de.walware.ecommons.ltk.internal.ui.AdvancedContentAssistInternal;


/**
 * Registry of contributions for {@link ContentAssistProcessor}s for a single content type.
 */
public class ContentAssistComputerRegistry implements SettingsChangeNotifier.ChangeListener, IDisposable {
	
	
	public static String DEFAULT_DISABLED = "assist.default.disabled_categories.ids"; //$NON-NLS-1$
	public static String CIRCLING_ORDERED = "assist.circling.ordered_categories.ids:enabled"; //$NON-NLS-1$
	
	
	public static final Bundle getBundle(final IConfigurationElement element) {
		final String namespace = element.getDeclaringExtension().getContributor().getName();
		final Bundle bundle = Platform.getBundle(namespace);
		return bundle;
	}
	
	protected static final Map<String, ContentAssistCategory> createCategoryByIdMap(final List<ContentAssistCategory> categories) {
		final Map<String, ContentAssistCategory> map = new HashMap<String, ContentAssistCategory>();
		for (final ContentAssistCategory category : categories) {
			map.put(category.getId(), category);
		}
		return map;
	}
	
	
	/**
	 * The description of an {@link IContentAssistComputer}
	 */
	final class ComputerDescriptor {
		
		
		/** The identifier of the extension. */
		private final String fId;
		/** The class name of the provided <code>ICompletionProposalComputer</code>. */
		private final Set<String> fPartitions;
		/** The configuration element of this extension. */
		private final IConfigurationElement fConfigurationElement;
		/** The computer, if instantiated, <code>null</code> otherwise. */
		private IContentAssistComputer fComputer;
		/** Tells whether we tried to load the computer. */
		boolean fTriedLoadingComputer = false;
		
		
		/**
		 * Creates a new descriptor with lazy loaded computer
		 */
		ComputerDescriptor(final String id, final Set<String> partitions, final IConfigurationElement configurationElement) {
			fId = id;
			fPartitions = partitions;
			fConfigurationElement = configurationElement;
		}
		
		
		/**
		 * Returns the identifier of the described extension.
		 *
		 * @return Returns the id
		 */
		public String getId() {
			return fId;
		}
		
		/**
		 * Returns the partition types of the described extension.
		 * 
		 * @return the set of partition types (element type: {@link String})
		 */
		public Set<String> getPartitions() {
			return fPartitions;
		}
		
		/**
		 * Returns a cached instance of the computer
		 */
		public IContentAssistComputer getComputer() {
			if (fComputer == null && !fTriedLoadingComputer && fConfigurationElement != null) {
				fTriedLoadingComputer = true;
				try {
					fComputer = (IContentAssistComputer) fConfigurationElement.createExecutableExtension(AdvancedContentAssistInternal.CONFIG_CLASS_ATTRIBUTE_NAME);
				}
				catch (final CoreException e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, -1,
							NLS.bind("Loading ICompletionProposalComputer with id ''{0}'' failed (contributed by = '' {1}'').", fId, fConfigurationElement.getDeclaringExtension().getContributor().getName()), e)); 
				}
			}
			return fComputer;
		}
		
	}
	
	class CategoryPreferences {
		
		public final List<ContentAssistCategory> allCategories;
		public final List<ContentAssistCategory> defaultEnabledCategories;
		public final List<ContentAssistCategory> circlingOrderedCategories;
		public final List<ContentAssistCategory> circlingOrderedEnabledCategories;
		
		
		public CategoryPreferences(final List<ContentAssistCategory> categories) {
			allCategories = categories;
			defaultEnabledCategories = new ArrayList<ContentAssistCategory>(categories.size());
			circlingOrderedCategories = new ArrayList<ContentAssistCategory>(categories.size());
			circlingOrderedEnabledCategories = new ArrayList<ContentAssistCategory>(categories.size());
		}
		
	}
	
	
	private final String fContentTypeId;
	private final String fSettingsGroupId;
	private final StringSetPref fPrefDisabledCategoryIds;
	private final StringArrayPref fPrefOrderedCategoryIds;
	
	private List<ContentAssistCategory> fCategories;
	private String fSpecificModeId;
	
	
	public ContentAssistComputerRegistry(final String contentTypeId, final String prefQualifier, 
			final String settingsGroupId) {
		fContentTypeId = contentTypeId;
		fSettingsGroupId = settingsGroupId;
		fPrefDisabledCategoryIds = new StringSetPref(prefQualifier, DEFAULT_DISABLED);
		fPrefOrderedCategoryIds = new StringArrayPref(prefQualifier, CIRCLING_ORDERED);
		
		PreferencesUtil.getSettingsChangeNotifier().addChangeListener(this);
	}
	
	
	public void dispose() {
		PreferencesUtil.getSettingsChangeNotifier().removeChangeListener(this);
	}
	
	
	String getSettingsGroupId() {
		return fSettingsGroupId;
	}
	
	StringSetPref getPrefDefaultDisabledCategoryIds() {
		return fPrefDisabledCategoryIds;
	}
	
	StringArrayPref getPrefCirclingOrderedCategoryIds() {
		return fPrefOrderedCategoryIds;
	}
	
	public void settingsChanged(final Set<String> groupIds) {
		if (fSettingsGroupId == null || groupIds.contains(fSettingsGroupId)) {
			synchronized (this) {
				if (fCategories != null) {
					fCategories = Collections.unmodifiableList(applyPreferences(PreferencesUtil.getInstancePrefs(), fCategories));
				}
			}
		}
	}
	
	private List<ContentAssistCategory> loadExtensions() {
		final ArrayList<IConfigurationElement> categoryConfigs = new ArrayList<IConfigurationElement>(); // categories of all content types!
		final Map<String, List<ComputerDescriptor>> computersByCategoryId = new HashMap<String, List<ComputerDescriptor>>();
		
		final IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		
		synchronized (this) {
			if (fCategories != null) {
				return fCategories;
			}
			final IConfigurationElement[] contributions = extensionRegistry.getConfigurationElementsFor(AdvancedContentAssistInternal.EXTENSIONPOINT_ID);
			for (final IConfigurationElement config : contributions) {
				if (config.getName().equals(AdvancedContentAssistInternal.CONFIG_CATEGORY_ELEMENT_NAME)) {
					categoryConfigs.add(config);
					continue;
				}
				if (config.getName().equals(AdvancedContentAssistInternal.CONFIG_COMPUTER_ELEMENT_NAME)) {
					// Create computer descriptor
					String id = null;
					try {
						final String contentTypeId = AdvancedContentAssistInternal.getCheckedString(config, AdvancedContentAssistInternal.CONFIG_CONTENT_TYPE_ID_ATTRIBUTE_NAME);
						if (!fContentTypeId.equals(contentTypeId)) {
							continue;
						}
						id = AdvancedContentAssistInternal.getCheckedString(config, AdvancedContentAssistInternal.CONFIG_ID_ATTRIBUTE_NAME).intern();
						final String categoryId = AdvancedContentAssistInternal.getCheckedString(config, AdvancedContentAssistInternal.CONFIG_CATEGORY_ID_ATTRIBUTE_NAME);
						final Set<String> partitions = new HashSet<String>();
						final IConfigurationElement[] partitionConfigs = config.getChildren(AdvancedContentAssistInternal.CONFIG_PARTITION_ELEMENT_NAME);
						for (final IConfigurationElement partitionConfig : partitionConfigs) {
							partitions.add(AdvancedContentAssistInternal.getCheckedString(partitionConfig, AdvancedContentAssistInternal.CONFIG_CONTENTTYPE_ID_ELEMENT_NAME).intern());
						}
						checkPartitions(partitions);
						
						final ComputerDescriptor comp = new ComputerDescriptor(id, partitions, config);
						
						List<ComputerDescriptor> list = computersByCategoryId.get(categoryId);
						if (list == null) {
							list = new ArrayList<ComputerDescriptor>(4);
							computersByCategoryId.put(categoryId, list);
						}
						list.add(comp);
					}
					catch (final CoreException e) {
						StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN,
								NLS.bind("Loading Completion Proposal Computer failed (id = ''{0}'', contributed by = ''{1}'')", (id != null) ? id : "", config.getDeclaringExtension().getContributor().getName()), e));
					}
				}
			}
			
			final ArrayList<ContentAssistCategory> categories = new ArrayList<ContentAssistCategory>(8);
			for (final IConfigurationElement catConfig : categoryConfigs) {
				// Create category descriptor
				String id = null;
				try {
					id = AdvancedContentAssistInternal.getCheckedString(catConfig, AdvancedContentAssistInternal.CONFIG_ID_ATTRIBUTE_NAME);
					final List<ComputerDescriptor> descriptors = computersByCategoryId.get(id);
					if (descriptors != null) {
						final ImageDescriptor icon = AdvancedContentAssistInternal.getImageDescriptor(catConfig, AdvancedContentAssistInternal.CONFIG_ICON_ATTRIBUTE_NAME);
						final String name = AdvancedContentAssistInternal.getCheckedString(catConfig, AdvancedContentAssistInternal.CONFIG_NAME_ATTRIBUTE_NAME);
						final ContentAssistCategory cat = new ContentAssistCategory(id, name, icon, descriptors);
						categories.add(cat);
					}
				}
				catch (final CoreException e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, SharedUIResources.PLUGIN_ID, ICommonStatusConstants.INTERNAL_PLUGGED_IN,
							NLS.bind("Loading Completion Proposal Category failed (id = ''{0}'', contributed by = ''{1}'')", (id != null) ? id : "", catConfig.getDeclaringExtension().getContributor().getName()), e));
				}
			}
			return (fCategories = Collections.unmodifiableList(applyPreferences(PreferencesUtil.getInstancePrefs(), categories)));
		}
	}
	
	List<ContentAssistCategory> applyPreferences(final IPreferenceAccess prefAccess, final List<ContentAssistCategory> categories) {
		final Set<String> disabledIds = prefAccess.getPreferenceValue(getPrefDefaultDisabledCategoryIds());
		for (final ContentAssistCategory category : categories) {
			final boolean enabled = disabledIds == null || !disabledIds.contains(category.getId());
			category.fIsIncludedInDefault = enabled;
		}
		
		final Map<String, ContentAssistCategory> map = createCategoryByIdMap(categories);
		final String[] orderPref = prefAccess.getPreferenceValue(getPrefCirclingOrderedCategoryIds());
		final List<ContentAssistCategory> ordered = new ArrayList<ContentAssistCategory>(categories.size());
		for (final String value : orderPref) {
			final String id;
			final boolean enabled;
			final int idx = value.lastIndexOf(':');
			if (idx > 0) {
				id = value.substring(0, idx);
				enabled = Boolean.parseBoolean(value.substring(idx+1));
			}
			else { // fallback
				id = value;
				enabled = false;
			}
			final ContentAssistCategory category = map.remove(id);
			if (category != null) {
				ordered.add(category);
				category.fIsEnabledAsSeparate = enabled;
			}
		}
		for (final ContentAssistCategory category : map.values()) {
			ordered.add(category);
			category.fIsEnabledAsSeparate = false;
		}
		
		return ordered;
	}
	
	public List<ContentAssistCategory> getCopyOfCategories() {
		List<ContentAssistCategory> categories = fCategories;
		if (categories == null) {
			categories = loadExtensions();
		}
		final List<ContentAssistCategory> copies = new ArrayList<ContentAssistCategory>(categories.size());
		for (final ContentAssistCategory category : categories) {
			copies.add(new ContentAssistCategory(category));
		}
		return copies;
	}
	
	Map<Preference, Object> createPreferences(final List<ContentAssistCategory> orderedCategories) {
		final Set<String> disabledIds = new HashSet<String>();
		final String[] orderedPref = new String[orderedCategories.size()];
		
		for (int i = 0; i < orderedCategories.size(); i++) {
			final ContentAssistCategory category = orderedCategories.get(i);
			if (!category.fIsIncludedInDefault) {
				disabledIds.add(category.getId());
			}
			orderedPref[i] = category.getId() +
					(category.fIsEnabledAsSeparate ? ":true" : ":false");
		}
		
		final Map<Preference, Object> prefMap = new HashMap<Preference, Object>();
		prefMap.put(getPrefDefaultDisabledCategoryIds(), disabledIds);
		prefMap.put(getPrefCirclingOrderedCategoryIds(), orderedPref);
		return prefMap;
	}
	
	
	protected void checkPartitions(final Set<String> partitions) {
	}
	
	
	public List<ContentAssistCategory> getCategories() {
		List<ContentAssistCategory> categories = fCategories;
		if (categories == null) {
			categories = loadExtensions();
		}
		if (fSpecificModeId != null) {
			for (final ContentAssistCategory category : categories) {
				if (category.getId().equals(fSpecificModeId)) {
					final ContentAssistCategory copy = new ContentAssistCategory(category);
					copy.fIsIncludedInDefault = true;
					return Collections.singletonList(copy);
				}
			}
			return Collections.emptyList();
		}
		return categories;
	}
	
	public void startSpecificMode(final String categoryId) {
		fSpecificModeId = categoryId;
	}
	
	public void stopSpecificMode() {
		fSpecificModeId = null;
	}
	
	public boolean isInSpecificMode() {
		return (fSpecificModeId != null);
	}
	
}
