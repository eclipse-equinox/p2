/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.pgp;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.artifact.processors.pgp.messages"; //$NON-NLS-1$

	public static String Error_SignatureAndFileDontMatch;

	public static String Error_CouldNotLoadSignature;

	public static String Error_publicKeyNotFound;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//empty
	}
}
