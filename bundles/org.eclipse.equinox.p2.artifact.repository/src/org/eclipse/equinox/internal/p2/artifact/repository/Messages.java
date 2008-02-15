/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.artifact.repository.messages"; //$NON-NLS-1$

	public static String artifact_not_found;
	public static String available_already_in;
	public static String cant_get_outputstream;
	public static String downloading;
	public static String error_closing_stream;
	public static String FileDownloadError;
	public static String io_failedRead;
	public static String io_incompatibleVersion;
	public static String io_parseError;
	public static String mirroring;
	public static String repoMan_exists;
	public static String repoMan_failedRead;
	public static String repoMan_internalError;
	public static String repoMan_notExists;
	public static String repoMan_unknownType;
	public static String repoFailedWrite;
	public static String repoReadOnly;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

}