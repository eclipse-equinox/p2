/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.persistence;

import org.eclipse.equinox.p2.metadata.Version;

public interface P2FConstants {

	public static final Version CURRENT_VERSION = Version.createOSGi(1, 0, 0);

	public static final String P2F_ELEMENT = "p2f"; //$NON-NLS-1$
	public static final String IUS_ELEMENT = "ius"; //$NON-NLS-1$
	public static final String IU_ELEMENT = "iu"; //$NON-NLS-1$

	public static final String REPOSITORIES_ELEMENT = "repositories"; //$NON-NLS-1$
	public static final String REPOSITORY_ELEMENT = "repository"; //$NON-NLS-1$
	public static final String P2FURI_ATTRIBUTE = "uri"; //$NON-NLS-1$

}