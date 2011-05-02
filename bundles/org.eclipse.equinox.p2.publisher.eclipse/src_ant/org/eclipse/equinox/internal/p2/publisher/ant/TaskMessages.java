/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.ant;

import org.eclipse.osgi.util.NLS;

public class TaskMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.publisher.ant.messages";//$NON-NLS-1$

	public static String exception_errorOccurredCallingGenerator;
	public static String exception_errorArtifactRepo;
	public static String exception_errorMetadataRepo;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, TaskMessages.class);
	}
}
