/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.updatesite;

import java.io.File;
import java.net.URI;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.updatesite.artifact.UpdateSiteArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.updatesite.metadata.UpdateSiteMetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class DoesNotCacheStaleData extends AbstractProvisioningTest {

	public void testLoadBadSiteForMetadata() {
		URI siteURI = getTestData("badUpdateSite", "testData/updatesite/badSiteXML/site.xml").toURI();
		File f = URIUtil.toFile(UpdateSiteMetadataRepositoryFactory.getLocalRepositoryLocation(siteURI));
		File contentXml = new File(f, "content.xml");
		contentXml.delete();
		Exception e = null;
		try {
			new UpdateSiteMetadataRepositoryFactory().load(siteURI, 0, new NullProgressMonitor());
		} catch (ProvisionException e1) {
			e = e1;
		}
		assertNotNull(e);
		assertTrue(new File(URIUtil.toFile(UpdateSiteMetadataRepositoryFactory.getLocalRepositoryLocation(siteURI)), "content.xml").exists());
		try {
			IMetadataRepository repo = new SimpleMetadataRepositoryFactory().load(f.toURI(), 0, new NullProgressMonitor());
			assertEquals("0", repo.getProperties().get("site.checksum"));
		} catch (ProvisionException e1) {
			fail("3.0", e1);
		}
	}

	public void testLoadBadSiteForArtifact() {
		URI siteURI = getTestData("badUpdateSite", "testData/updatesite/badSiteXML/site.xml").toURI();
		File f = URIUtil.toFile(UpdateSiteMetadataRepositoryFactory.getLocalRepositoryLocation(siteURI));
		File contentXml = new File(f, "artifacts.xml");
		contentXml.delete();
		Exception e = null;
		try {
			new UpdateSiteArtifactRepositoryFactory().load(siteURI, 0, new NullProgressMonitor());
		} catch (ProvisionException e1) {
			e = e1;
		}
		assertNotNull(e);
		assertTrue(new File(URIUtil.toFile(UpdateSiteMetadataRepositoryFactory.getLocalRepositoryLocation(siteURI)), "artifacts.xml").exists());
		try {
			IArtifactRepository repo = new SimpleArtifactRepositoryFactory().load(f.toURI(), 0, new NullProgressMonitor());
			assertEquals("0", repo.getProperties().get("site.checksum"));
		} catch (ProvisionException e1) {
			fail("3.0", e1);
		}
	}
}
