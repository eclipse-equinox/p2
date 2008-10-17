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
package org.eclipse.equinox.p2.tests.updatesite;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSite;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * @since 1.0
 */
public class UpdateSiteTest extends AbstractProvisioningTest {
	/*
	 * Constructor for the class.
	 */
	public UpdateSiteTest(String name) {
		super(name);
	}

	/*
	 * Run all the tests in this class.
	 */
	public static Test suite() {
		return new TestSuite(UpdateSiteTest.class);
	}

	public void testRelativeSiteURL() {
		File site = getTestData("0.1", "/testData/updatesite/siteurl");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}

		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testAbsoluteSiteURL() {
		File site = getTestData("0.1", "/testData/updatesite/siteurl2");
		File siteDirectory = getTestData("0.1", "/testData/updatesite/siteurl2/siteurl/");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
			updatesite.getSite().setLocationURIString(siteDirectory.toURI().toString());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}

		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testDefaultDigestURL() {
		File site = getTestData("0.1", "/testData/updatesite/digest");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}

		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testRelativeDigestURL() {
		File site = getTestData("0.1", "/testData/updatesite/digesturl");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}

		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testAbsoluteDigestURL() {
		File site = getTestData("0.1", "/testData/updatesite/digesturl2");
		File digestDirectory = getTestData("0.1", "/testData/updatesite/digesturl2/digesturl/");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
			updatesite.getSite().setDigestURIString(digestDirectory.toURI().toString());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}

		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	/*
	 * Test in which we load an update site from a valid site.xml file. Handle
	 * all the variations in the file.
	 */
	public void testNoDigestGoodSite() {
		File site = getTestData("0.1", "/testData/updatesite/site");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testNoEndingSlashURL() {
		File base = getTestData("0.1", "/testData/updatesite");
		UpdateSite updatesite = null;
		try {
			URI siteURL = base.toURI().resolve("site");
			updatesite = UpdateSite.load(siteURL, getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testSiteXMLURL() {
		File site = getTestData("0.1", "/testData/updatesite/site/site.xml");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(getMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testSiteWithSpaces() {
		File site = getTestData("0.1", "/testData/updatesite/site with spaces/");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testXXXSiteXXXXMLURL() {
		File site = getTestData("0.1", "/testData/updatesite/xxxsitexxx/xxxsitexxx.xml");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testBadXXXSiteXXXXMLURL() {
		File siteDir = getTestData("0.1", "/testData/updatesite/xxxsitexxx");
		File site = new File(siteDir, "site.xml");
		try {
			UpdateSite.load(site.toURI(), getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// expected
		}
	}

	public void testBadDigestGoodSite() {
		File site = getTestData("0.1", "/testData/updatesite/baddigestgoodsite");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			updatesite.loadFeatures(new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testBadDigestBadSite() {
		File site = getTestData("0.1", "/testData/updatesite/baddigestbadsite");
		try {
			UpdateSite.load(site.toURI(), getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// expected
		}
	}

	public void testBadSiteXML() {
		// handle the case where the site.xml doesn't parse correctly
		File site = getTestData("0.1", "/testData/updatesite/badSiteXML");
		try {
			UpdateSite.load(site.toURI(), getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// expected exception
		}
	}

	/*
	 * Test the case where we don't have a digest or site.xml.
	 */
	public void testNoSite() {
		// ensure we have a validate, empty location
		File temp = getTempFolder();
		temp.mkdirs();
		try {
			UpdateSite.load(temp.toURI(), getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// we expect an exception
		}
	}

	public void testNullSite() {
		try {
			assertNull("1.0", UpdateSite.load(null, getMonitor()));
		} catch (ProvisionException e) {
			fail("1.99", e);
		}
	}

	public void testBadFeatureURL() {
		File site = getTestData("0.1", "/testData/updatesite/badfeatureurl");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(0, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	public void testGoodFeatureURL() {
		File site = getTestData("0.1", "/testData/updatesite/goodfeatureurl");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	public void testGetFileURI() throws URISyntaxException {
		URI rootNoSlash = new URI("http://eclipse.org/eclipse/updates");
		URI rootSlash = new URI("http://eclipse.org/eclipse/updates/");
		URI rootSiteXML = new URI("http://eclipse.org/eclipse/updates/site.xml");
		URI rootSiteXML2 = new URI("http://eclipse.org/eclipse/updates/site_old.xml");
		URI[] allURIs = new URI[] {rootNoSlash, rootSlash, rootSiteXML, rootSiteXML2};
		for (URI uri : allURIs) {
			assertEquals("1." + uri, new URI("http://eclipse.org/eclipse/updates/digest.zip"), UpdateSite.getFileURI(uri, "digest.zip"));
		}

		URI rootEmpty = new URI("http://update.eclemma.org");
		assertEquals("2.1", new URI("http://update.eclemma.org/digest.zip"), UpdateSite.getFileURI(rootEmpty, "digest.zip"));
	}

	public void testIncludedFeature() {
		File site = getTestData("0.1", "/testData/updatesite/includedfeature");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(2, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	public void testIncludedFeatureArchive() {
		File site = getTestData("0.1", "/testData/updatesite/includedfeaturearchive");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(2, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	public void testBadIncludedFeatureArchive() {
		File site = getTestData("0.1", "/testData/updatesite/badincludedfeaturearchive");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	public void testNoFeatureIdAndVersion() {
		File site = getTestData("0.1", "/testData/updatesite/nofeatureidandversion");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(2, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	/**
	 * Tests that a feature requiring a bundle with no range is converted correctly.
	 */
	public void testBug243422() {
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		assertNotNull(repoMan);
		File site = getTestData("Update site", "/testData/updatesite/UpdateSite243422/");
		IMetadataRepository metadataRepo = null;
		try {
			metadataRepo = repoMan.loadRepository(site.toURI(), null);
		} catch (ProvisionException e) {
			fail("Can't load repository UpdateSite243422");
		}
		InstallableUnitQuery query = new InstallableUnitQuery("org.eclipse.jdt.astview.feature.feature.group", new Version("1.0.1"));
		Collector result = metadataRepo.query(query, new Collector(), null);
		assertEquals("1.0", 1, result.size());
		IInstallableUnit featureIU = (IInstallableUnit) result.iterator().next();
		RequiredCapability[] required = featureIU.getRequiredCapabilities();
		for (int i = 0; i < required.length; i++) {
			if (required[i].getName().equals("org.eclipse.ui.ide")) {
				assertEquals("2.0", VersionRange.emptyRange, required[i].getRange());
			}
		}
	}

	public void testShortenVersionNumberInFeature() {
		IArtifactRepositoryManager repoMan = (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IArtifactRepositoryManager.class.getName());
		assertNotNull(repoMan);
		File site = getTestData("Update site", "/testData/updatesite/240121/UpdateSite240121/");
		IArtifactRepository artifactRepo = null;
		try {
			artifactRepo = repoMan.loadRepository(site.toURI(), null);
		} catch (ProvisionException e) {
			fail("Can't load repository UpdateSite240121");
		}
		IArtifactKey[] keys = artifactRepo.getArtifactKeys();
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].getId().equals("Plugin240121")) {
				IStatus status = artifactRepo.getArtifact(artifactRepo.getArtifactDescriptors(keys[i])[0], new ByteArrayOutputStream(500), new NullProgressMonitor());
				if (!status.isOK())
					fail("Can't get the expected artifact:" + keys[i]);
			}
		}
	}

	/**
	 * Tests that the feature jar IU has the appropriate touchpoint instruction for
	 * unzipping the feature on install.
	 */
	public void testFeatureJarUnzipInstruction() {
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		File site = getTestData("0.1", "/testData/updatesite/site");
		URI location = null;
		location = site.toURI();
		IMetadataRepository repository;
		try {
			repository = repoMan.loadRepository(location, getMonitor());
		} catch (ProvisionException e) {
			fail("1.99", e);
			return;
		}
		Collector result = repository.query(new InstallableUnitQuery("test.feature.feature.jar"), new Collector(), getMonitor());
		assertTrue("1.0", !result.isEmpty());
		IInstallableUnit unit = (IInstallableUnit) result.iterator().next();
		TouchpointData[] data = unit.getTouchpointData();
		assertEquals("1.1", 1, data.length);
		Map instructions = data[0].getInstructions();
		assertEquals("1.2", 1, instructions.size());
		assertEquals("1.3", "true", ((TouchpointInstruction) instructions.get("zipped")).getBody());
	}

	public void testMirrors() {
		// TODO test the case where the site.xml points to a mirror location
	}
}
