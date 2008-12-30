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
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;

/**
 * Interface for elements that represent IU's.
 * 
 * @since 3.4
 */
public interface IIUElement {

	/**
	 * Indicates that the size is currently unknown
	 */
	public static final long SIZE_UNKNOWN = -1L;

	/**
	 * Indicates that the size is unavailable (an
	 * attempt was made to compute size but it failed)
	 */
	public static final long SIZE_UNAVAILABLE = -2L;

	/**
	 * Indicates that there was nothing to size (there
	 * was no valid plan that could be used to compute
	 * size).
	 */
	public static final long SIZE_NOTAPPLICABLE = -3L;

	public IInstallableUnit getIU();

	public boolean shouldShowSize();

	public boolean shouldShowVersion();

	public long getSize();

	public void computeSize(IProgressMonitor monitor);

	public IRequiredCapability[] getRequirements();

	public Object getParent(Object obj);

	public boolean shouldShowChildren();
}
