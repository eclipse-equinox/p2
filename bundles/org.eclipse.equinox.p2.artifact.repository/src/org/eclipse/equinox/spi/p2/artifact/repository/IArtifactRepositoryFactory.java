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
package org.eclipse.equinox.spi.p2.artifact.repository;

import java.net.URL;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;

public interface IArtifactRepositoryFactory {

	public IArtifactRepository load(URL location);

	public IArtifactRepository create(URL location, String name, String type);
}
