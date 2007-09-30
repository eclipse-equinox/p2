/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.prov.ui.viewers;

import java.util.EventObject;
import org.eclipse.equinox.prov.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.prov.engine.*;
import org.eclipse.equinox.prov.ui.IProvisioningProperties;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Display;

/**
 * ProvisioningListener which updates a structured viewer based on
 * provisioning changes
 * 
 * @since 3.4
 */
public class StructuredViewerProvisioningListener implements SynchronousProvisioningListener, IPropertyChangeListener {

	// TODO this should be replaced with actual event topic ids from the event API once they are defined
	// TODO the IPropertyChangeListener implementation should also disappear once repo events are supported
	public static final int PROV_EVENT_REPOSITORY = 0x0001;
	public static final int PROV_EVENT_IU = 0x0002;
	public static final int PROV_EVENT_PROFILE = 0x0004;

	int eventTypes = 0;
	StructuredViewer viewer;
	Display display;

	public StructuredViewerProvisioningListener(StructuredViewer viewer, int eventTypes) {
		this.viewer = viewer;
		this.eventTypes = eventTypes;
		this.display = viewer.getControl().getDisplay();
	}

	public void notify(EventObject o) {
		// Commit operations on a profile will refresh the structure of the profile
		if (o instanceof CommitOperationEvent && (((eventTypes & PROV_EVENT_PROFILE) == PROV_EVENT_PROFILE) || (eventTypes & PROV_EVENT_IU) == PROV_EVENT_IU)) {
			CommitOperationEvent event = (CommitOperationEvent) o;
			final Profile profile = event.getProfile();
			display.asyncExec(new Runnable() {
				public void run() {
					viewer.refresh(profile);

				}
			});
		} else if (o instanceof ProfileEvent && ((eventTypes & PROV_EVENT_IU) == PROV_EVENT_IU)) {
			// We assume for now that it was either an add or remove of a
			// profile, so rather than update a profile, we update everything.
			display.asyncExec(new Runnable() {
				public void run() {
					viewer.refresh();
				}
			});
		}
	}

	public void propertyChange(final PropertyChangeEvent event) {
		// Currently we only support repo events
		if ((eventTypes & PROV_EVENT_REPOSITORY) == PROV_EVENT_REPOSITORY) {
			String property = event.getProperty();
			if (property.equals(IProvisioningProperties.REPO_NAME)) {
				display.asyncExec(new Runnable() {
					public void run() {
						viewer.update(event.getSource(), null);
					}
				});
			} else if (property.equals(IProvisioningProperties.REPO_ADDED) || (property.equals(IProvisioningProperties.REPO_REMOVED))) {
				display.asyncExec(new Runnable() {
					public void run() {
						viewer.refresh();
					}
				});
			}
		}

	}

	// TODO temporary for use by ProvUIActivator
	public int getEventTypes() {
		return eventTypes;
	}
}
