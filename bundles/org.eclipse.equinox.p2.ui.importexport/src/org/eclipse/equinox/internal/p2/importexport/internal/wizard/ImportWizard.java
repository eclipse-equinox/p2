/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.importexport.internal.Constants;
import org.eclipse.equinox.internal.p2.importexport.internal.Messages;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;

public class ImportWizard extends InstallWizard implements IImportWizard {

	public ImportWizard() {
		this(ProvisioningUI.getDefaultUI(), null, null, null);
	}

	public ImportWizard(ProvisioningUI ui, InstallOperation operation, Collection<IInstallableUnit> initialSelections, LoadMetadataRepositoryJob preloadJob) {
		super(ui, operation, initialSelections, preloadJob);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.ImportWizard_WINDOWTITLE);
		setDefaultPageImageDescriptor(ImageDescriptor.createFromURL(Platform.getBundle(Constants.Bundle_ID).getEntry("icons/install_wiz.gif"))); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	@Override
	protected ISelectableIUsPage createMainPage(IUElementListRoot input,
			Object[] selections) {
		return new ImportPage(ui, this);
	}

	@Override
	protected ProvisioningContext getProvisioningContext() {
		return ((ImportPage) mainPage).getProvisioningContext();
	}

	/**
	 * Recompute the provisioning plan based on the items in the IUElementListRoot and the given provisioning context.
	 * Report progress using the specified runnable context.  This method may be called before the page is created.
	 * 
	 * @param runnableContext
	 */
	@Override
	public void recomputePlan(IRunnableContext runnableContext) {
		if (((ImportPage) mainPage).hasUnloadedRepo()) {
			try {
				runnableContext.run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InterruptedException {						
						SubMonitor sub = SubMonitor.convert(monitor, 1000);
						((ImportPage) mainPage).recompute(sub.newChild(800));
						if (sub.isCanceled())
							throw new InterruptedException();
						Display.getDefault().syncExec(new Runnable() {

							public void run() {
								ProvisioningContext context = getProvisioningContext();
								initializeResolutionModelElements(getOperationSelections());
								if (planSelections.length == 0) {
									operation = null;
									couldNotResolve(ProvUIMessages.ResolutionWizardPage_NoSelections);
								} else {
									operation = getProfileChangeOperation(planSelections);
									operation.setProvisioningContext(context);
								}
							}
						});
						if (sub.isCanceled())
							throw new InterruptedException();
						operation.resolveModal(sub.newChild(200));
						Display.getDefault().asyncExec(new Runnable() {

							public void run() {
								planChanged();
							}
						});						
					}
				});				
			} catch (InterruptedException e) {
				// Nothing to report if thread was interrupted
			} catch (InvocationTargetException e) {
				ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
				couldNotResolve(null);
			}
		} else
			super.recomputePlan(runnableContext);
	}

	void couldNotResolve(String message) {
		IStatus couldNotResolveStatus;
		if (message != null) {
			couldNotResolveStatus = new Status(IStatus.ERROR, Constants.Bundle_ID, message, null);
		} else {
			couldNotResolveStatus = new Status(IStatus.ERROR, Constants.Bundle_ID, ProvUIMessages.ProvisioningOperationWizard_UnexpectedFailureToResolve, null);
		}
		StatusManager.getManager().handle(couldNotResolveStatus, StatusManager.LOG);
	}
}
