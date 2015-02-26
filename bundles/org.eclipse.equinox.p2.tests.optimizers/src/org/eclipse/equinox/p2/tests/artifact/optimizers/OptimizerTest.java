/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.optimizers;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.artifact.optimizers.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;

public abstract class OptimizerTest extends TestCase {
	public OptimizerTest(String name) {
		super(name);
	}

	public OptimizerTest() {
		super();
	}

	protected static IProvisioningAgent getAgent() {
		//get the global agent for the currently running system
		return ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.class);
	}

}
