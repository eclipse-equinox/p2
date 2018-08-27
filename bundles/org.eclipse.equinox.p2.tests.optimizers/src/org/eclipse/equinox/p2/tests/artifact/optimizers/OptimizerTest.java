/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.optimizers;

import org.eclipse.equinox.internal.p2.artifact.optimizers.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;

public abstract class OptimizerTest {

	protected static IProvisioningAgent getAgent() {
		// get the global agent for the currently running system
		return ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.class);
	}

}
