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

package de.walware.ecommons.ltk.core.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.text.Collator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.DeleteArguments;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.ltk.internal.core.refactoring.Resources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;

import de.walware.ecommons.io.FileUtil;
import de.walware.ecommons.text.BasicHeuristicTokenScanner;
import de.walware.ecommons.text.PartitioningConfiguration;
import de.walware.ecommons.text.TextUtil;

import de.walware.ecommons.ltk.IModelElement;
import de.walware.ecommons.ltk.ISourceElement;
import de.walware.ecommons.ltk.ISourceStructElement;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.LTK;
import de.walware.ecommons.ltk.LTKUtil;
import de.walware.ecommons.ltk.internal.core.refactoring.Messages;


/**
 * Provides common functions for refacotring. 
 * Can be extended to adapt to language specific peculiarity.
 */
public abstract class RefactoringAdapter {
	
	
	private static final Comparator<IModelElement> MODELELEMENT_SORTER = new Comparator<IModelElement>() {
		
		private final Collator ID_COMPARATOR = Collator.getInstance();
		
		public int compare(final IModelElement e1, final IModelElement e2) {
			final ISourceUnit u1 = LTKUtil.getSourceUnit(e1);
			final ISourceUnit u2 = LTKUtil.getSourceUnit(e2);
			int result = 0;
			if (u1 != null && u2 != null) {
				if (u1 != u2) {
					result = ID_COMPARATOR.compare(u1.getId(), u2.getId());
				}
				if (result == 0) {
					if (e1 instanceof ISourceElement && e2 instanceof ISourceElement) {
						return (((ISourceStructElement) e1).getSourceRange().getOffset() - 
								((ISourceStructElement) e2).getSourceRange().getOffset());
					}
					if (e1 instanceof ISourceUnit) {
						if (e2 instanceof ISourceUnit) {
							return 0;
						}
						return -1000000;
					}
					else if (e2 instanceof ISourceUnit) {
						return 1000000;
					}
				}
				else {
					return result;
				}
				return ID_COMPARATOR.compare(e1.getId(), e2.getId());
			}
			if (u1 == null && u2 != null) {
				return Integer.MAX_VALUE;
			}
			if (u2 == null && u1 != null) {
				return Integer.MIN_VALUE;
			}
			return 0;
		}
	};
	
	
	protected final BasicHeuristicTokenScanner fScanner;
	protected final PartitioningConfiguration fPartitioning;
	
	
	public RefactoringAdapter(final BasicHeuristicTokenScanner scanner) {
		fScanner = scanner;
		fPartitioning = scanner.getPartitioningConfig();
	}
	
	
	public Comparator<IModelElement> getModelElementComparator() {
		return MODELELEMENT_SORTER;
	}
	
	public abstract String getPluginIdentifier();
	
	public abstract boolean isCommentContent(final ITypedRegion partition);
	
	/**
	 * - Sort elements
	 * - Removes nested children.
	 * 
	 * @param elements must be sorted by unit and order
	 * @return
	 */
	public ISourceStructElement[] checkElements(final ISourceStructElement[] elements) {
		if (elements.length <= 1) {
			return elements;
		}
		Arrays.sort(elements, getModelElementComparator());
		ISourceStructElement last = elements[0];
		ISourceUnit unitOfLast = last.getSourceUnit();
		int endOfLast = last.getSourceRange().getOffset()+last.getSourceRange().getLength();
		final List<ISourceStructElement> checked = new ArrayList<ISourceStructElement>(elements.length);
		for (final ISourceStructElement element : elements) {
			final ISourceUnit unit = element.getSourceUnit();
			final int end = last.getSourceRange().getOffset()+last.getSourceRange().getLength();
			if (unit != unitOfLast) {
				checked.add(element);
				last = element;
				unitOfLast = unit;
				endOfLast = end;
				continue;
			}
			if (end > endOfLast) {
				checked.add(element);
				last = element;
				endOfLast = end;
				continue;
			}
			// is child, ignore
			continue;
		}
		return checked.toArray(new ISourceStructElement[checked.size()]);
	}
	
