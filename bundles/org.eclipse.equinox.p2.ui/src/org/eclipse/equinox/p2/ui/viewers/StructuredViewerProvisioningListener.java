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

package org.eclipse.equinox.p2.ui.viewers;

import java.util.EventObject;
import org.eclipse.equinox.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.engine.ProfileEvent;
import org.eclipse.equinox.p2.ui.IProvisioningListener;
import org.eclipse.equinox.p2.ui.query.IProvElementQueryProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Display;

/**
 * ProvisioningListener which updates a structured viewer based on
 * provisioning changes
 * 
 * @since 3.4
 */
public class StructuredViewerProvisioningListener implements SynchronousProvisioningListener, IProvisioningListener {

	// TODO the IProvisioningListener tag should also disappear once repo events are supported
	public static final int PROV_EVENT_REPOSITORY = 0x0001;
	public static final int PROV_EVENT_IU = 0x0002;
	public static final int PROV_EVENT_PROFILE = 0x0004;

	int eventTypes = 0;
	StructuredViewer viewer;
	Display display;
	IProvElementQueryProvider queryProvider;

	public StructuredViewerProvisioningListener(StructuredViewer viewer, int eventTypes, IProvElementQueryProvider queryProvider) {
		this.viewer = viewer;
		this.eventTypes = eventTypes;
		this.display = viewer.getControl().getDisplay();
		this.queryProvider = queryProvider;
	}

	public void notify(EventObject o) {
		if (o instanceof ProfileEvent && (((eventTypes & PROV_EVENT_IU) == PROV_EVENT_IU) || ((eventTypes & PROV_EVENT_PROFILE) == PROV_EVENT_PROFILE))) {
			ProfileEvent event = (ProfileEvent) o;
			if (event.getReason() == ProfileEvent.CHANGED) {
				final Profile profile = event.getProfile();
				display.asyncExec(new Runnable() {
					public void run() {
						// TODO
						/*
						 * Below is an optimization that is not currently working.
						 * For now we will refresh everything.
						 
						// We want to refresh the affected profile, so we
						// construct a profile element on this profile.
						// We can't match on the raw profile because the viewer
						// will set its item backpointer to the refreshed element
						ProfileElement element = new ProfileElement(profile);
						element.setQueryProvider(queryProvider);
						viewer.refresh(element);
						*/
						viewer.refresh();
					}
				});
			} else {
				display.asyncExec(new Runnable() {
					public void run() {
						viewer.refresh();
					}
				});
			}
		} else if ((o.getSource() instanceof String) && (eventTypes & PROV_EVENT_REPOSITORY) == PROV_EVENT_REPOSITORY) {
			String name = (String) o.getSource();
			if (name.equals(IProvisioningListener.REPO_ADDED) || (name.equals(IProvisioningListener.REPO_REMOVED))) {
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
