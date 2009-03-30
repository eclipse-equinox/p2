/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui;

import java.util.EventObject;
import org.eclipse.equinox.internal.p2.ui.BatchChangeBeginningEvent;
import org.eclipse.equinox.internal.p2.ui.BatchChangeCompleteEvent;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.engine.ProfileEvent;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;

/**
 * ProvisioningListener which handles event batching and other
 * extensions to the provisioning event framework that are used by
 * the UI.
 * 
 * @since 3.5
 */
public abstract class ProvUIProvisioningListener implements ProvisioningListener {

	public static final int PROV_EVENT_METADATA_REPOSITORY = 0x0001;
	public static final int PROV_EVENT_IU = 0x0002;
	public static final int PROV_EVENT_PROFILE = 0x0004;
	public static final int PROV_EVENT_ARTIFACT_REPOSITORY = 0x0008;

	int eventTypes = 0;
	int batchCount = 0;

	public ProvUIProvisioningListener(int eventTypes) {
		this.eventTypes = eventTypes;
	}

	public void notify(EventObject o) {
		if (o instanceof BatchChangeBeginningEvent) {
			batchCount++;
		} else if (o instanceof BatchChangeCompleteEvent) {
			batchCount--;
			// A batch operation completed.  Refresh if we are
			// to honor it.
			if (batchCount <= 0 && ((BatchChangeCompleteEvent) o).notify)
				refreshAll();
		} else if (batchCount > 0) {
			// We are in the middle of a batch operation
			return;
		} else if (o instanceof ProfileEvent && (((eventTypes & PROV_EVENT_IU) == PROV_EVENT_IU) || ((eventTypes & PROV_EVENT_PROFILE) == PROV_EVENT_PROFILE))) {
			ProfileEvent event = (ProfileEvent) o;
			if (event.getReason() == ProfileEvent.CHANGED) {
				profileChanged(event.getProfileId());
			} else if (event.getReason() == ProfileEvent.ADDED) {
				profileAdded(event.getProfileId());
			} else if (event.getReason() == ProfileEvent.REMOVED) {
				profileRemoved(event.getProfileId());
			}
		} else if (o instanceof RepositoryEvent) {
			RepositoryEvent event = (RepositoryEvent) o;
			// Do not handle unless this is the type of repo that we are interested in
			if ((event.getRepositoryType() == IRepository.TYPE_METADATA && (eventTypes & PROV_EVENT_METADATA_REPOSITORY) == PROV_EVENT_METADATA_REPOSITORY) || (event.getRepositoryType() == IRepository.TYPE_ARTIFACT && (eventTypes & PROV_EVENT_ARTIFACT_REPOSITORY) == PROV_EVENT_ARTIFACT_REPOSITORY)) {
				if (event.getKind() == RepositoryEvent.ADDED) {
					repositoryAdded(event);
				} else if (event.getKind() == RepositoryEvent.REMOVED) {
					repositoryRemoved(event);
				} else if (event.getKind() == RepositoryEvent.DISCOVERED) {
					repositoryDiscovered(event);
				} else if (event.getKind() == RepositoryEvent.CHANGED) {
					repositoryChanged(event);
				} else if (event.getKind() == RepositoryEvent.ENABLEMENT) {
					repositoryEnablement(event);
				}
			}
		}
	}

	/**
	 * A repository has been added.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param event the RepositoryEvent describing the details
	 */
	protected void repositoryAdded(RepositoryEvent event) {
		// Do nothing.  This method is not abstract because subclasses
		// may not be interested in repository events at all and should
		// not have to implement it.
	}

	/**
	 * A repository has been removed.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param event the RepositoryEvent describing the details
	 */
	protected void repositoryRemoved(RepositoryEvent event) {
		// Do nothing.  This method is not abstract because subclasses
		// may not be interested in repository events at all and should
		// not have to implement it.
	}

	/**
	 * A repository has been discovered.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param event the RepositoryEvent describing the details
	 */
	protected void repositoryDiscovered(RepositoryEvent event) {
		// Do nothing.  This method is not abstract because subclasses
		// may not be interested in repository events at all and should
		// not have to implement it.
	}

	/**
	 * A repository has changed.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param event the RepositoryEvent describing the details
	 */
	protected void repositoryChanged(RepositoryEvent event) {
		// Do nothing.  This method is not abstract because subclasses
		// may not be interested in repository events at all and should
		// not have to implement it.
	}

	/**
	 * A repository's enablement state has changed.  This is treated
	 * as repository addition or removal by default.  Subclasses may
	 * override.  May be called from a non-UI thread.
	 * @param event
	 */
	protected void repositoryEnablement(RepositoryEvent event) {
		// We treat enablement of a repository as if one were added.
		if (event.isRepositoryEnabled())
			repositoryAdded(event);
		else
			repositoryRemoved(event);
	}

	/**
	 * The specified profile has changed.   Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param profileId the id of the profile that changed.
	 */
	protected void profileChanged(final String profileId) {
		// Do nothing.  This method is not abstract because subclasses
		// may not be interested in profile events at all and should
		// not have to implement it.
	}

	/**
	 * The specified profile has been added.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param profileId the id of the profile that has been added.
	 */
	protected void profileAdded(final String profileId) {
		// Do nothing.  This method is not abstract because subclasses
		// may not be interested in profile events at all and should
		// not have to implement it.
	}

	/**
	 * The specified profile has been removed.  Subclasses may override.  May be called
	 * from a non-UI thread.
	 * 
	 * @param profileId the id of the profile that has been removed.
	 */
	protected void profileRemoved(final String profileId) {
		// Do nothing.  This method is not abstract because subclasses
		// may not be interested in profile events at all and should
		// not have to implement it.
	}

	/**
	 * An event requiring a complete refresh of the listener's state has
	 * been received.  This is used, for example, when a batch change has
	 * completed.  Subclasses may override.  May be called from a non-UI
	 * thread.
	 */
	protected void refreshAll() {
		// Do nothing by default.
	}

	public int getEventTypes() {
		return eventTypes;
	}
}
