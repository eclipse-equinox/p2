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
package org.eclipse.equinox.internal.provisional.p2.ui;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

/**
 * Abstract class for a manager which tracks which licenses have been accepted.
 * 
 * @since 3.4
 */
public abstract class LicenseManager {

	public abstract boolean acceptLicense(IInstallableUnit iu);

	public abstract boolean rejectLicense(IInstallableUnit iu);

	public abstract boolean isAccepted(IInstallableUnit iu);

	public abstract boolean hasAcceptedLicenses();
}
