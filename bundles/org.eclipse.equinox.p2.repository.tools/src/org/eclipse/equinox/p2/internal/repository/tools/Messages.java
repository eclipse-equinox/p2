/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.p2.internal.repository.tools.messages"; //$NON-NLS-1$
	public static String exception_destinationNotModifiable;
	public static String exception_unableToRemoveRepo;
	public static String exception_notLocalFileRepo;
	public static String exception_noEngineService;
	public static String exception_needIUsOrNonEmptyRepo;
	public static String exception_needDestinationRepo;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}

}
