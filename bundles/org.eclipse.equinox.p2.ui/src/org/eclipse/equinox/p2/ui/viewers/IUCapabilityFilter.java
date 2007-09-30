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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Viewer filter which filters IU's so that only those that satisfy the required
 * capabilities appear in the viewer.
 * 
 * @since 3.4
 */
public class IUCapabilityFilter extends ViewerFilter {

	private RequiredCapability[] requirements;

	public IUCapabilityFilter(RequiredCapability[] requiredCapabilities) {
		super();
		this.requirements = requiredCapabilities;
	}

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		IInstallableUnit iu = null;
		if (element instanceof InstallableUnit) {
			iu = (IInstallableUnit) element;
		} else if (element instanceof IAdaptable) {
			iu = (IInstallableUnit) ((IAdaptable) element).getAdapter(InstallableUnit.class);
		}
		if (iu == null) {
			return true;
		}
		ProvidedCapability[] capabilities = iu.getProvidedCapabilities();
		for (int i = 0; i < requirements.length; i++) {
			boolean satisfied = false;
			for (int j = 0; j < capabilities.length; j++) {
				if (capabilities[j].isSatisfiedBy(requirements[i])) {
					satisfied = true;
					break;
				}
			}
			if (!satisfied) {
				return false;
			}
		}
		return true;
	}
}
