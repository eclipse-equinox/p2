/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import java.util.Collection;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

public interface IPublisherResult {
	public static final int MERGE_MATCHING = 0;
	public static final int MERGE_ALL_ROOT = 1;
	public static final int MERGE_ALL_NON_ROOT = 2;

	// type markers
	public static final String ROOT = "root"; //$NON-NLS-1$
	public static final String NON_ROOT = "non_root"; //$NON-NLS-1$

	public void addIU(IInstallableUnit iu, String type);

	public void addIUs(Collection ius, String type);

	/**
	 * Returns the IUs in this result with the given id.
	 */
	public Collection getIUs(String id, String type);

	public IInstallableUnit getIU(String id, String type);

	public void merge(IPublisherResult result, int mode);
}
