/*******************************************************************************
 * Copyright (c) 2015, 2018 Mykola Nikishov.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mykola Nikishov - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.artifact.processors.checksum;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.artifact.processors.checksum.messages"; //$NON-NLS-1$

	public static String Error_invalid_checksum;
	public static String Error_checksum_unavailable;
	public static String Error_unexpected_checksum;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//empty
	}
}
