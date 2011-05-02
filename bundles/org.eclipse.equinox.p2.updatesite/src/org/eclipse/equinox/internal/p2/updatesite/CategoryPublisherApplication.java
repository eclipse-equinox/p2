/*******************************************************************************
 * Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * <p>
 * This application categorizes the elements in a repo based on a category definition file.  The category definition
 * file is specified with <source>-categoryDefinition</source>
 * </p>
 */
public class CategoryPublisherApplication extends AbstractPublisherApplication {

	private String categoryQualifier = null;
	private URI categoryDefinition = null;

	public CategoryPublisherApplication() {
		// nothing todo
	}

	/*
	 * Check to see if an existing repository already has the "compressed" flag set
	 */
	protected void initializeRepositories(PublisherInfo publisherInfo) throws ProvisionException {
		try {
			if (metadataLocation != null) {
				// Try to load the metadata repository. If it loads, check the "compressed" flag, and cache it.
				// If there are any errors loading it (i.e. it doesn't exist), just skip this step
				// If there are serious problems with the repository, the superclass initializeRepositories method
				// will handle it.
				IMetadataRepository result = Publisher.loadMetadataRepository(agent, metadataLocation, true, true);
				if (result != null) {
					Object property = result.getProperties().get(IRepository.PROP_COMPRESSED);
					if (property != null) {
						boolean compressProperty = Boolean.valueOf((String) property);
						this.compress = compressProperty || compress;
					}
				}
			}
		} catch (ProvisionException e) {
			//do nothing
		}
		super.initializeRepositories(publisherInfo);
	}

	protected void processParameter(String arg, String parameter, PublisherInfo pinfo) throws URISyntaxException {
		super.processParameter(arg, parameter, pinfo);

		this.append = true; // Always append, otherwise we will end up with nothing

		if (arg.equalsIgnoreCase("-categoryQualifier")) //$NON-NLS-1$
			categoryQualifier = parameter;

		if (arg.equalsIgnoreCase("-categoryDefinition")) //$NON-NLS-1$
			categoryDefinition = URIUtil.fromString(parameter);

	}

	protected IPublisherAction[] createActions() {
		return new IPublisherAction[] {new CategoryXMLAction(categoryDefinition, categoryQualifier)};
	}
}