/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.metadata;

/**
 * 	Definitions for constants associated with InstallableUnits.
 */
public interface IInstallableUnitConstants {

	/*
	 *  Keys for common properties that optionally 
	 *  be defined in InstallableUnits.
	 */
	public String NAME = "equinox.p2.name"; //$NON-NLS-1$
	public String DESCRIPTION = "equinox.p2.description"; //$NON-NLS-1$
	public String DOC_URL = "equinox.p2.doc.url"; //$NON-NLS-1$
	public String PROVIDER = "equinox.p2.provider"; //$NON-NLS-1$
	public String CONTACT = "equinox.p2.contact"; //$NON-NLS-1$
	public String LICENSE = "equinox.p2.license"; //$NON-NLS-1$
	public String COPYRIGHT = "equinox.p2.copyright"; //$NON-NLS-1$
	public String UPDATE_SITE = "equinox.p2.update.site"; //$NON-NLS-1$
	public String UPDATE_FROM = "equinox.p2.update.from"; //$NON-NLS-1$
	public String UPDATE_RANGE = "equinox.p2.update.range"; //$NON-NLS-1$

	//TODO This is not the ideal location for these constants
	public static final String PROFILE_IU_KEY = "profileIU"; //$NON-NLS-1$	 
	public static final String PROFILE_ROOT_IU = "profileRootIU"; //$NON-NLS-1$
}
