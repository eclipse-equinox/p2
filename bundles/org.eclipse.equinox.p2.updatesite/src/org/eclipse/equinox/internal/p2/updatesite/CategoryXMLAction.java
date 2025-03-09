/******************************************************************************* 
* Copyright (c) 2009, 2017 EclipseSource and others.
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
*   Sonatype, Inc. - transport split
******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;

/**
 * This action parses a category file and publishes all the categories whose 
 * elements are contained in the publisher results.
 */
public class CategoryXMLAction extends SiteXMLAction {

	public CategoryXMLAction(URI location, String categoryQualifier) {
		super(location, categoryQualifier);
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		try {
			updateSite = UpdateSite.loadCategoryFile(location, getTransport(publisherInfo), monitor);
		} catch (ProvisionException e) {
			return new Status(IStatus.ERROR, Activator.ID, Messages.Error_generating_category, e);
		}
		if (updateSite == null) {
			return new Status(IStatus.ERROR, Activator.ID, Messages.Error_generating_category);
		}
		return super.perform(publisherInfo, results, monitor);
	}
}
