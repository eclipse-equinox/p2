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
package org.eclipse.equinox.prov.ui.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.prov.core.ProvisionException;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.prov.ui.ProvisioningUtil;

/**
 * Element class that represents the root of a metadata
 * repository viewer.  Its children are all of the known
 * metadata repositories.
 * 
 * @since 3.4
 *
 */
public class AllMetadataRepositories extends ProvElement {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.prov.ui.model.ProvElement#fetchChildren(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
	 */
	protected Object[] fetchChildren(Object o, IProgressMonitor monitor, IAdaptable uiInfo) {
		try {
			return ProvisioningUtil.getMetadataRepositories(monitor, uiInfo);
		} catch (ProvisionException e) {
			handleException(e, null);
		}
		return new IMetadataRepository[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return ""; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.prov.ui.model.RepositoryElement#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return null;
	}

}
