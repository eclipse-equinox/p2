/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.MetadataRepositoryFactory;

/**
 * A repository factory that always throws exceptions.
 */
public class FailingMetadataRepositoryFactory extends MetadataRepositoryFactory {

	@Override
	public IMetadataRepository create(URI location, String name, String type, Map properties) {
		throw new RuntimeException("Exception thrown deliberately as part of test");
	}

	@Override
	public IMetadataRepository load(URI location, IProgressMonitor monitor) {
		throw new RuntimeException("Exception thrown deliberately as part of test");
	}

	@Override
	public IStatus validate(URI location, IProgressMonitor monitor) {
		throw new RuntimeException("Exception thrown deliberately as part of test");
	}

}
