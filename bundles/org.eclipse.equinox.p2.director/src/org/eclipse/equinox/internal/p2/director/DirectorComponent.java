/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.planner.IPlanner;

public class DirectorComponent implements IAgentServiceFactory {

	@Override
	public Object createService(IProvisioningAgent agent) {
		IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		return new SimpleDirector(engine, planner);
	}

}
