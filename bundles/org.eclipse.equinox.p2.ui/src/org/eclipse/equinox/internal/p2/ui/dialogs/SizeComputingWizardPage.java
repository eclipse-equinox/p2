/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.text.NumberFormat;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * 
 * @since 3.5
 */
public abstract class SizeComputingWizardPage extends ResolutionResultsWizardPage {
	protected Label sizeInfo;
	protected long size;
	Job sizingJob;
	private IProvisioningPlan lastComputedPlan = null;

	protected SizeComputingWizardPage(ProvisioningUI ui, ProvisioningOperationWizard wizard, IUElementListRoot root, ProfileChangeOperation initialResolution) {
		super(ui, wizard, root, initialResolution);
		// Compute size immediately if a plan is available.  This may or may not finish before
		// the widgetry is created.
		if (initialResolution != null && initialResolution.hasResolved())
			computeSizing(initialResolution.getProvisioningPlan(), initialResolution.getProvisioningContext());
		else
			// Set the size to indicate there is no size yet.
			size = ProvUI.SIZE_NOTAPPLICABLE;
	}

	protected void computeSizing(final IProvisioningPlan plan, final ProvisioningContext provisioningContext) {
		if (plan == lastComputedPlan)
			return;
		lastComputedPlan = plan;
		size = ProvUI.SIZE_UNKNOWN;
		updateSizingInfo();
		if (sizingJob != null)
			sizingJob.cancel();
		sizingJob = new Job(ProvUIMessages.SizeComputingWizardPage_SizeJobTitle) {
			protected IStatus run(IProgressMonitor monitor) {
				size = ProvUI.getSize(ProvUI.getEngine(getProvisioningUI().getSession()), plan, provisioningContext, monitor);
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				if (display != null) {
					display.asyncExec(new Runnable() {
						public void run() {
							updateSizingInfo();
						}
					});
				}
				return Status.OK_STATUS;
			}

		};
		sizingJob.setUser(false);
		sizingJob.setSystem(true);
		sizingJob.setProperty(LoadMetadataRepositoryJob.SUPPRESS_AUTHENTICATION_JOB_MARKER, Boolean.toString(true));
		sizingJob.schedule();
	}

	protected void createSizingInfo(Composite parent) {
		sizeInfo = new Label(parent, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		sizeInfo.setLayoutData(data);
		updateSizingInfo();
	}

	protected void updateSizingInfo() {
		if (sizeInfo != null && !sizeInfo.isDisposed()) {
			if (size == ProvUI.SIZE_NOTAPPLICABLE)
				sizeInfo.setVisible(false);
			else {
				sizeInfo.setText(NLS.bind(ProvUIMessages.UpdateOrInstallWizardPage_Size, getFormattedSize()));
				sizeInfo.setVisible(true);
			}
		}
	}

	protected String getFormattedSize() {
		if (size == ProvUI.SIZE_UNKNOWN || size == ProvUI.SIZE_UNAVAILABLE)
			return ProvUIMessages.IUDetailsLabelProvider_Unknown;
		if (size > 1000L) {
			long kb = size / 1000L;
			return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_KB, NumberFormat.getInstance().format(new Long(kb)));
		}
		return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_Bytes, NumberFormat.getInstance().format(new Long(size)));
	}

	public void dispose() {
		if (sizingJob != null) {
			sizingJob.cancel();
			sizingJob = null;
		}
	}

	public void updateStatus(IUElementListRoot root, ProfileChangeOperation op) {
		super.updateStatus(root, op);
		if (op != null && op.getProvisioningPlan() != null)
			computeSizing(op.getProvisioningPlan(), op.getProvisioningContext());
	}

	protected IQueryable<IInstallableUnit> getQueryable(IProvisioningPlan plan) {
		return plan.getAdditions();
	}
}