	public IRegion getContinuousSourceRange(final ISourceStructElement[] elements) {
		if (elements == null || elements.length == 0) {
			return null;
		}
		final ISourceUnit sourceUnit = elements[0].getSourceUnit();
		if (sourceUnit == null) {
			return null;
		}
		final AbstractDocument doc = sourceUnit.getDocument(null);
		if (doc == null) {
			return null;
		}
		
		// check if no other code is between the elements
		// and create one single range including comments at line end
		try {
			fScanner.configure(doc);
			final int start = elements[0].getSourceRange().getOffset();
			int end = elements[0].getSourceRange().getOffset() + elements[0].getSourceRange().getLength();
			
			for (int i = 1; i < elements.length; i++) {
				if (elements[i].getSourceUnit() != sourceUnit) {
					return null;
				}
				final int elementStart = elements[i].getSourceRange().getOffset();
				final int elementEnd = elementStart + elements[i].getSourceRange().getLength();
				if (elementEnd <= end) {
					continue;
				}
				int match;
				while (end < elementStart &&
						(match = fScanner.findAnyNonBlankForward(end, elementStart, true)) >= 0) {
					final ITypedRegion partition = doc.getPartition(fPartitioning.getPartitioning(), match, false);
					if (isCommentContent(partition)) {
						end = partition.getOffset() + partition.getLength();
					}
					else {
						return null;
					}
				}
				end = elementEnd;
			}
			final IRegion lastLine = doc.getLineInformationOfOffset(end);
			final int match = fScanner.findAnyNonBlankForward(end, lastLine.getOffset()+lastLine.getLength(), true);
			if (match >= 0) {
				final ITypedRegion partition = doc.getPartition(fPartitioning.getPartitioning(), match, false);
				if (isCommentContent(partition)) {
					end = partition.getOffset() + partition.getLength();
				}
			}
			return new Region(start, end-start);
		}
		catch (final BadPartitioningException e) {
		}
		catch (final BadLocationException e) {
		}
		return null;
	}
	
