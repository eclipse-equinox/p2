/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;

import java.util.Map;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public interface IPropertyAdvice extends IPublisherAdvice {

	/**
	 * Returns the set of extra properties to be associated with the IU
	 */
	public Map<String, String> getInstallableUnitProperties(InstallableUnitDescription iu);

	/**
	 * Returns the set of extra properties to be associated with the artifact descriptor
	 * being published
	 */
	public Map<String, String> getArtifactProperties(IInstallableUnit iu, IArtifactDescriptor descriptor);
}
