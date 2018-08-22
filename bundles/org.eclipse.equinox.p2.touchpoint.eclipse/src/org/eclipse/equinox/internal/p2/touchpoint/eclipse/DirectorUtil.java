/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;

public class DirectorUtil {

	public static IStatus validateProfile(IProfile profile) {
		IPlanner planner = (IPlanner) profile.getProvisioningAgent().getService(IPlanner.SERVICE_NAME);
		IProfileChangeRequest pcr = planner.createChangeRequest(profile);
		ProvisioningContext ctx = new ProvisioningContext(profile.getProvisioningAgent());
		ctx.setMetadataRepositories(new URI[0]);
		return planner.getProvisioningPlan(pcr, ctx, null).getStatus();
	}
}
