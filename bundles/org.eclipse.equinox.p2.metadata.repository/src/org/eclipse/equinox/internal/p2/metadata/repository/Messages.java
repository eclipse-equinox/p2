/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 * Sonatype Inc - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.metadata.repository.messages"; //$NON-NLS-1$

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

	public static String io_failedRead;
	public static String io_failedWrite;

	public static String io_IncompatibleVersion;
	public static String io_parseError;

	public static String no_provisioning_agent;

	public static String repo_loading;

	public static String repoMan_internalError;
	public static String repoMan_invalidLocation;
}
