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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.net.URL;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;

public class BundlePoolFactory extends SimpleArtifactRepositoryFactory {

	public IArtifactRepository load(URL location) {
		SimpleArtifactRepository repository = (SimpleArtifactRepository) super.load(location);
		return new BundlePool(repository);
	}

	public IArtifactRepository create(URL location, String name, String type) {
		SimpleArtifactRepository repository = (SimpleArtifactRepository) super.create(location, name, type);
		return new BundlePool(repository);
	}
}
