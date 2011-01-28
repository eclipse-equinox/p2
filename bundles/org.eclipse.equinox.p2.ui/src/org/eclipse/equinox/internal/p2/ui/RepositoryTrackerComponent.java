/*******************************************************************************
 * Copyright (c) 2011 EclipseSource Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     EclipseSource Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Component that provides a factory that can create and initialize
 * {@link RepositoryTracker} instances.
 */
public class RepositoryTrackerComponent implements IAgentServiceFactory {

	public Object createService(IProvisioningAgent agent) {
		ProvisioningUI ui = (ProvisioningUI) agent.getService(ProvisioningUI.class.getName());
		if (ui == null)
			return null;
		return new ColocatedRepositoryTracker(ui);
	}
}
