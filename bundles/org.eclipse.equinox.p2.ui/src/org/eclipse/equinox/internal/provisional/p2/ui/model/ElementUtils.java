/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.model;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;

/**
 * Utility methods for manipulating model elements.
 * 
 * @since 3.4
 *
 */
public class ElementUtils {
	public static IInstallableUnit[] getIUs(Object[] elements) {
		if (elements == null || elements.length == 0)
			return new IInstallableUnit[0];
		Set iuChildren = new HashSet(elements.length);
		for (int i = 0; i < elements.length; i++) {
			iuChildren.addAll(getIUs(elements[i]));
		}
		return (IInstallableUnit[]) iuChildren.toArray(new IInstallableUnit[iuChildren.size()]);
	}

	public static Set getIUs(Object element) {
		Set ius = new HashSet();
		// Check first for a container.  Elements like categories are both
		// a container and an IU, and the container aspect is what we want
		// when we want to find out which IU's to manipulate.
		if (element instanceof IUContainerElement) {
			ius.addAll(Arrays.asList(((IUContainerElement) element).getIUs()));
		} else if (element instanceof IUElement) {
			ius.add(((IUElement) element).getIU());
		} else {
			IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(element, IInstallableUnit.class);
			if (iu != null)
				ius.add(iu);
		}
		return ius;
	}
}
