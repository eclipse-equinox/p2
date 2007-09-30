/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.prov.metadata;

/**
 * 	Definitions for constants associated with InstallableUnits.
 */
public interface IInstallableUnitConstants {

	/*
	 *  Keys for common properties that optionally 
	 *  be defined in InstallableUnits.
	 */
	public String NAME = "equinox.prov.name"; //$NON-NLS-1$
	public String DESCRIPTION = "equinox.prov.description"; //$NON-NLS-1$
	public String DOC_URL = "equinox.prov.doc.url"; //$NON-NLS-1$
	public String PROVIDER = "equinox.prov.provider"; //$NON-NLS-1$
	public String CONTACT = "equinox.prov.contact"; //$NON-NLS-1$
	public String LICENSE = "equinox.prov.license"; //$NON-NLS-1$
	public String COPYRIGHT = "equinox.prov.copyright"; //$NON-NLS-1$
	public String UPDATE_SITE = "equinox.prov.update.site"; //$NON-NLS-1$
	public String UPDATE_FROM = "equinox.prov.update.from"; //$NON-NLS-1$
	public String UPDATE_RANGE = "equinox.prov.update.range"; //$NON-NLS-1$

	//TODO This is not the ideal location for these constants
	public String ENTRYPOINT_IU_KEY = "entryPoint"; //$NON-NLS-1$
	public static final String PROFILE_IU_KEY = "profileIU"; //$NON-NLS-1$	 

}
