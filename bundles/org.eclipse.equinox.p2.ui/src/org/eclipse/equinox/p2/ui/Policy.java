/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * The Policy class is used to locate application specific policies that
 * should be used in the standard p2 UI class libraries.   The default policy
 * is acquired using the OSGi service model.
 * 
 * Policy allows clients to specify things such as how repositories 
 * are manipulated in the standard wizards and dialogs, and how the repositories
 * or the installation itself should be traversed when displaying content.
 * 
 * In some cases, the Policy is used only to define a default value that can
 * be overridden by user choice and subsequently stored in dialog settings.
 * 
 * Client applications should ensure that their Policy is registered before
 * any of the p2 UI objects access the default Policy.  
 * 
 * @since 2.0
 */

public class Policy {

	/**
	 * A constant indicating that restart should be forced (without
	 * confirmation) immediately after completion of a provisioning operation.
	 * 
	 * @since 3.6
	 */
	public static final int FORCE_RESTART = 1;

	/**
	 * A constant indicating that the changes should be applied dynamically
	 * to the profile (without confirmation) immediately after completion of 
	 * a provisioning operation.
	 * 
	 * @since 3.6
	 */
	public static final int FORCE_APPLY = 2;

	/**
	 * A constant indicating that the user should be prompted to
	 * restart after completion of a provisioning operation.
	 * 
	 * @since 3.6
	 */
	public static final int PROMPT_RESTART = 3;

	/**
	 * A constant indicating that, where possible, the user should 
	 * be given the option to restart or dynamically apply the changes
	 * after completion of a provisioning operation.
	 * 
	 * @since 3.6
	 */
	public static final int PROMPT_RESTART_OR_APPLY = 4;

	private LicenseManager licenseManager;
	private IUViewQueryContext queryContext;
	private RepositoryManipulator repositoryManipulator;
	private int restartPolicy = PROMPT_RESTART_OR_APPLY;

	/**
	 * Returns the license manager used to remember accepted licenses
	 * 
	 * @return the licenseManager
	 */
	public LicenseManager getLicenseManager() {
		if (licenseManager == null) {
			licenseManager = getDefaultLicenseManager();
		}
		return licenseManager;
	}

	/**
	 * Set the license manager used to remember accepted licenses.
	 * 
	 * @param manager the manager to use, or <code>null</code> to use 
	 * the default manager
	 */
	public void setLicenseManager(LicenseManager manager) {
		licenseManager = manager;
	}

	/**
	 * Returns the plan validator used to validate a proposed provisioning
	 * plan
	 * 
	 * @return the plan validator
	 */
	/**
	 * Get the query context that is used to drive the filtering and 
	 * traversal of any IU views
	 * 
	 * @return the queryContext
	 */
	public IUViewQueryContext getQueryContext() {
		if (queryContext == null) {
			queryContext = getDefaultQueryContext();
		}
		return queryContext;
	}

	/**
	 * Set the query context that is used to drive the filtering and
	 * traversal of any IU views
	 * 
	 * @param context the context to use, or <code>null</code> to use 
	 * the default context
	 */
	public void setQueryContext(IUViewQueryContext context) {
		queryContext = context;
	}

	/**
	 * Get the repository manipulator that is used to perform repository
	 * operations given a URL.
	 * 
	 * @return the repository manipulator
	 */
	public RepositoryManipulator getRepositoryManipulator() {
		if (repositoryManipulator == null)
			repositoryManipulator = getDefaultRepositoryManipulator();
		return repositoryManipulator;
	}

	/**
	 * Set the repository manipulator that is used to perform repository
	 * operations given a URL.
	 * 
	 * @param manipulator the manipulator to use, or <code>null</code> to use 
	 * the default manipulator
	 */
	public void setRepositoryManipulator(RepositoryManipulator manipulator) {
		repositoryManipulator = manipulator;
	}

	/**
	 * Reset all of the policies to their default values
	 */
	public void reset() {
		licenseManager = null;
		queryContext = null;
		repositoryManipulator = null;
	}

