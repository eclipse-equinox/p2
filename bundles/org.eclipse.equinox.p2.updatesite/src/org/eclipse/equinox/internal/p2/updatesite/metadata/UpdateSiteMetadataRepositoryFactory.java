/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.metadata;

import java.net.URL;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.spi.p2.metadata.repository.IMetadataRepositoryFactory;

public class UpdateSiteMetadataRepositoryFactory implements IMetadataRepositoryFactory {

	public IMetadataRepository create(URL location, String name, String type) {
		return null;
	}

	public IMetadataRepository load(URL location) {
		if (!location.getPath().endsWith("site.xml"))
			return null;
		return new UpdateSiteMetadataRepository(location);
	}
}
