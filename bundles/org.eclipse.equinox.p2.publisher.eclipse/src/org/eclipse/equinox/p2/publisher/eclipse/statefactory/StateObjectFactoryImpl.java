/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
package org.eclipse.equinox.p2.publisher.eclipse.statefactory;

import java.util.Dictionary;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.BundleException;

public class StateObjectFactoryImpl {
	public BundleDescription createBundleDescription(State xxx, Dictionary<String, String> manifest, String location,
			long xxxx) throws BundleException {
		BundleDescriptionImpl result = StateBuilder.createBundleDescription(null, manifest, location);
		result.setBundleId(1);
		return result;
	}
}
