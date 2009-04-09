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
package org.eclipse.equinox.p2.tests.ui.dialogs;

import java.net.URI;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * Tests for the install wizard
 */
public abstract class WizardTest extends AbstractProvisioningUITest {
	protected PlannerResolutionOperation getResolvedOperation(ProfileChangeRequest request) throws ProvisionException {
		PlannerResolutionOperation op = new PlannerResolutionOperation("Test resolve operation", request.getProfile().getProfileId(), request, new ProvisioningContext(new URI[] {}), new MultiStatus(ProvUIActivator.PLUGIN_ID, 0, "This is just a test multistatus", null), true);
		op.execute(getMonitor());
		return op;
	}
}
