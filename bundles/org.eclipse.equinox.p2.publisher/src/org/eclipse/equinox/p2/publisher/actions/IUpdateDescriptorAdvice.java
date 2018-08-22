/******************************************************************************* 
* Copyright (c) 2010 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

/**
 * Advice to set the update descriptor on an IU
 * @author Ian Bull
 */
public interface IUpdateDescriptorAdvice extends IPublisherAdvice {

	public IUpdateDescriptor getUpdateDescriptor(InstallableUnitDescription iu);
}
