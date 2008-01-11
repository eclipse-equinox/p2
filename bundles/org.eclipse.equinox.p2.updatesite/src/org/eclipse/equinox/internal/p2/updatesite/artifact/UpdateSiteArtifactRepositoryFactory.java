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
package org.eclipse.equinox.internal.p2.updatesite.artifact;

import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.spi.p2.artifact.repository.IArtifactRepositoryFactory;

public class UpdateSiteArtifactRepositoryFactory implements IArtifactRepositoryFactory {

	public IArtifactRepository create(URL location, String name, String type) {
		return null;
	}

	public IArtifactRepository load(URL location, IProgressMonitor monitor) {
		if (!location.getPath().endsWith("site.xml"))
			return null;
		return new UpdateSiteArtifactRepository(location, monitor);
	}
}
