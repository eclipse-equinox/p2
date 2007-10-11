/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.ui.viewers;

import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Viewer filter which shows only IUs that have a property matching
 * the specified value.
 * 
 * @since 3.4
 */
public class IUProfilePropertyFilter extends IUPropertyFilter {

	public IUProfilePropertyFilter(String name, String value) {
		super(name, value);
	}

	protected String getProperty(IInstallableUnit iu, Object parentElement, String key) {
		if (parentElement instanceof Profile)
			return ((Profile) parentElement).getInstallableUnitProfileProperty(iu, key);
		return super.getProperty(iu, parentElement, key);
	}
}
