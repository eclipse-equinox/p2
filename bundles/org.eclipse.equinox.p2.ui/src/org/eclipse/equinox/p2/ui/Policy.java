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
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.query.GroupQuery;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * The Policy class is used to specify application specific policies that
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
	*/
	public static final int RESTART_POLICY_FORCE = 1;

	/**
	 * A constant indicating that the changes should be applied dynamically
	 * to the profile (without confirmation) immediately after completion of 
	 * a provisioning operation.
	 */
	public static final int RESTART_POLICY_FORCE_APPLY = 2;

	/**
	 * A constant indicating that the user should be prompted to
	 * restart after completion of a provisioning operation.
	 */
	public static final int RESTART_POLICY_PROMPT = 3;

	/**
	 * A constant indicating that, where possible, the user should 
	 * be given the option to restart or dynamically apply the changes
	 * after completion of a provisioning operation.
	 */
	public static final int RESTART_POLICY_PROMPT_RESTART_OR_APPLY = 4;

	private IQuery visibleAvailableIUQuery = new GroupQuery();
	private IQuery visibleInstalledIUQuery = new UserVisibleRootQuery();
	private boolean groupByCategory = true;
	private boolean allowDrilldown = true;
	private boolean repositoriesVisible = true;
	private boolean showLatestVersionsOnly = true;
	private int restartPolicy = RESTART_POLICY_PROMPT_RESTART_OR_APPLY;
	private String repoPrefPageId;
	private String repoPrefPageName;

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

	/**
	 * Return a status that can be used to describe the failure to
	 * retrieve a profile.
	 * @return a status describing a failure to retrieve a profile,
	 * or <code>null</code> if there is no such status.
	 */
	public IStatus getNoProfileChosenStatus() {
		return null;
	}

	public IQuery getVisibleAvailableIUQuery() {
		return visibleAvailableIUQuery;
	}

	public void setVisibleAvailableIUQuery(IQuery query) {
		visibleAvailableIUQuery = query;
	}

	public IQuery getVisibleInstalledIUQuery() {
		return visibleInstalledIUQuery;
	}

	public void setVisibleInstalledIUQuery(IQuery query) {
		visibleInstalledIUQuery = query;
	}

	public int getRestartPolicy() {
		return restartPolicy;
	}

	public void setRestartPolicy(int restartPolicy) {
		this.restartPolicy = restartPolicy;
	}

	public boolean getRepositoriesVisible() {
		return repositoriesVisible;
	}

	public void setRepositoriesVisible(boolean visible) {
		this.repositoriesVisible = visible;
	}

	public boolean getShowLatestVersionsOnly() {
		return showLatestVersionsOnly;
	}

	public void setShowLatestVersionsOnly(boolean showLatest) {
		this.showLatestVersionsOnly = showLatest;
	}

	public boolean getShowDrilldownRequirements() {
		return allowDrilldown;
	}

	public void setShowDrilldownRequirements(boolean drilldown) {
		this.allowDrilldown = drilldown;
	}

	public boolean getGroupByCategory() {
		return groupByCategory;
	}

	public void setGroupByCategory(boolean group) {
		this.groupByCategory = group;
	}

	public String getRepositoryPreferencePageId() {
		return repoPrefPageId;
	}

	public void setRepositoryPreferencePageId(String id) {
		this.repoPrefPageId = id;
	}

	public String getRepositoryPreferencePageName() {
		return repoPrefPageName;
	}

	public void setRepositoryPreferencePageName(String name) {
		this.repoPrefPageName = name;
	}
}
