/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.pack200;

import org.eclipse.osgi.util.NLS;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 * 
 * @deprecated See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a> for details.
 */
@Deprecated(forRemoval = true, since = "1.2.0")
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.artifact.optimizers.pack200.messages"; //$NON-NLS-1$
	public static String failed_getting_artifact;
	public static String skip_optimization;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}
}
