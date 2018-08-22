/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.updatechecker;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;

/**
 * Component for instantiating update checker service instances.
 */
public class UpdateCheckerComponent implements IAgentServiceFactory {
	public static final String BUNDLE_ID = "org.eclipse.equinox.p2.updatechecker"; //$NON-NLS-1$

	@Override
	public Object createService(IProvisioningAgent agent) {
		return new UpdateChecker(agent);
	}

}
