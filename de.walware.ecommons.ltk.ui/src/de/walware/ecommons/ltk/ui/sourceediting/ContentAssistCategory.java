/*******************************************************************************
 * Copyright (c) 2005-2011 WalWare/StatET-Project (www.walware.de/goto/statet)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Wahlbrink - adapted API and improvements
 *******************************************************************************/

package de.walware.ecommons.ltk.ui.sourceediting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;

import de.walware.ecommons.ui.util.MessageUtil;


/**
 * Describes a category for {@link ContentAssistProcessor}s.
 */
public final class ContentAssistCategory {
	
	
	private static final List<IContentAssistComputer> NO_COMPUTERS = Collections.emptyList();
	
	private final String fId;
	
	private final String fName;
	
	/** The image descriptor for this category, or <code>null</code> if none specified. */
	private final ImageDescriptor fImage;
	
	boolean fIsEnabledAsSeparate = false;
	
	boolean fIsIncludedInDefault = false;
	
	private final int fSortOrder = 0x10000;
	
	private final List<ContentAssistComputerRegistry.ComputerDescriptor> fComputerDescriptors;
	private final Map<String, List<IContentAssistComputer>> fComputersByPartition;
	
	
	public ContentAssistCategory(final String partitionId, final List<IContentAssistComputer> computers) {
		fId = "explicite"+partitionId;
		fName = null;
		fImage = null;
		fComputerDescriptors = Collections.emptyList();
		fComputersByPartition = new HashMap<String, List<IContentAssistComputer>>();
		fComputersByPartition.put(partitionId, computers);
		fIsIncludedInDefault = true;
	}
	
	ContentAssistCategory(final String id, final String name, final ImageDescriptor imageDsrc,
			final List<ContentAssistComputerRegistry.ComputerDescriptor> computers) {
		fId = id;
		fName = name;
		fImage = imageDsrc;
		fComputerDescriptors = computers;
		fComputersByPartition = new HashMap<String, List<IContentAssistComputer>>();
	}
	
	ContentAssistCategory(final ContentAssistCategory template) {
		fId = template.fId;
		fName = template.fName;
		fImage = template.fImage;
		fComputerDescriptors = template.fComputerDescriptors;
		fComputersByPartition = template.fComputersByPartition;
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
	 * Returns the name of the described extension.
	 * 
	 * @return Returns the name
	 */
	public String getName() {
		return fName;
	}
	
	/**
	 * Returns the name of the described extension
	 * without mnemonic hint in order to be displayed
	 * in a message.
	 * 
	 * @return Returns the name
	 */
	public String getDisplayName() {
		return MessageUtil.removeMnemonics(fName);
	}
	
	/**
	 * Returns the image descriptor of the described category.
	 * 
	 * @return the image descriptor of the described category
	 */
	public ImageDescriptor getImageDescriptor() {
		return fImage;
	}
	
	public boolean isEnabledInDefault() {
		return fIsIncludedInDefault;
	}
	
	public boolean isEnabledInCircling() {
		return fIsEnabledAsSeparate;
	}
	
//	public int getSortOrder() {
//		return fSortOrder;
//	}
//	
	public boolean hasComputers(final String contentTypeId) {
		final List<IContentAssistComputer> computers = fComputersByPartition.get(contentTypeId);
		if (computers == null) {
			for (final ContentAssistComputerRegistry.ComputerDescriptor dscr : fComputerDescriptors) {
				if (dscr.getPartitions().contains(contentTypeId)) {
					return true;
				}
			}
			fComputersByPartition.put(contentTypeId, NO_COMPUTERS);
			return false;
		}
		else {
			return !computers.isEmpty();
		}
	}
	
	public List<IContentAssistComputer> getComputers(final String contentTypeId) {
		List<IContentAssistComputer> computers = fComputersByPartition.get(contentTypeId);
		if (computers == null) {
			computers = initComputers(contentTypeId);
		}
		return computers;
	}
	
	private List<IContentAssistComputer> initComputers(final String contentTypeId) {
		final List<IContentAssistComputer> computers = new ArrayList<IContentAssistComputer>();
		for (final ContentAssistComputerRegistry.ComputerDescriptor dscr : fComputerDescriptors) {
			if (dscr.getPartitions().contains(contentTypeId)) {
				final IContentAssistComputer computer = dscr.getComputer();
				if (computer != null) {
					computers.add(computer);
				}
			}
		}
		fComputersByPartition.put(contentTypeId, computers);
		return computers;
	}
	
}
