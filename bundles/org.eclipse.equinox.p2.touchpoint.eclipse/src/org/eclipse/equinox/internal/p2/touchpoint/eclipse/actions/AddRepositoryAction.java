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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Activator;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

/**
 * An action that adds a repository to the list of known repositories.
 */
public class AddRepositoryAction extends ProvisioningAction {
	public static final String ID = "addRepository"; //$NON-NLS-1$

	static RepositoryEvent createEvent(Map parameters) throws CoreException {
		String parm = (String) parameters.get(ActionConstants.PARM_REPOSITORY_LOCATION);
		if (parm == null)
			throw new CoreException(Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_REPOSITORY_LOCATION, ID)));
		URI location = null;
		try {
			location = new URI(parm);
		} catch (URISyntaxException e) {
			throw new CoreException(Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_REPOSITORY_LOCATION, ID), e));
		}
		parm = (String) parameters.get(ActionConstants.PARM_REPOSITORY_TYPE);
		if (parm == null)
			throw new CoreException(Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_REPOSITORY_TYPE, ID)));
		int type = 0;
		try {
			type = Integer.parseInt(parm);
		} catch (NumberFormatException e) {
			throw new CoreException(Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_REPOSITORY_TYPE, ID), e));
		}
		//default is to be enabled
		String enablement = (String) parameters.get(ActionConstants.PARM_REPOSITORY_ENABLEMENT);
		boolean enabled = enablement == null ? true : Boolean.valueOf(enablement).booleanValue();
		return new RepositoryEvent(location, type, RepositoryEvent.DISCOVERED, enabled);
	}

	public IStatus execute(Map parameters) {
		IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(Activator.getContext(), IProvisioningEventBus.SERVICE_NAME);
		try {
			if (bus != null)
				bus.publishEvent(createEvent(parameters));
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return e.getStatus();
		}
	}

	public IStatus undo(Map parameters) {
		//TODO: we don't know if the repository was already present
		return Status.OK_STATUS;
	}
}
