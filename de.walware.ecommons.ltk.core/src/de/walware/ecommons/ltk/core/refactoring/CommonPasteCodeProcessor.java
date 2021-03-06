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

package de.walware.ecommons.ltk.core.refactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.osgi.util.NLS;

import de.walware.ecommons.ltk.internal.core.refactoring.Messages;


public abstract class CommonPasteCodeProcessor extends RefactoringProcessor {
	
	
	private final RefactoringAdapter fAdapter;
	
	private final String fCode;
	private final RefactoringDestination fDestination;
	
	private Change fInsertChange;
	
	
	public CommonPasteCodeProcessor(final String code, final RefactoringDestination destination,
			final RefactoringAdapter adapter) {
		assert (code != null);
		assert (destination != null);
		assert (adapter != null);
		
		fDestination = destination;
		fCode = code;
		
		fAdapter = adapter;
	}
	
	
	@Override
	public abstract String getIdentifier();
	
	protected abstract String getRefactoringIdentifier();
	
	@Override
	public String getProcessorName() {
		return Messages.PasteRefactoring_label; 
	}
	
	@Override
	public Object[] getElements() {
		return fDestination.getInitialObjects().toArray();
	}
	
	public Change getInsertChange() {
		return fInsertChange;
	}
	
	@Override
	public boolean isApplicable() throws CoreException {
		return fAdapter.canInsertTo(fDestination);
	}
	
	@Override
	public RefactoringStatus checkInitialConditions(final IProgressMonitor monitor)
			throws CoreException {
		final RefactoringStatus result = new RefactoringStatus();
		fAdapter.checkInitialToModify(result, fDestination);
		return result;
	}
	
	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor,
			final CheckConditionsContext context)
			throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, RefactoringMessages.Common_FinalCheck_label, 1); 
		try{
			final RefactoringStatus result = new RefactoringStatus();
			
			fDestination.postProcess();
			fAdapter.checkFinalToModify(result, fDestination, progress.newChild(1));
			
			final TextChangeManager textManager = new TextChangeManager();
			
			fInsertChange = fAdapter.createChangeToInsert(getProcessorName(),
					fCode, fDestination, textManager, progress.newChild(1) );
			
			final ResourceChangeChecker checker = (ResourceChangeChecker) context.getChecker(ResourceChangeChecker.class);
			final IResourceChangeDescriptionFactory deltaFactory = checker.getDeltaFactory();
			fAdapter.buildDeltaToModify(fDestination, deltaFactory);
			
			return result;
		}
		catch (final OperationCanceledException e) {
			throw e;
		}
		finally{
			progress.done();
		}
	}
	
	@Override
	public RefactoringParticipant[] loadParticipants(final RefactoringStatus status,
			final SharableParticipants shared)
			throws CoreException {
		final List<RefactoringParticipant> result = new ArrayList<>();
//		fAdapter.addParticipantsTo(fElementsToDelete, result, status, this, shared);
		return result.toArray(new RefactoringParticipant[result.size()]);
	}
	
	@Override
	public Change createChange(final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(RefactoringMessages.Common_CreateChanges_label, 1);
			final Map<String, String> arguments = new HashMap<>();
			final String description = Messages.PasteRefactoring_Code_description;
			final IProject resource = fDestination.getSingleProject();
			final String project = (resource != null) ? resource.getName() : null;
			final String source = (project != null) ? NLS.bind(RefactoringMessages.Common_Source_Project_label, project) : RefactoringMessages.Common_Source_Workspace_label;
//			final String header = NLS.bind(RefactoringCoreMessages.JavaDeleteProcessor_header, new String[] { String.valueOf(fElements.length), source});
//			final int flags = JavaRefactoringDescriptor.JAR_MIGRATION | JavaRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
//			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
//			if (fDeleteSubPackages)
//				comment.addSetting(RefactoringCoreMessages.JavaDeleteProcessor_delete_subpackages);
//		 	if (fAccessorsDeleted)
//				comment.addSetting(RefactoringCoreMessages.JavaDeleteProcessor_delete_accessors);
//			arguments.put(ATTRIBUTE_DELETE_SUBPACKAGES, Boolean.valueOf(fDeleteSubPackages).toString());
//			arguments.put(ATTRIBUTE_SUGGEST_ACCESSORS, Boolean.valueOf(fSuggestGetterSetterDeletion).toString());
//			arguments.put(ATTRIBUTE_RESOURCES, new Integer(fResources.length).toString());
//			for (int offset= 0; offset < fResources.length; offset++)
//				arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + (offset + 1), JavaRefactoringDescriptorUtil.resourceToHandle(project, fResources[offset]));
//			arguments.put(ATTRIBUTE_ELEMENTS, new Integer(fModelElements.length).toString());
//			for (int offset= 0; offset < fModelElements.length; offset++)
//				arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + (offset + fResources.length + 1), JavaRefactoringDescriptorUtil.elementToHandle(project, fModelElements[offset]));
			final int flags = 0;
			final String comment = ""; //$NON-NLS-1$
			
			final CommonRefactoringDescriptor descriptor = new CommonRefactoringDescriptor(
					getIdentifier(), project, description, comment, arguments, flags);
			
			return new DynamicValidationRefactoringChange(descriptor,
					getProcessorName(), 
					new Change[] { fInsertChange },
					null );
		}
		finally {
			monitor.done();
		}
	}
	
	
}
