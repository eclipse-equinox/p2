/*******************************************************************************
 * Copyright (c) 2011, 2017 WindRiver Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     IBM Corporation - Ongoing development
 *     Ericsson (AB) - bug 409073
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.FrameworkUtil;

public class ImportWizard extends InstallWizard implements IImportWizard {

	public ImportWizard() {
		this(ProvisioningUI.getDefaultUI(), null, null, null);
	}

	public ImportWizard(ProvisioningUI ui, InstallOperation operation, Collection<IInstallableUnit> initialSelections,
			LoadMetadataRepositoryJob preloadJob) {
		super(ui, operation, initialSelections, preloadJob);
		IDialogSettings workbenchSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(ImportFromInstallationWizard.class))
				.getDialogSettings();
		String sectionName = "ImportWizard"; //$NON-NLS-1$
		IDialogSettings section = workbenchSettings.getSection(sectionName);
		if (section == null) {
			section = workbenchSettings.addNewSection(sectionName);
		}
		setDialogSettings(section);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.ImportWizard_WINDOWTITLE);
		setDefaultPageImageDescriptor(ImageDescriptor
				.createFromURL(Platform.getBundle(Constants.Bundle_ID).getEntry("icons/wizban/install_wiz.png"))); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	@Override
	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		return new ImportPage(ui, this);
	}

	@Override
	protected ProvisioningContext getProvisioningContext() {
		return ((ImportPage) mainPage).getProvisioningContext();
	}

	@Override
	public void recomputePlan(IRunnableContext runnableContext) {
		recomputePlan(runnableContext, true);
	}

	/**
	 * Recompute the provisioning plan based on the items in the IUElementListRoot
	 * and the given provisioning context. Report progress using the specified
	 * runnable context. This method may be called before the page is created.
	 *
	 * @param runnableContext
	 */
	@Override
	public void recomputePlan(IRunnableContext runnableContext, final boolean withRemediation) {
		if (((ImportPage) mainPage).hasUnloadedRepo()) {
			try {
				runnableContext.run(true, true, monitor -> {
					final SubMonitor sub = SubMonitor.convert(monitor, withRemediation ? 15 : 10);
					((ImportPage) mainPage).recompute(sub.newChild(8));
					if (sub.isCanceled())
						throw new InterruptedException();
					Display.getDefault().syncExec(() -> {
						ProvisioningContext context = getProvisioningContext();
						initializeResolutionModelElements(getOperationSelections());
						if (planSelections.length == 0) {
							operation = new InstallOperation(new ProvisioningSession(AbstractPage.agent),
									new ArrayList<>()) {
								@Override
								protected void computeProfileChangeRequest(MultiStatus status,
										IProgressMonitor progressMonitor) {
									progressMonitor.done();
								}

								@Override
								public IStatus getResolutionResult() {
									if (sub.isCanceled())
										return Status.CANCEL_STATUS;
									return new Status(IStatus.ERROR, Constants.Bundle_ID,
											Messages.ImportWizard_CannotQuerySelection);
								}
							};
						} else {
							operation = getProfileChangeOperation(planSelections);
							operation.setProvisioningContext(context);
						}
					});
					if (sub.isCanceled())
						throw new InterruptedException();
					if (operation.resolveModal(sub.newChild(2)).getSeverity() == IStatus.CANCEL)
						throw new InterruptedException();
					if (withRemediation) {
						IStatus status = operation.getResolutionResult();
						if (remediationPage != null && shouldRemediate(status)) {
							computeRemediationOperation(operation, ui, sub.newChild(5));
						}
					}
					Display.getDefault().asyncExec(this::planChanged);
				});
			} catch (InterruptedException e) {
				operation = new InstallOperation(new ProvisioningSession(AbstractPage.agent), new ArrayList<>()) {

					@Override
					public IStatus getResolutionResult() {
						return Status.CANCEL_STATUS;
					}
				};
			} catch (InvocationTargetException e) {
				ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
				unableToResolve(null);
			}
		} else
			super.recomputePlan(runnableContext, withRemediation);
	}

	void unableToResolve(String message) {
		IStatus couldNotResolveStatus;
		if (message != null) {
			couldNotResolveStatus = new Status(IStatus.ERROR, Constants.Bundle_ID, message, null);
		} else {
			couldNotResolveStatus = new Status(IStatus.ERROR, Constants.Bundle_ID,
					ProvUIMessages.ProvisioningOperationWizard_UnexpectedFailureToResolve, null);
		}
		StatusManager.getManager().handle(couldNotResolveStatus, StatusManager.LOG);
	}
}
