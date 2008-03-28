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

package org.eclipse.equinox.internal.provisional.p2.ui.viewers;

import java.util.EventObject;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.engine.ProfileEvent;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * ProvisioningListener which updates a structured viewer based on
 * provisioning changes
 * 
 * @since 3.4
 */
public class StructuredViewerProvisioningListener implements SynchronousProvisioningListener {

	public static final int PROV_EVENT_METADATA_REPOSITORY = 0x0001;
	public static final int PROV_EVENT_IU = 0x0002;
	public static final int PROV_EVENT_PROFILE = 0x0004;
	public static final int PROV_EVENT_ARTIFACT_REPOSITORY = 0x0008;

	int eventTypes = 0;
	StructuredViewer viewer;
	Display display;
	IQueryProvider queryProvider;

	public StructuredViewerProvisioningListener(StructuredViewer viewer, int eventTypes, IQueryProvider queryProvider) {
		this.viewer = viewer;
		this.eventTypes = eventTypes;
		this.display = viewer.getControl().getDisplay();
		this.queryProvider = queryProvider;
	}

	public void notify(EventObject o) {
		if (o instanceof ProfileEvent && (((eventTypes & PROV_EVENT_IU) == PROV_EVENT_IU) || ((eventTypes & PROV_EVENT_PROFILE) == PROV_EVENT_PROFILE))) {
			ProfileEvent event = (ProfileEvent) o;
			if (event.getReason() == ProfileEvent.CHANGED) {
				final String profileId = event.getProfileId();
				display.asyncExec(new Runnable() {
					public void run() {
						if (viewer.getControl().isDisposed())
							return;
						// We want to refresh the affected profile, so we
						// construct a profile element on this profile.
						ProfileElement element = new ProfileElement(profileId);
						element.setQueryProvider(queryProvider);
						viewer.refresh(element);
					}
				});
			} else {
				display.asyncExec(new Runnable() {
					public void run() {
						if (viewer.getControl().isDisposed())
							return;
						refreshAll();
					}
				});
			}
		} else if (o instanceof RepositoryEvent) {
			RepositoryEvent event = (RepositoryEvent) o;
			// Do not refresh unless this is the type of repo that we are interested in
			if ((event.getRepositoryType() == IRepository.TYPE_METADATA && (eventTypes & PROV_EVENT_METADATA_REPOSITORY) == PROV_EVENT_METADATA_REPOSITORY) || (event.getRepositoryType() == IRepository.TYPE_ARTIFACT && (eventTypes & PROV_EVENT_ARTIFACT_REPOSITORY) == PROV_EVENT_ARTIFACT_REPOSITORY)) {
				if (event.getKind() == RepositoryEvent.ADDED || event.getKind() == RepositoryEvent.REMOVED) {
					display.asyncExec(new Runnable() {
						public void run() {
							IWorkbench workbench = PlatformUI.getWorkbench();
							if (workbench.isClosing())
								return;
							Control control = viewer.getControl();
							if (control != null && !control.isDisposed())
								refreshAll();
						}
					});
				}
			}
		}

	}

	/**
	 * Refresh the entire structure of the viewer.  Subclasses may
	 * override to ensure that any caching done in content providers or
	 * model elements is refreshed before the viewer is refreshed.
	 */
	protected void refreshAll() {
		viewer.refresh();
	}

	public int getEventTypes() {
		return eventTypes;
	}
}
