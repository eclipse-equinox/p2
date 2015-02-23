/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Fix compiler problems from generified IAdaptable#getAdapter
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * Adapter factory for provisioning elements
 * 
 * @since 3.4
 * 
 */

public class ProvUIAdapterFactory implements IAdapterFactory {
	private static final Class<?>[] CLASSES = new Class[] {IInstallableUnit.class, IProfile.class, IRepository.class, IMetadataRepository.class, IArtifactRepository.class};

	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		return ProvUI.getAdapter(adaptableObject, adapterType);
	}

	public Class<?>[] getAdapterList() {
		return CLASSES;
	}

}
