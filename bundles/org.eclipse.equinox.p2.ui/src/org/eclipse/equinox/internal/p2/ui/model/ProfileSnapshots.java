/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

/**
 * Element class for profile snapshots
 *
 * @since 3.5
 */
public class ProfileSnapshots extends ProvElement implements IDeferredWorkbenchAdapter {

	String profileId;
	ProvisioningSession session;

	public ProfileSnapshots(String profileId, ProvisioningSession session) {
		super(null);
		this.profileId = profileId;
		this.session = session;
	}

	public String getProfileId() {
		return profileId;
	}

	@Override
	public Object[] getChildren(Object o) {
		IProfileRegistry registry = ProvUI.getProfileRegistry(session);
		long[] timestamps = registry.listProfileTimestamps(profileId);

		// find out which profile states we should hide
		Map<String, String> hidden = registry.getProfileStateProperties(profileId, IProfile.STATE_PROP_HIDDEN);
		Map<String, String> tag = registry.getProfileStateProperties(profileId, IProfile.STATE_PROP_TAG);
		List<RollbackProfileElement> elements = new ArrayList<>();

		for (int i = 0; i < timestamps.length; i++) {
			if (hidden.containsKey(String.valueOf(timestamps[i]))) {
				continue;
			}
			RollbackProfileElement element = null;
			String timestamp = String.valueOf(timestamps[i]);
			if (!tag.containsKey(timestamp)) {
				element = new RollbackProfileElement(this, profileId, timestamps[i]);
			} else {
				element = new RollbackProfileElement(this, profileId, timestamps[i], tag.get(timestamp));
			}
			elements.add(element);

			// Eliminate the first in the list (earliest) if there was no content at all.
			// This doesn't always happen, but can, and we don't want to offer the user an
			// empty profile to
			// revert to. Just reset the list since it only has one element.
			if (i == 0 && element.getChildren(element).length == 0) {
				elements.clear();
			}
		}
		// current profile is the last one in the list
		if (elements.size() > 0) {
			elements.get(elements.size() - 1).setIsCurrentProfile(true);
		}
		return elements.toArray(new RollbackProfileElement[elements.size()]);
	}

	@Override
	public String getLabel(Object o) {
		return ProvUIMessages.ProfileSnapshots_Label;
	}

	@Override
	public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
		Object[] children = getChildren(object);
		collector.add(children, monitor);
	}

	@Override
	public ISchedulingRule getRule(Object object) {
		return null;
	}

	@Override
	public boolean isContainer() {
		return false;
	}
}
