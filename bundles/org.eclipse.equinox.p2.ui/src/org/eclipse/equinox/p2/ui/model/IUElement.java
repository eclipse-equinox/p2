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
package org.eclipse.equinox.p2.ui.model;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Interface for elements that represent IU's.
 * 
 * @since 3.4
 */
public interface IUElement {

	public static final long SIZE_UNKNOWN = -1L;

	public IInstallableUnit getIU();

	public boolean shouldShowSize();

	public boolean shouldShowVersion();

	public long getSize();

	public void computeSize();
}
