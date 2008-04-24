/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.generator;

import org.eclipse.equinox.internal.p2.publisher.PublisherInfo;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.tests.TestArtifactRepository;
import org.eclipse.equinox.p2.tests.TestMetadataRepository;

/**
 * Simple implementation of IGeneratorInfo used for testing purposes.
 */
public class TestPublisherInfo extends PublisherInfo {

	public IArtifactRepository getArtifactRepository() {
		IArtifactRepository result = super.getArtifactRepository();
		if (result != null) {
			result = new TestArtifactRepository();
			setArtifactRepository(result);
		}
		return result;
	}

	public IMetadataRepository getMetadataRepository() {
		IMetadataRepository result = super.getMetadataRepository();
		if (result != null) {
			result = new TestMetadataRepository(new IInstallableUnit[0]);
			setMetadataRepository(result);
		}
		return result;
	}
}
