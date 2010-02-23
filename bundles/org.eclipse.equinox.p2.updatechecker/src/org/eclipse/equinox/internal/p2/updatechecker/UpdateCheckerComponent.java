/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public Object createService(IProvisioningAgent agent) {
		return new UpdateChecker(agent);
	}

}
