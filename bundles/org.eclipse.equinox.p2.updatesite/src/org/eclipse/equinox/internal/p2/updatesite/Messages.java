/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.updatesite;

import org.eclipse.osgi.util.NLS;

/**
 * @since 3.4
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.updatesite.messages"; //$NON-NLS-1$

	public static String UpdateSiteMetadataRepository_ErrorParsingUpdateSite;
	public static String UpdateSiteMetadataRepository_ErrorReadingUpdateSite;
	public static String UpdateSiteMetadataRepository_PlatformAdminNotRegistered;
	public static String UpdateSiteMetadataRepository_ErrorReadingSite;
	public static String UpdateSiteMetadataRepository_InvalidRepositoryLocation;
	public static String UpdateSiteArtifactRepository_InvalidRepositoryLocation;
	public static String UpdateSiteArtifactRepository_ErrorReadingSite;
	public static String UpdateSiteArtifactRepository_ErrorParsingUpdateSite;
	public static String UpdateSiteArtifactRepository_ErrorReadingUpdateSite;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
