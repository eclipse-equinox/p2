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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Activator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;

/**
 * An action that adds a repository to the list of known repositories.
 */
public class RemoveRepositoryAction extends ProvisioningAction {
	public static final String ID = "removeRepository"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		try {
			RepositoryEvent event = AddRepositoryAction.createEvent(parameters);
			if (event.getRepositoryType() == IRepository.TYPE_METADATA) {
				IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
				if (manager != null)
					manager.removeRepository(event.getRepositoryLocation());
			} else if (event.getRepositoryType() == IRepository.TYPE_ARTIFACT) {
				IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.SERVICE_NAME);
				if (manager != null)
					manager.removeRepository(event.getRepositoryLocation());
			}
		} catch (CoreException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(Activator.getContext(), IProvisioningEventBus.SERVICE_NAME);
		try {
			if (bus != null)
				bus.publishEvent(AddRepositoryAction.createEvent(parameters));
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return e.getStatus();
		}
	}
}