	public String getSourceCodeStringedTogether(final ISourceStructElement[] sourceElements, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, sourceElements.length * 2);
		ISourceUnit lastUnit = null;
		try {
			final RefactoringElementSet elements = new RefactoringElementSet(sourceElements);
			elements.removeElementsWithAncestorsOnList();
			Collections.sort(elements.getModelElements(), getModelElementComparator());
			final String lineDelimiter = TextUtil.getPlatformLineDelimiter();
			
			AbstractDocument doc = null;
			final List<IModelElement> modelElements = elements.getModelElements();
			int todo = modelElements.size();
			
			final StringBuilder sb = new StringBuilder(todo*100);
			for (final IModelElement element : modelElements) {
				final ISourceUnit su = LTKUtil.getSourceUnit(element);
				if (su != lastUnit) {
					if (lastUnit != null) {
						progress.setWorkRemaining(todo*2);
						lastUnit.disconnect(progress.newChild(1));
						lastUnit = null;
					}
					su.connect(progress.newChild(1));
					lastUnit = su;
					doc = su.getDocument(monitor);
				}
				final IRegion range = expandElementRange((ISourceStructElement) element, doc);
				sb.append(doc.get(range.getOffset(), range.getLength()));
				sb.append(lineDelimiter);
				
				todo--;
			}
			return sb.toString();
		}
		catch (final BadLocationException e) {
			throw new CoreException(failDocAnalyzation(e));
		}
		catch (final BadPartitioningException e) {
			throw new CoreException(failDocAnalyzation(e));
		}
		finally {
			if (lastUnit != null) {
				progress.setWorkRemaining(1);
				lastUnit.disconnect(progress.newChild(1));
				lastUnit = null;
			}
		}
	}
	
	public IRegion expandElementRange(final ISourceElement element, final AbstractDocument doc) 
			throws BadLocationException, BadPartitioningException {
		final IRegion sourceRange = element.getSourceRange();
		int start = sourceRange.getOffset();
		int end = start + sourceRange.getLength();
		
		final IRegion docRange = element.getDocumentationRange();
		if (docRange != null) {
			if (docRange.getOffset() < start) {
				start = docRange.getOffset();
			}
			if (docRange.getOffset()+docRange.getLength() > end) {
				end = docRange.getOffset()+docRange.getLength();
			}
		}
		
		fScanner.configure(doc);
		
		IRegion lastLineInfo;
		int match;
		lastLineInfo = doc.getLineInformationOfOffset(end);
		match = fScanner.findAnyNonBlankForward(end, lastLineInfo.getOffset()+lastLineInfo.getLength(), true);
		if (match >= 0) {
			final ITypedRegion partition = doc.getPartition(fPartitioning.getPartitioning(), match, false);
			if (isCommentContent(partition)) {
				end = partition.getOffset() + partition.getLength();
			}
		}
		final int checkLine = doc.getLineOfOffset(end)+1;
		if (checkLine < doc.getNumberOfLines()) {
			final IRegion checkLineInfo = doc.getLineInformation(checkLine);
			match = fScanner.findAnyNonBlankForward(
					end, checkLineInfo.getOffset()+checkLineInfo.getLength(), true);
			if (match < 0) {
				end = checkLineInfo.getOffset()+checkLineInfo.getLength();
			}
		}
		
		return new Region(start, end-start);
	}
	
	public IRegion expandWhitespaceBlock(final AbstractDocument document, final IRegion region) throws BadLocationException {
		fScanner.configure(document);
		final int firstLine = document.getLineOfOffset(region.getOffset());
		int lastLine = document.getLineOfOffset(region.getOffset()+region.getLength());
		if (lastLine > firstLine && document.getLineOffset(lastLine) == region.getOffset()+region.getLength()) {
			lastLine--;
		}
		int result;
		final int min = document.getLineOffset(firstLine);
		final int max = document.getLineOffset(lastLine)+document.getLineLength(lastLine);
		result = fScanner.findAnyNonBlankForward(region.getOffset()+region.getLength(), max, true);
		final int end = (result >= 0) ? result : max;
		result = fScanner.findAnyNonBlankBackward(region.getOffset(), min, true);
		if (result >= 0) {
			return new Region(result+1, end-(result+1));
		}
		else {
			return new Region(min, end-min);
		}
	}
	
	
	public boolean canDelete(final RefactoringElementSet elements) {
		if (elements.getInitialObjects().length == 0) {
			return false;
		}
		if (elements.isOK()) {
			return false;
		}
		for (final IModelElement element : elements.getModelElements()) {
			if (!canDelete(element)) {
				return false;
			}
		}
		for (final IResource element : elements.getResources()) {
			if (!canDelete(element)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canDelete(final IModelElement element) {
		if (!element.exists()) {
			return false;
		}
//		if ((element.getElementType() & IModelElement.MASK_C1) == IModelElement.PROJECT) {
//			return false;
//		}
		if (element.isReadOnly()) {
			return false;
		}
		return true;
	}
	
	public boolean canDelete(final IResource resource) {
		if (!resource.exists() || resource.isPhantom()) {
			return false;
		}
		if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT) {
			return false;
		}
		if (resource.getParent() != null) {
			final ResourceAttributes attributes = resource.getParent().getResourceAttributes();
			if (attributes != null && attributes.isReadOnly()) {
				return false;
			}
		}
		return true;
	}
	
	public void checkInitialForModification(final RefactoringStatus result, final RefactoringElementSet elements) {
		final Set<IResource> resources = new HashSet<IResource>();
		resources.addAll(elements.getResources());
		for(final IModelElement element : elements.getModelElements()) {
			final ISourceUnit su = LTKUtil.getSourceUnit(element);
			if (su != null) {
				final IResource resource = su.getResource();
				if (resource != null) {
					resources.add(resource);
					continue;
				}
			}
			result.addFatalError(Messages.Check_ElementNotInWS_message);
			return;
		}
		result.merge(RefactoringStatus.create(
				Resources.checkInSync(resources.toArray(new IResource[resources.size()]))
				));
	}
	
	public void checkFinalForModification(final RefactoringStatus result, final RefactoringElementSet elements, final IProgressMonitor monitor) {
		final Set<IResource> resources = new HashSet<IResource>();
		resources.addAll(elements.getResources());
		for(final IModelElement element : elements.getModelElements()) {
			final ISourceUnit su = LTKUtil.getSourceUnit(element);
			if (su != null) {
				final IResource resource = su.getResource();
				if (resource != null) {
					resources.add(resource);
					continue;
				}
			}
			result.addFatalError(Messages.Check_ElementNotInWS_message);
			return;
		}
		final IResource[] array = resources.toArray(new IResource[resources.size()]);
		result.merge(RefactoringStatus.create(Resources.checkInSync(array)));
		result.merge(RefactoringStatus.create(Resources.makeCommittable(array, IWorkspace.VALIDATE_PROMPT)));
	}
	
	public void checkFinalToDelete(final RefactoringStatus result, final RefactoringElementSet elements) throws CoreException {
		for (final IModelElement element : elements.getModelElements()) {
			checkFinalToDeletion(result, element);
		}
		for (final IResource element : elements.getResources()) {
			checkFinalToDelete(result, element);
		}
	}
	
	public void checkFinalToDelete(final RefactoringStatus result, final IResource element) throws CoreException {
		if (element.getType() == IResource.FILE) {
			warnIfDirty(result, (IFile) element);
			return;
		}
		else {
			element.accept(new IResourceVisitor() {
				public boolean visit(final IResource visitedResource) throws CoreException {
					if (visitedResource instanceof IFile) {
						warnIfDirty(result, (IFile) visitedResource);
					}
					return true;
				}
			}, IResource.DEPTH_INFINITE, false);
		}
	}
	
	public void checkFinalToDeletion(final RefactoringStatus result, final IModelElement element) throws CoreException {
		if ((element.getElementType() & IModelElement.MASK_C2) == IModelElement.C2_SOURCE_FILE) {
			final IResource resource = ((ISourceUnit) element).getResource();
			if (resource != null) {
				checkFinalToDelete(result, resource);
			}
		}
		else if ((element.getElementType() & IModelElement.MASK_C1) == IModelElement.C1_BUNDLE
				&& element instanceof ISourceStructElement) {
			final List<? extends IModelElement> children = ((ISourceStructElement) element).getSourceChildren(null);
			for (final IModelElement child : children) {
				checkFinalToDeletion(result, child);
			}
		}
	}
	
	public void warnIfDirty(final RefactoringStatus result, final IFile file) {
		if (file == null || !file.exists()) {
			return;
		}
		final ITextFileBuffer buffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
		if (buffer != null && buffer.isDirty()) {
			if (buffer.isStateValidated() && buffer.isSynchronized()) {
				result.addWarning(NLS.bind(
					Messages.Check_FileUnsavedChanges_message,
					FileUtil.getFileUtil(file).getFileLabel()) );
			} else {
				result.addFatalError(NLS.bind(
					Messages.Check_FileUnsavedChanges_message, 
					FileUtil.getFileUtil(file).getFileLabel()) );
			}
		}
	}
	
	public boolean confirmDeleteOfReadOnlyElements(final RefactoringElementSet elements, final Object queries) throws CoreException {
		// TODO add query support
		return hasReadOnlyElements(elements);
	}
	
	public boolean hasReadOnlyElements(final RefactoringElementSet elements) throws CoreException {
		for (final IResource element : elements.getResources()) {
			if (hasReadOnlyElements(element)) {
				return true;
			}
		}
		for (final IModelElement element : elements.getModelElements()) {
			if (hasReadOnlyElements(element)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasReadOnlyElements(final IResource element) throws CoreException {
		if (isReadOnly(element)) {
			return true;
		}
		if (element instanceof IContainer) {
			final IResource[] members = ((IContainer) element).members(false);
			for (final IResource member : members) {
				if (hasReadOnlyElements(member)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean hasReadOnlyElements(final IModelElement element) throws CoreException {
		final ISourceUnit sourceUnit = LTKUtil.getSourceUnit(element);
		IResource resource = null;
		if (sourceUnit != null) {
			resource = sourceUnit.getResource();
		}
		if (resource == null) {
			resource = (IResource) element.getAdapter(IResource.class);
		}
		if (resource != null) {
			return hasReadOnlyElements(resource);
		}
		return false;
	}
	
	public boolean isReadOnly(final IResource element) {
		final ResourceAttributes attributes = element.getResourceAttributes();
		if (attributes != null) {
			return attributes.isReadOnly();
		}
		return false;
	}
	
	
	public void addParticipantsToDelete(final RefactoringElementSet elements,
			final List<RefactoringParticipant> list,
			final RefactoringStatus status, final RefactoringProcessor processor, 
			final String[] natures, final SharableParticipants shared) {
		final DeleteArguments arguments = new DeleteArguments();
		for (final IResource resource : elements.getResources()) {
			final DeleteParticipant[] deletes = ParticipantManager.loadDeleteParticipants(status, 
				processor, resource, 
				arguments, natures, shared);
			list.addAll(Arrays.asList(deletes));
		}
		for (final IResource resource : elements.getResourcesOwnedByElements()) {
			final DeleteParticipant[] deletes = ParticipantManager.loadDeleteParticipants(status, 
				processor, resource, 
				arguments, natures, shared);
			list.addAll(Arrays.asList(deletes));
		}
		for (final IModelElement element : elements.getModelElements()) {
			final DeleteParticipant[] deletes = ParticipantManager.loadDeleteParticipants(status, 
				processor, element, 
				arguments, natures, shared);
			list.addAll(Arrays.asList(deletes));
		}
	}
	
	public void buildDeltaToDelete(final RefactoringElementSet elements,
			final IResourceChangeDescriptionFactory resourceDelta) {
		for (final IResource resource : elements.getResources()) {
			resourceDelta.delete(resource);
		}
		for (final IResource resource : elements.getResourcesOwnedByElements()) {
			resourceDelta.delete(resource);
		}
		for (final IFile file : elements.getFilesContainingElements()) {
			resourceDelta.change(file);
		}
	}
	
	/**
	 * @param changeName the name of the change
	 * @param resources the resources to delete
	 * @param manager the text change manager
	 * @return the created change
	 * @throws CoreException 
	 */
	public Change createChangeToDelete(final String changeName, 
			final RefactoringElementSet elementsToDelete,
			final TextChangeManager manager, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor);
		final CompositeChange result = new CompositeChange(changeName);
		
		addChangesToDelete(result, elementsToDelete, manager, progress);
		
		return result;
	}
	
	protected void addChangesToDelete(final CompositeChange result, 
			final RefactoringElementSet elements,
			final TextChangeManager manager, final SubMonitor progress) throws CoreException {
		for (final IResource resource : elements.getResources()) {
			result.add(createChangeToDelete(elements, resource));
		}
		final Map<ISourceUnit, List<IModelElement>> suSubChanges = new HashMap<ISourceUnit, List<IModelElement>>();
		for (final IModelElement element : elements.getModelElements()) {
			final IResource resource = elements.getOwningResource(element);
			if (resource != null) {
				result.add(createChangeToDelete(elements, resource));
			}
			else {
				final ISourceUnit su = LTKUtil.getSourceUnit(element);
				List<IModelElement> list = suSubChanges.get(su);
				if (list == null) {
					list = new ArrayList<IModelElement>(1);
					suSubChanges.put(su, list);
				}
				list.add(element);
			}
		}
		if (!suSubChanges.isEmpty()) {
			progress.setWorkRemaining(suSubChanges.size()*3);
			for (final Map.Entry<ISourceUnit, List<IModelElement>> suChanges : suSubChanges.entrySet()) {
				result.add(createChangeToDelete(elements, suChanges.getKey(), suChanges.getValue(), manager, progress));
			}
		}
	}
	
	private Change createChangeToDelete(final RefactoringElementSet elements,
			final ISourceUnit su, final List<IModelElement> elementsInUnit,
			final TextChangeManager manager, final SubMonitor progress) throws CoreException {
		if (su == null || su.getResource() == null || su.getResource().getType() != IResource.FILE) {
			throw new IllegalArgumentException();
		}
		su.connect(progress.newChild(1));
		try {
			final TextFileChange textFileChange = manager.get(su);
			if (su.getWorkingContext() == LTK.EDITOR_CONTEXT) {
				textFileChange.setSaveMode(TextFileChange.LEAVE_DIRTY);
			}
			final MultiTextEdit rootEdit = new MultiTextEdit();
			textFileChange.setEdit(rootEdit);
			
			for (final IModelElement element : elementsInUnit) {
				final ISourceElement member = (ISourceElement) element;
				final IRegion sourceRange = expandElementRange(member, member.getSourceUnit().getDocument(null));
				final DeleteEdit edit = new DeleteEdit(sourceRange.getOffset(), sourceRange.getLength());
				rootEdit.addChild(edit);
			}
			progress.worked(1);
			
			return textFileChange;
		}
		catch (final BadLocationException e) {
			throw new CoreException(failCreation(e));
		}
		catch (final BadPartitioningException e) {
			throw new CoreException(failCreation(e));
		}
		finally {
			su.disconnect(progress.newChild(1));
		}
	}
	
	protected Change createChangeToDelete(final RefactoringElementSet elements, final IModelElement element) throws CoreException {
		final IResource resource = elements.getOwningResource(element);
		if (resource != null) {
			return createChangeToDelete(elements, resource);
		}
		throw new IllegalStateException(); 
	}
	
	protected Change createChangeToDelete(final RefactoringElementSet elements, final IResource resource) {
		if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT) {
			throw new IllegalStateException();
		}
		return new DeleteResourceChange(resource.getFullPath(), true);
	}
	
	protected Change createChangeToDelete(final RefactoringElementSet elements, final ISourceUnit su) throws CoreException {
		final IResource resource = su.getResource();
		if (resource != null) {
			return createChangeToDelete(elements, resource);
		}
		throw new IllegalStateException();
	}
	
	protected IStatus failDocAnalyzation(final Throwable e) {
		return new Status(IStatus.ERROR, LTK.PLUGIN_ID, Messages.Common_error_AnalyzingSourceDocument_message);
	}
	
	protected IStatus failCreation(final Throwable e) {
		return new Status(IStatus.ERROR, LTK.PLUGIN_ID, Messages.Common_error_CreatingElementChange_message);
	}
	
}
