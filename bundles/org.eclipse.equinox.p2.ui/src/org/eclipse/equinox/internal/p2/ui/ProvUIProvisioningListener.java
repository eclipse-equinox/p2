/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui;

import java.util.EventObject;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.engine.IProfileEvent;
import org.eclipse.equinox.p2.repository.IRepository;

/**
 * ProvisioningListener which handles event batching and other
 * extensions to the provisioning event framework that are used by
 * the UI.
 * 
 * @since 3.5
 */
public abstract class ProvUIProvisioningListener implements SynchronousProvisioningListener {

	public static final int PROV_EVENT_METADATA_REPOSITORY = 0x0001;
	public static final int PROV_EVENT_IU = 0x0002;
	public static final int PROV_EVENT_PROFILE = 0x0004;
	public static final int PROV_EVENT_ARTIFACT_REPOSITORY = 0x0008;

	int eventTypes = 0;
	String name;
	private ProvisioningOperationRunner runner;

	public ProvUIProvisioningListener(String name, int eventTypes, ProvisioningOperationRunner runner) {
		this.name = name;
		this.eventTypes = eventTypes;
		this.runner = runner;
	}

	public void notify(EventObject o) {
		if (o instanceof RepositoryOperationBeginningEvent) {
			if (Tracing.DEBUG_EVENTS_CLIENT)
				Tracing.debug("Batch Eventing:  Ignore Following Events. " + getReceiverString()); //$NON-NLS-1$
		} else if (o instanceof RepositoryOperationEndingEvent) {
			RepositoryOperationEndingEvent event = (RepositoryOperationEndingEvent) o;

			if (Tracing.DEBUG_EVENTS_CLIENT)
				Tracing.debug("Batch Eventing:  Batch Ended. " + getReceiverString()); //$NON-NLS-1$
			// A batch operation completed.  Refresh.
			if (runner.eventBatchCount <= 0) {
				if (Tracing.DEBUG_EVENTS_CLIENT)
					Tracing.debug("Batch Eventing Complete." + getReceiverString()); //$NON-NLS-1$
				if (event.getEvent() == null && event.update()) {
					if (Tracing.DEBUG_EVENTS_CLIENT) {
						Tracing.debug("Refreshing After Batch." + getReceiverString()); //$NON-NLS-1$
					}
					refreshAll();
				} else if (event.update()) {
					if (Tracing.DEBUG_EVENTS_CLIENT)
						Tracing.debug("Dispatching Last Event in Batch." + getReceiverString()); //$NON-NLS-1$
					notify(event.getEvent());
				} else if (Tracing.DEBUG_EVENTS_CLIENT) {
					Tracing.debug("No Refresh on Batch Complete."); //$NON-NLS-1$
				}
			} else {
				// We are still in the middle of a batch operation, but we've been notified
				// about a nested batch that ended.  See if it ended with a specific event.  
				// If it did, this means there was a user action involving a repository 
				// (rather than side-effect events).  For example, the user might add a repo while a full 
				// background load is running.  We want to honor that
				// event.  See https://bugs.eclipse.org/bugs/show_bug.cgi?id=305478
				RepositoryEvent innerEvent = event.getEvent();
				if (innerEvent != null) {
					handleRepositoryEvent(innerEvent);
				}
			}
		} else if (runner.eventBatchCount > 0) {
			// ignore raw events during a batch
			if (Tracing.DEBUG_EVENTS_CLIENT)
				Tracing.debug(name + " Ignoring: " + o.toString()); //$NON-NLS-1$
			return;
		} else if (o instanceof IProfileEvent && (((eventTypes & PROV_EVENT_IU) == PROV_EVENT_IU) || ((eventTypes & PROV_EVENT_PROFILE) == PROV_EVENT_PROFILE))) {
			if (Tracing.DEBUG_EVENTS_CLIENT)
				Tracing.debug(o.toString() + getReceiverString());
			IProfileEvent event = (IProfileEvent) o;
			if (event.getReason() == IProfileEvent.CHANGED) {
				profileChanged(event.getProfileId());
			} else if (event.getReason() == IProfileEvent.ADDED) {
				profileAdded(event.getProfileId());
			} else if (event.getReason() == IProfileEvent.REMOVED) {
				profileRemoved(event.getProfileId());
			}
		} else if (o instanceof RepositoryEvent) {
			if (Tracing.DEBUG_EVENTS_CLIENT)
				Tracing.debug(o.toString() + getReceiverString());
			handleRepositoryEvent((RepositoryEvent) o);
		}
	}

	private String getReceiverString() {
		return " --  <" + name + "> "; //$NON-NLS-1$//$NON-NLS-2$
	}

	private void handleRepositoryEvent(RepositoryEvent event) {
		// Do not handle unless this is the type of repo that we are interested in
		if ((event.getRepositoryType() == IRepository.TYPE_METADATA && (eventTypes & PROV_EVENT_METADATA_REPOSITORY) == PROV_EVENT_METADATA_REPOSITORY) || (event.getRepositoryType() == IRepository.TYPE_ARTIFACT && (eventTypes & PROV_EVENT_ARTIFACT_REPOSITORY) == PROV_EVENT_ARTIFACT_REPOSITORY)) {
			if (event.getKind() == RepositoryEvent.ADDED && event.isRepositoryEnabled()) {
				repositoryAdded(event);
			} else if (event.getKind() == RepositoryEvent.REMOVED && event.isRepositoryEnabled()) {
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
