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
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Viewer filter which filters out internal repositories.
 * 
 * @since 3.4
 */
public class InternalRepositoryFilter extends ViewerFilter {

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		IRepository repo = null;
		if (element instanceof IRepository) {
			repo = (IRepository) element;
		} else if (element instanceof IAdaptable) {
			repo = (IRepository) ((IAdaptable) element).getAdapter(IRepository.class);
		}
		if (repo == null) {
			return true;
		}
		return !(Boolean.valueOf(repo.getProperties().getProperty(IRepository.IMPLEMENTATION_ONLY_KEY, "false"))).booleanValue(); //$NON-NLS-1$
	}
}
