/*******************************************************************************
 * Copyright (c) 2011 EclipseSource Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public Object createService(IProvisioningAgent agent) {
		ProvisioningUI ui = (ProvisioningUI) agent.getService(ProvisioningUI.class.getName());
		if (ui == null)
			return null;
		return new ColocatedRepositoryTracker(ui);
	}
}
