/*******************************************************************************
 *  Copyright (c) 2007, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.util.Collection;

/**
 * Represents a fragment that contributes additional requirements, capabilities, 
 * and other properties to some host installable unit. Installable unit fragments
 * are not directly installed, but rather they alter the metadata of other installable
 * units.
 * <p>
 * Instances of this class are handle objects and do not necessarily
 * reflect entities that exist in any particular profile or repository. These handle 
 * objects can be created using {@link MetadataFactory}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 * @see MetadataFactory#createInstallableUnitFragment(org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription)
 */
public interface IInstallableUnitFragment extends IInstallableUnit {
	public Collection<IRequirement> getHost();
}