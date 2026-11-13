/****************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.util;

import java.util.Arrays;
import java.util.List;
import org.osgi.framework.*;

/**
 * @since 3.9
 */
public class BundleStarter {

	public static void startDependents(BundleContext context, String[] bundleSymbolicNames, int stateMask)
			throws BundleException {
		List<String> bsns = Arrays.asList(bundleSymbolicNames);
		for (Bundle b : context.getBundles())
			if (bsns.contains(b.getSymbolicName()) && ((b.getState() & stateMask) != 0))
				b.start();
	}
}
