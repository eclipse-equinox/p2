/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.common;

import java.io.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.ILicense;

/**
 * Abstract class for a license manager which tracks which 
 * licenses have been accepted in a provisioning client.
 * 
 * @since 2.0
 */
public abstract class LicenseManager {

	public abstract boolean accept(ILicense license);

	public abstract boolean reject(ILicense license);

	public abstract boolean isAccepted(ILicense license);

	public abstract boolean hasAcceptedLicenses();

	/**
	 * 
	 * @param stream
	 * @throws IOException 
	 * @since 2.0
	 */
	public abstract void write(OutputStream stream) throws IOException;

	/**
	 * 
	 * @param stream
	 * @throws IOException 
	 * @since 2.0
	 */
	public abstract void read(InputStream stream) throws IOException;

}
