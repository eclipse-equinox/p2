/*******************************************************************************
 *  Copyright (c) 2008, 2010 EclipseSource and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      Eclipse Source - initial API and implementation
 *      IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;

import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

public interface ICapabilityAdvice extends IPublisherAdvice {

	public IProvidedCapability[] getProvidedCapabilities(InstallableUnitDescription iu);

	public IRequirement[] getRequiredCapabilities(InstallableUnitDescription iu);

	public IRequirement[] getMetaRequiredCapabilities(InstallableUnitDescription iu);
}
