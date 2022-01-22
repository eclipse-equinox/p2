/*******************************************************************************
 * Copyright (c) 2022 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import org.eclipse.equinox.internal.provisional.p2.repository.DefaultPGPPublicKeyService;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;

public class PGPKeyServiceComponent implements IAgentServiceFactory {
	@Override
	public Object createService(IProvisioningAgent agent) {
		return new DefaultPGPPublicKeyService(agent);
	}
}
