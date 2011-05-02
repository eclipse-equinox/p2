/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * RepositoryManipulatorDropTarget recognizes both URLTransfer and
 * FileTransfer data types.  Files are converted to URL's with the file
 * protocol.  Any dropped URLs (or Files) are interpreted to mean that the
 * user wishes to add these files as repositories.
 * 
 * @since 3.4
 *
 */
public class RepositoryManipulatorDropTarget extends URLDropAdapter {
	ProvisioningUI ui;
	RepositoryTracker tracker;
	Control control;

	public RepositoryManipulatorDropTarget(ProvisioningUI ui, Control control) {
		super(true); // convert file drops to URL
		Assert.isNotNull(ui);
		this.ui = ui;
		this.tracker = ui.getRepositoryTracker();
		this.control = control;
	}

	protected void handleDrop(String urlText, final DropTargetEvent event) {
		event.detail = DND.DROP_NONE;
		final URI[] location = new URI[1];
		try {
			location[0] = URIUtil.fromString(urlText);
		} catch (URISyntaxException e) {
			ProvUI.reportStatus(tracker.getInvalidLocationStatus(urlText), StatusManager.SHOW | StatusManager.LOG);
			return;
		}
		if (location[0] == null)
			return;

		Job job = new WorkbenchJob(ProvUIMessages.RepositoryManipulatorDropTarget_DragAndDropJobLabel) {

			public IStatus runInUIThread(IProgressMonitor monitor) {
				IStatus status = tracker.validateRepositoryLocation(ui.getSession(), location[0], false, monitor);
				if (status.isOK()) {
					tracker.addRepository(location[0], null, ui.getSession());
					event.detail = DND.DROP_LINK;
				} else if (status.getSeverity() == IStatus.CANCEL) {
					event.detail = DND.DROP_NONE;
				} else {
					status = new MultiStatus(ProvUIActivator.PLUGIN_ID, 0, new IStatus[] {status}, NLS.bind(ProvUIMessages.RepositoryManipulatorDropTarget_DragSourceNotValid, URIUtil.toUnencodedString(location[0])), null);
					event.detail = DND.DROP_NONE;
				}
				return status;
			}
		};
		job.setPriority(Job.SHORT);
		job.setUser(true);
		job.schedule();
	}
}