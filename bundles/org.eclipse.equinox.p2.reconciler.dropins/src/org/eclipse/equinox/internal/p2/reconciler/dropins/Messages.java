/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype Inc. - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import org.eclipse.osgi.util.NLS;

/**
 * @since 1.0
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.reconciler.dropins.messages"; //$NON-NLS-1$
	public static String artifact_repo_manager_not_registered;
	public static String errorLoadingRepository;
	public static String errorProcessingConfg;
	public static String metadata_repo_manager_not_registered;
	public static String error_reading_link;
	public static String error_resolving_link;
	public static String remove_root;
	public static String remove_all_roots;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// prevent instantiation
	}
}
