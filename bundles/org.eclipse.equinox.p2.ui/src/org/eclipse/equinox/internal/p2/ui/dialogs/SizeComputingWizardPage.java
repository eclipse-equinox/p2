/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.text.NumberFormat;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * 
 * @since 3.5
 */
public abstract class SizeComputingWizardPage extends ResolutionWizardPage {

	protected SizeComputingWizardPage(Policy policy, IUElementListRoot root, String profileID, PlannerResolutionOperation initialResolution) {
		super(policy, root, profileID, initialResolution);
	}

	protected Label sizeInfo;
	protected long size;
	Job sizingJob;

	protected void computeSizing(final ProvisioningPlan plan, final String profileId) {
		size = IIUElement.SIZE_UNKNOWN;
		if (sizingJob != null)
			sizingJob.cancel();
		sizingJob = new Job(ProvUIMessages.SizeComputingWizardPage_SizeJobTitle) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					size = ProvisioningUtil.getSize(plan, profileId, monitor);
				} catch (ProvisionException e) {
					return e.getStatus();
				}
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
		sizingJob.schedule();
		sizingJob.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				sizingJob = null;
			}
		});
	}

	protected void createSizingInfo(Composite parent) {
		sizeInfo = new Label(parent, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		sizeInfo.setLayoutData(data);
		updateSizingInfo();
	}

	protected void updateSizingInfo() {
		if (sizeInfo != null && !sizeInfo.isDisposed()) {
			if (size == IIUElement.SIZE_NOTAPPLICABLE)
				sizeInfo.setVisible(false);
			else {
				sizeInfo.setText(NLS.bind(ProvUIMessages.UpdateOrInstallWizardPage_Size, getFormattedSize()));
				sizeInfo.setVisible(true);
			}
		}
	}

	protected String getFormattedSize() {
		if (size == IIUElement.SIZE_UNKNOWN || size == IIUElement.SIZE_UNAVAILABLE)
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

	public void recomputePlan(IUElementListRoot root) {
		super.recomputePlan(root);
		if (getCurrentPlan() != null)
			computeSizing(getCurrentPlan(), getProfileId());
	}

	protected IQueryable getQueryable(ProvisioningPlan plan) {
		return plan.getAdditions();
	}
}
