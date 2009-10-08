/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;

import java.io.ByteArrayOutputStream;
import java.io.File;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ArtifactRepositoryWithReferenceDescriptors extends AbstractProvisioningTest {
	IArtifactRepository repo = null;
	ArtifactDescriptor descriptor1 = null;
	ArtifactDescriptor descriptor2 = null;

	//    <artifact classifier='org.eclipse.update.feature' id='org.eclipse.datatools.sqldevtools.feature' version='1.6.0.v200805301340-7F7d-E8yz-SHrDBONwUwXwIyxYSZ'>
	//    <repositoryProperties size='4'>
	//      <property name='artifact.reference' value='file:/Users/Pascal/Downloads/eclipse 2/features/org.eclipse.datatools.sqldevtools.feature_1.6.0.v200805301340-7F7d-E8yz-SHrDBONwUwXwIyxYSZ/'/>
	//      <property name='artifact.folder' value='true'/>
	//      <property name='file.name' value='/Users/Pascal/Downloads/eclipse 2/features/org.eclipse.datatools.sqldevtools.feature_1.6.0.v200805301340-7F7d-E8yz-SHrDBONwUwXwIyxYSZ'/>
	//      <property name='file.lastModified' value='1214242394000'/>
	//    </repositoryProperties>
	//  </artifact>

	protected void setUp() throws Exception {
		super.setUp();
		repo = createArtifactRepository(getTempFolder().toURI(), null);
		File fileLocation = getTestData("Artifacts for repositor with references", "testData/referenceArtifactRepo/test1 Reference.jar");
		descriptor1 = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "test1Reference", new Version("1.0.0")));
		descriptor1.setProcessingSteps(new ProcessingStepDescriptor[0]);
		descriptor1.setRepositoryProperty("artifact.reference", fileLocation.toURL().toExternalForm());
		descriptor1.setRepositoryProperty("file.name", fileLocation.getAbsolutePath());
		descriptor1.setRepositoryProperty("file.lastModified", Long.toString(fileLocation.lastModified()));

		descriptor2 = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "test1Reference", new Version("1.0.0")));
		descriptor2.setProcessingSteps(new ProcessingStepDescriptor[0]);
		descriptor2.setRepositoryProperty("artifact.reference", fileLocation.toURI().toString());
		descriptor2.setRepositoryProperty("file.name", fileLocation.getAbsolutePath());
		descriptor2.setRepositoryProperty("file.lastModified", Long.toString(fileLocation.lastModified()));

		repo.addDescriptor(descriptor1);
		repo.addDescriptor(descriptor2);
	}

	public void testOldStyleReference() {
		IStatus s = repo.getArtifact(descriptor1, new ByteArrayOutputStream(500), new NullProgressMonitor());
		if (!s.isOK())
			fail("1.0 Can not find artifact for the given descriptor. Status" + s.toString());
	}

	public void testNewStyleReference() {
		IStatus s = repo.getArtifact(descriptor2, new ByteArrayOutputStream(500), new NullProgressMonitor());
		if (!s.isOK())
			fail("1.1 Can not find artifact for the given descriptor. Status" + s.toString());
	}

}
