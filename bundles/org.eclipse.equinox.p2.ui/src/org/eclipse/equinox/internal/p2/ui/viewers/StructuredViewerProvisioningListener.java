/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.viewers;

import org.eclipse.equinox.internal.p2.ui.ProvUIProvisioningListener;
import org.eclipse.equinox.internal.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * ProvisioningListener which updates a structured viewer based on
 * provisioning changes.  Provides default behavior which refreshes particular
 * model elements or the entire viewer based on the nature of the change and the
 * changes that the client is interested in.  Subclasses typically only need
 * override when there is additional, specialized behavior required.
 * 
 * @since 3.4
 */
public class StructuredViewerProvisioningListener extends ProvUIProvisioningListener {

	StructuredViewer viewer;
	Display display;

	public StructuredViewerProvisioningListener(String name, StructuredViewer viewer, int eventTypes, ProvisioningOperationRunner runner) {
		super(name, eventTypes, runner);
		this.viewer = viewer;
		this.display = viewer.getControl().getDisplay();
	}

	/**
	 * A repository has been added.  The default behavior is to
	 * refresh the viewer.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param event the RepositoryEvent describing the details
	 */
	protected void repositoryAdded(RepositoryEvent event) {
		safeRefresh();
	}

	/**
	 * A repository has been removed.  The default behavior is to
	 * refresh the viewer.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param event the RepositoryEvent describing the details
	 */
	protected void repositoryRemoved(RepositoryEvent event) {
		safeRefresh();
	}

	/**
	 * A repository has been discovered.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param event the RepositoryEvent describing the details
	 */
	protected void repositoryDiscovered(RepositoryEvent event) {
		// Do nothing for now
	}

	/**
	 * A repository has changed.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param event the RepositoryEvent describing the details
	 */
	protected void repositoryChanged(RepositoryEvent event) {
		// Do nothing for now.  When this event is actually used in
		// the core, we may want to refresh particular elements the way
		// we currently refresh a profile element.
	}

	/**
	 * The specified profile has changed.  The default behavior is to refresh the viewer
	 * with a profile element of the matching id.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param profileId the id of the profile that changed.
	 */
	protected void profileChanged(final String profileId) {
		display.asyncExec(new Runnable() {
			public void run() {
				if (isClosing())
					return;
				// We want to refresh the affected profile, so we
				// construct a profile element on this profile.
				ProfileElement element = new ProfileElement(null, profileId);
				viewer.refresh(element);
			}
		});
	}

	/**
	 * The specified profile has been added.  The default behavior is to fully
	 * refresh the associated viewer. Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param profileId the id of the profile that has been added.
	 */
	protected void profileAdded(final String profileId) {
		safeRefresh();
	}

	/**
	 * The specified profile has been removed.  The default behavior is to fully
	 * refresh the associated viewer. Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param profileId the id of the profile that has been removed.
	 */
	protected void profileRemoved(final String profileId) {
		safeRefresh();
	}

	protected void safeRefresh() {
		if (Display.getCurrent() != null) {
			refreshViewer();
			return;
		}

		display.asyncExec(new Runnable() {
			public void run() {
				if (isClosing())
					return;
				refreshViewer();
			}
		});
	}

	protected void refreshAll() {
		safeRefresh();
	}

	/**
	 * Refresh the entire structure of the viewer.  Subclasses may
	 * override to ensure that any caching done in content providers or
	 * model elements is refreshed before the viewer is refreshed.  This will 
	 * always be called from the UI thread.
	 */
	protected void refreshViewer() {
		viewer.refresh();
	}

	/**
	 * Return whether the viewer is closing or shutting down.
	 * This method should be used in async execs to ensure that
	 * the viewer is still alive.
	 * @return a boolean indicating whether the viewer is closing
	 */
	protected boolean isClosing() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench.isClosing())
			return true;

		if (viewer.getControl().isDisposed())
			return true;

		return false;
	}
}
