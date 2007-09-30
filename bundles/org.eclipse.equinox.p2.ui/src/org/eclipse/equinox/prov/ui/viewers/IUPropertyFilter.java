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

package org.eclipse.equinox.prov.ui.viewers;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Viewer filter which shows only IUs that have a property matching
 * the specified value.
 * 
 * @since 3.4
 */
public class IUPropertyFilter extends ViewerFilter {

	private String propertyName;
	private String propertyValue;

	public IUPropertyFilter(String name, String value) {
		this.propertyName = name;
		this.propertyValue = value;
	}

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		IInstallableUnit iu = null;
		if (element instanceof IInstallableUnit) {
			iu = (IInstallableUnit) element;
		} else if (element instanceof IAdaptable) {
			iu = (IInstallableUnit) ((IAdaptable) element).getAdapter(IInstallableUnit.class);
		}
		if (iu == null)
			return false;
		String prop = iu.getProperty(propertyName);
		if (prop == null)
			return false;
		return prop.equals(propertyValue);
	}
}