	/**
	 * Answer a boolean indicating whether the caller should continue to work with the
	 * specified operation.  This method is used when an operation has been resolved, but
	 * the UI may have further restrictions on continuing with it.
	 * 
	 * @param operation the operation in question.  It must already be resolved.
	 * @param shell the shell to use for any interaction with the user
	 * @return <code>true</code> if processing of the operation should continue, <code>false</code> if
	 * not.  It is up to the implementor to report any errors to the user when answering <code>false</code>.
	 */
	public boolean continueWorkingWithOperation(ProfileChangeOperation operation, Shell shell) {
		Assert.isTrue(operation.hasResolved());
		IStatus status = operation.getResolutionResult();
		// user cancelled
		if (status.getSeverity() == IStatus.CANCEL)
			return false;

		// Special case those statuses where we would never want to open a wizard
		if (status.getCode() == ProvisioningSession.STATUS_NOTHING_TO_UPDATE) {
			ProvUI.reportStatus(status, StatusManager.BLOCK);
			return false;
		}

		// there is no plan, so we can't continue.  Report any reason found
		if (operation.getProvisioningPlan() == null && !status.isOK()) {
			StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
			return false;
		}

		// If the plan requires install handler support, we want to open the old update UI and
		// cancel this operation
		if (UpdateManagerCompatibility.requiresInstallHandlerSupport(operation.getProvisioningPlan())) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					Shell shell = ProvUI.getDefaultParentShell();
					MessageDialog dialog = new MessageDialog(shell, ProvUIMessages.Policy_RequiresUpdateManagerTitle, null, ProvUIMessages.Policy_RequiresUpdateManagerMessage, MessageDialog.WARNING, new String[] {ProvUIMessages.LaunchUpdateManagerButton, IDialogConstants.CANCEL_LABEL}, 0);
					if (dialog.open() == 0)
						BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
							public void run() {
								UpdateManagerCompatibility.openInstaller();
							}
						});
				}
			});
			return false;
		}
		// Allow the wizard to open otherwise.
		return true;
	}

	/*
	 * Returns the license manager to use if none has been set.
	 */
	private LicenseManager getDefaultLicenseManager() {
		return new SimpleLicenseManager();
	}

	/*
	 * Returns an IUViewQueryContext with default values
	 */
	private IUViewQueryContext getDefaultQueryContext() {
		return new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_BY_REPO);
	}

	private RepositoryManipulator getDefaultRepositoryManipulator() {
		return new ColocatedRepositoryManipulator(null);
	}

	/**
	 * Return a status that can be used to describe the failure to
	 * retrieve a profile.
	 * @return a status describing a failure to retrieve a profile,
	 * or <code>null</code> if there is no such status.
	 */
	public IStatus getNoProfileChosenStatus() {
		return null;
	}

	/**
	 * Return a constant that indicates how restarts are handled after
	 * completion of a provisioning operation.
	 * 
	 * @return an integer constant indicating how restarts are to be
	 * handled.  
	 * 
	 * @see #FORCE_RESTART
	 * @see #FORCE_APPLY
	 * @see #PROMPT_RESTART
	 * @see #PROMPT_RESTART_OR_APPLY
	 */
	public int getRestartPolicy() {
		return restartPolicy;
	}

	/**
	 * Set a constant that indicates how restarts are handled after
	 * completion of a provisioning operation.
	 * 
	 * @param policy an integer constant indicating how restarts are to be
	 * handled.  Should be one of <code>FORCE_RESTART</code>, <code>FORCE_APPLY</code>, 
	 * <code>PROMPT_RESTART</code>, or <code>PROMPT_RESTART_OR_APPLY</code>.
	 * 
	 * @see #FORCE_RESTART
	 * @see #FORCE_APPLY
	 * @see #PROMPT_RESTART
	 * @see #PROMPT_RESTART_OR_APPLY
	 */
	public void setRestartPolicy(int policy) {
		restartPolicy = policy;
	}
}
