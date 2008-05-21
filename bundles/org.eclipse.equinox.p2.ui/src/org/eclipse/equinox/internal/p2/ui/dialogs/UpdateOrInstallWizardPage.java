/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.Sizing;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public abstract class UpdateOrInstallWizardPage extends ProfileModificationWizardPage {

	protected UpdateOrInstallWizard wizard;
	// private static final int DEFAULT_COLUMN_WIDTH = 150;
	protected Label sizeInfo;
	protected Sizing sizing;
	private Job sizingJob;

	protected UpdateOrInstallWizardPage(String id, IInstallableUnit[] ius, String profileId, ProvisioningPlan plan, UpdateOrInstallWizard wizard) {
		super(id, ius, profileId, plan);
		computeSizing(plan, profileId);
		this.wizard = wizard;
	}

	// This method is removed to improve performance
	// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=221087
	/*
	protected IUColumnConfig[] getColumnConfig() {
		initializeDialogUnits(getShell());
		int pixels = convertHorizontalDLUsToPixels(DEFAULT_COLUMN_WIDTH);
		return new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, pixels), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, pixels), new IUColumnConfig(ProvUIMessages.ProvUI_SizeColumnTitle, IUColumnConfig.COLUMN_SIZE, pixels / 2)};
	}
	*/

	protected void computeSizing(final ProvisioningPlan plan, final String profileId) {
		sizing = null;
		if (sizeInfo != null)
			if (!getShell().isDisposed()) {
				getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						updateSizingInfo();
					}

				});
			}
		if (sizingJob != null)
			sizingJob.cancel();
		sizingJob = new Job("Computing size") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					sizing = ProvisioningUtil.getSizeInfo(plan, profileId, monitor);
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

	protected void checkedIUsChanged() {
		// First ensure that a new plan is computed.
		super.checkedIUsChanged();
		// Now update the license page accordingly.  This requires the plan so
		// that licenses for required items can also be checked.
		wizard.planChanged(elementsToIUs(getCheckedElements()), currentPlan);
		// status of license page could change status of wizard next button
		getContainer().updateButtons();

	}

	protected void updateSizingInfo() {
		long size = IUElement.SIZE_UNAVAILABLE;
		if (sizing != null) {
			size = sizing.getDiskSize();
			if (size == 0)
				size = IUElement.SIZE_UNAVAILABLE;
		}
		if (sizeInfo != null && !sizeInfo.isDisposed())
			sizeInfo.setText(NLS.bind(ProvUIMessages.UpdateOrInstallWizardPage_Size, getFormattedSize(size)));
	}

	protected String getFormattedSize(long size) {
		if (size == IUElement.SIZE_UNKNOWN || size == IUElement.SIZE_UNAVAILABLE)
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
}
