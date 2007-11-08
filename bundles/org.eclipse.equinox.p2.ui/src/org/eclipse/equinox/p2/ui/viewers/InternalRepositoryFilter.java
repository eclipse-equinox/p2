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

import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Viewer filter which filters out internal repositories.
 * 
 * @since 3.4
 */
public class InternalRepositoryFilter extends ViewerFilter {

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		IRepository repo = (IRepository) ProvUI.getAdapter(element, IRepository.class);
		if (repo == null) {
			return true;
		}
		Object implOnly = repo.getProperties().get(IRepository.IMPLEMENTATION_ONLY_KEY);
		return implOnly == null || !Boolean.valueOf((String) implOnly).booleanValue();
	}
}
