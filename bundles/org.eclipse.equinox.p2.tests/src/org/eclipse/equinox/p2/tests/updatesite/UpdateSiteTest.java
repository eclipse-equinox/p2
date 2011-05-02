/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.updatesite;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorSelector;
import org.eclipse.equinox.internal.p2.artifact.repository.RawMirrorRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.updatesite.SiteFeature;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSite;
import org.eclipse.equinox.internal.p2.updatesite.artifact.UpdateSiteArtifactRepository;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.StringBufferStream;
import org.w3c.dom.*;

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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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

	public void testZippedDefaultDigestURL() throws URISyntaxException {
		File site = getTestData("0.1", "/testData/updatesite/digest/site.zip");
		URI siteURI = new URI("jar:" + site.toURI() + "!/");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(siteURI, getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(siteURL, getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			UpdateSite.load(site.toURI(), getTransport(), getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// expected
		}
	}

	public void testBadDigestGoodSite() {
		File site = getTestData("0.1", "/testData/updatesite/baddigestgoodsite");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		try {
			updatesite.loadFeatures(new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testCorruptDigestGoodSite() {
		File site = getTestData("0.1", "/testData/updatesite/corruptdigestgoodsite");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		StringBuffer buffer = new StringBuffer();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream(buffer)));
			updatesite.loadFeatures(new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("0.4", e);
		} finally {
			System.setOut(out);
		}
		assertTrue(buffer.toString().contains("Content is not allowed in prolog."));
	}

	public void testBadDigestBadSite() {
		File site = getTestData("0.1", "/testData/updatesite/baddigestbadsite");
		try {
			UpdateSite.load(site.toURI(), getTransport(), getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// expected
		}
	}

	public void testBadSiteXML() {
		// handle the case where the site.xml doesn't parse correctly
		File site = getTestData("0.1", "/testData/updatesite/badSiteXML");
		try {
			UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			UpdateSite.load(temp.toURI(), getTransport(), getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// we expect an exception
		}
	}

	public void testNullSite() {
		try {
			assertNull("1.0", UpdateSite.load(null, getTransport(), getMonitor()));
		} catch (ProvisionException e) {
			fail("1.99", e);
		}
	}

	public void testBadFeatureURL() {
		File site = getTestData("0.1", "/testData/updatesite/badfeatureurl");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		StringBuffer buffer = new StringBuffer();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream(buffer)));
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(0, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");

		} finally {
			System.setOut(out);
		}
		assertTrue(buffer.toString().contains("Error reading feature"));
	}

	public void testGoodFeatureURL() {
		File site = getTestData("0.1", "/testData/updatesite/goodfeatureurl");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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

	public void testZippedGoodFeatureURL() throws URISyntaxException {

		File site = getTestData("0.1", "/testData/updatesite/goodfeatureurl/site.zip");
		URI siteURI = new URI("jar:" + site.toURI() + "!/");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(siteURI, getTransport(), getMonitor());
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

	public void testIncludedFeature() {
		File site = getTestData("0.1", "/testData/updatesite/includedfeature");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		StringBuffer buffer = new StringBuffer();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream(buffer)));
			int featureCount = updatesite.loadFeatures(new NullProgressMonitor()).length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		} finally {
			System.setOut(out);
		}
		assertTrue(buffer.toString().contains("Error reading feature"));
	}

	public void testNoFeatureIdAndVersion() {
		File site = getTestData("0.1", "/testData/updatesite/nofeatureidandversion");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURI(), getTransport(), getMonitor());
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

	public void testSiteFeatureVersionEquals() {
		SiteFeature a = new SiteFeature();
		SiteFeature b = new SiteFeature();
		assertEquals("1.0", a, b);
		b.setFeatureVersion("1.0.0");
		a.setFeatureVersion("1.0.0");
		b.setFeatureVersion("1.0.0");
		assertEquals("1.1", a, b);
		b.setFeatureVersion("2.0.0");
		assertFalse("1.2", a.equals(b));
		b.setFeatureVersion(null);
		assertFalse("1.3", a.equals(b));
		assertFalse("1.4", b.equals(a));
	}

	public void testSiteFeatureLabelEquals() {
		SiteFeature a = new SiteFeature();
		SiteFeature b = new SiteFeature();
		assertEquals("1.0", a, b);
		a.setLabel("foo");
		b.setLabel("foo");
		assertEquals("1.1", a, b);
		b.setLabel("bar");
		assertFalse("1.2", a.equals(b));
		b.setLabel(null);
		assertFalse("1.3", a.equals(b));
		assertFalse("1.4", b.equals(a));
	}

	public void testSiteFeatureIDEquals() {
		SiteFeature a = new SiteFeature();
		SiteFeature b = new SiteFeature();
		assertEquals("1.0", a, b);
		a.setFeatureIdentifier("org.foo");
		b.setFeatureIdentifier("org.foo");
		assertEquals("1.1", a, b);
		b.setFeatureIdentifier("org.bar");
		assertFalse("1.2", a.equals(b));
		b.setFeatureIdentifier(null);
		assertFalse("1.3", a.equals(b));
		assertFalse("1.4", b.equals(a));
	}

	public void testSiteFeatureEquals() {
		SiteFeature a = new SiteFeature();
		SiteFeature b = new SiteFeature();
		assertEquals("1.0", a, b);
		a.setURLString("http://foo");
		assertFalse("1.1", a.equals(b));
		b.setURLString("http://foo");
		assertEquals("1.2", a, b);
		a.setURLString("http://FOO");
		assertEquals("1.3", a, b);
		a.setURLString("file://FOO");
		assertFalse("1.4", a.equals(b));
		a.setURLString(null);
		assertFalse("1.5", a.equals(b));
		assertFalse("1.6", b.equals(a));
	}

	public void testSiteFeatureHash() {
		SiteFeature a = new SiteFeature();
		SiteFeature b = new SiteFeature();
		assertEquals("1.0", a.hashCode(), b.hashCode());
		a.setURLString("http://foo");
		b.setURLString("http://foo");
		assertEquals("1.1", a.hashCode(), b.hashCode());
		a.setURLString("http://FOO/");
		assertEquals("1.2", a.hashCode(), b.hashCode());
		a.setURLString("foo");
		b.setURLString("FoO");
		assertEquals("1.3", a.hashCode(), b.hashCode());
	}

	public void testSiteFeatureNotEquals() {
		SiteFeature a = new SiteFeature();
		SiteFeature b = new SiteFeature();
		assertEquals("1.0", a, b);
		a.setURLString("file:/c:/foo");
		assertFalse("1.1", a.equals(b));
		b.setURLString("file:/c:/bar");
		assertFalse("1.2", a.equals(b));
		assertFalse("1.3", b.equals(a));
		a.setURLString("http://foo");
		b.setURLString("http://bar/");
		assertFalse("1.4", b.equals(a));
	}

	public void testSiteFeatureFileURL() {
		SiteFeature a = new SiteFeature();
		SiteFeature b = new SiteFeature();
		assertEquals("1.0", a, b);
		a.setURLString("file:/c:/foo");
		b.setURLString("file:/c:/FOO");
		if (a.equals(b))
			assertEquals("1.1", a.hashCode(), b.hashCode());
		a.setURLString("FILE:/c:/foo");
		b.setURLString("file:/c:/FOO");
		if (a.equals(b))
			assertEquals("1.2", a.hashCode(), b.hashCode());
		a.setURLString("HTTP://example.com");
		b.setURLString("HTtP://example.com");
		if (a.equals(b))
			assertEquals("1.3", a.hashCode(), b.hashCode());
		a.setURLString("HTTP://eXaMpLe.com");
		b.setURLString("HTtP://example.com");
		if (a.equals(b))
			assertEquals("1.4", a.hashCode(), b.hashCode());
		a.setURLString("HTTP://eXaMpLe.com/");
		b.setURLString("HTtP://example.com");
		assertEquals(a, b);
		if (a.equals(b))
			assertEquals("1.5", a.hashCode(), b.hashCode());
		a.setURLString("http://localhost");
		b.setURLString("http://127.0.0.1");
		if (a.equals(b))
			assertEquals("1.6", a.hashCode(), b.hashCode());
	}

	public void testRepoWithFeatureWithNullUpdateURL() {
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(repoMan);
		File site = getTestData("Update site", "/testData/updatesite/missingUpdateURLFeature/");
		IMetadataRepository metadataRepo = null;
		StringBuffer buffer = new StringBuffer();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream(buffer)));
			metadataRepo = repoMan.loadRepository(site.toURI(), null);
		} catch (ProvisionException e) {
			fail("Can't load repository missingUpdateURLFeature");
		} finally {
			System.setOut(out);
		}
		assertTrue(buffer.toString().contains("Invalid site reference null in feature test.featurewithmissingupdateurl."));
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery("test.featurewithmissingupdateurl.feature.group", Version.create("1.0.0"));
		IQueryResult result = metadataRepo.query(query, null);
		assertEquals("1.0", 1, queryResultSize(result));
	}

	/**
	 * Tests that a feature requiring a bundle with no range is converted correctly.
	 */
	public void testBug243422() {
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(repoMan);
		File site = getTestData("Update site", "/testData/updatesite/UpdateSite243422/");
		IMetadataRepository metadataRepo = null;
		try {
			metadataRepo = repoMan.loadRepository(site.toURI(), null);
		} catch (ProvisionException e) {
			fail("Can't load repository UpdateSite243422");
		}
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery("org.eclipse.jdt.astview.feature.feature.group", Version.create("1.0.1"));
		IQueryResult result = metadataRepo.query(query, null);
		assertEquals("1.0", 1, queryResultSize(result));
		IInstallableUnit featureIU = (IInstallableUnit) result.iterator().next();
		Collection<IRequirement> required = featureIU.getRequirements();
		for (Iterator iterator = required.iterator(); iterator.hasNext();) {
			IRequiredCapability req = (IRequiredCapability) iterator.next();
			if (req.getName().equals("org.eclipse.ui.ide")) {
				assertEquals("2.0", VersionRange.emptyRange, req.getRange());
			}
		}
	}

	public void testShortenVersionNumberInFeature() {
		IArtifactRepositoryManager repoMan = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		assertNotNull(repoMan);
		File site = getTestData("Update site", "/testData/updatesite/240121/UpdateSite240121/");
		IArtifactRepository artifactRepo = null;
		try {
			artifactRepo = repoMan.loadRepository(site.toURI(), null);
		} catch (ProvisionException e) {
			fail("Can't load repository UpdateSite240121");
		}
		IQueryResult keys = artifactRepo.query(new ArtifactKeyQuery(null, "Plugin240121", null), null);
		assertEquals(1, queryResultSize(keys));
		IArtifactKey key = (IArtifactKey) keys.iterator().next();
		IStatus status = artifactRepo.getArtifact(artifactRepo.getArtifactDescriptors(key)[0], new ByteArrayOutputStream(500), new NullProgressMonitor());
		if (!status.isOK())
			fail("Can't get the expected artifact:" + key);
	}

	/**
	 * Tests that the feature jar IU has the appropriate touchpoint instruction for
	 * unzipping the feature on install.
	 */
	public void testFeatureJarUnzipInstruction() {
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
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
		IQueryResult result = repository.query(QueryUtil.createIUQuery("test.feature.feature.jar"), getMonitor());
		assertTrue("1.0", !result.isEmpty());
		IInstallableUnit unit = (IInstallableUnit) result.iterator().next();
		Collection<ITouchpointData> data = unit.getTouchpointData();
		assertEquals("1.1", 1, data.size());
		Map instructions = data.iterator().next().getInstructions();
		assertEquals("1.2", 1, instructions.size());
		assertEquals("1.3", "true", ((ITouchpointInstruction) instructions.get("zipped")).getBody());
	}

	/**
	 * TODO Failing test, see bug 265528.
	 */
	public void _testFeatureSiteReferences() throws ProvisionException, URISyntaxException {
		File site = getTestData("0.1", "/testData/updatesite/siteFeatureReferences");
		URI siteURI = site.toURI();
		URI testUpdateSite = new URI("http://download.eclipse.org/test/updatesite/");
		URI testDiscoverySite = new URI("http://download.eclipse.org/test/discoverysite");

		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(manager);
		manager.removeRepository(testUpdateSite);
		manager.removeRepository(testDiscoverySite);
		IMetadataRepository repository = manager.loadRepository(siteURI, 0, getMonitor());
		try {
			//wait for site references to be published asynchronously
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			fail("4.99", e);
		}
		assertNotNull(repository);
		assertTrue("1.0", manager.contains(testUpdateSite));
		assertTrue("1.1", manager.contains(testDiscoverySite));
		assertFalse("1.2", manager.isEnabled(testUpdateSite));
		assertFalse("1.3", manager.isEnabled(testDiscoverySite));
	}

	public void testMetadataRepoCount() {
		File site = getTestData("0.1", "/testData/updatesite/site");
		URI siteURI = site.toURI();

		IMetadataRepositoryManager metadataRepoMan = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(metadataRepoMan);

		URI[] knownRepos = metadataRepoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < knownRepos.length; i++) {
			if (siteURI.equals(knownRepos[i])) {
				metadataRepoMan.removeRepository(siteURI);
				knownRepos = metadataRepoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
				break;
			}
		}

		try {
			metadataRepoMan.loadRepository(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("1.0", e);
			return;
		}
		URI[] afterKnownRepos = metadataRepoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		assertTrue("1.1", afterKnownRepos.length == knownRepos.length + 1);
	}

	public void testArtifactRepoCount() {
		File site = getTestData("0.1", "/testData/updatesite/site");
		URI siteURI = site.toURI();

		IArtifactRepositoryManager artifactRepoMan = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		assertNotNull(artifactRepoMan);

		URI[] knownRepos = artifactRepoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < knownRepos.length; i++) {
			if (siteURI.equals(knownRepos[i])) {
				artifactRepoMan.removeRepository(siteURI);
				knownRepos = artifactRepoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
				break;
			}
		}

		try {
			artifactRepoMan.loadRepository(site.toURI(), getMonitor());
		} catch (ProvisionException e) {
			fail("1.0", e);
			return;
		}
		URI[] afterKnownRepos = artifactRepoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		assertTrue("1.1", afterKnownRepos.length == knownRepos.length + 1);
	}

	public void testPack200() {
		File output = new File(getTempFolder(), getUniqueString());
		File site = getTestData("0.1", "/testData/updatesite/packedSiteWithMirror");
		URI siteURI = site.toURI();

		IArtifactRepository repo = null;
		try {
			repo = getArtifactRepositoryManager().loadRepository(siteURI, new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		}
		IArtifactKey key = new ArtifactKey("org.eclipse.update.feature", "test.feature", Version.create("1.0.0"));
		IArtifactDescriptor[] descriptors = repo.getArtifactDescriptors(key);

		// Should have a packed & canonical version
		assertEquals(2, descriptors.length);
		IArtifactDescriptor desc = IArtifactDescriptor.FORMAT_PACKED.equals(descriptors[0].getProperty(IArtifactDescriptor.FORMAT)) ? descriptors[0] : descriptors[1];
		OutputStream out = null;
		try {
			out = new FileOutputStream(output);
			IStatus status = repo.getRawArtifact(desc, out, new NullProgressMonitor());
			out.close();
			// Transfer should succeed
			assertTrue(status.isOK());
			// Length should be as expected
			assertEquals(480, output.length());
		} catch (IOException e) {
			fail("Failed", e);
		} finally {
			getArtifactRepositoryManager().removeRepository(siteURI);
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					// Don't care
				}
		}
	}

	public void testMirrors() {
		String testDataLocation = "/testData/updatesite/packedSiteWithMirror";
		File targetLocation = null;
		URI siteURI = getTestData("0.1", testDataLocation).toURI();
		try {
			IArtifactKey key = new ArtifactKey("osgi.bundle", "test.fragment", Version.create("1.0.0"));
			// Load source repository
			IArtifactRepository sourceRepo = getArtifactRepositoryManager().loadRepository(siteURI, getMonitor());

			// Hijack source repository's mirror selector
			new OrderedMirrorSelector(sourceRepo, testDataLocation);

			// Create target repository
			targetLocation = File.createTempFile("target", ".repo");
			targetLocation.delete();
			targetLocation.mkdirs();
			IArtifactRepository targetRepository = new SimpleArtifactRepository(getAgent(), "TargetRepo", targetLocation.toURI(), null);

			// Load the packed descriptor
			IArtifactDescriptor[] descriptors = sourceRepo.getArtifactDescriptors(key);
			IArtifactDescriptor descriptor = null;
			for (int i = 0; i < descriptors.length && descriptor == null; i++)
				if (IArtifactDescriptor.FORMAT_PACKED.equals(descriptors[i].getProperty(IArtifactDescriptor.FORMAT)))
					descriptor = descriptors[i];

			if (descriptor == null)
				fail("0.3");

			RawMirrorRequest mirror = new RawMirrorRequest(descriptor, new ArtifactDescriptor(descriptor), targetRepository, getTransport());
			mirror.perform(sourceRepo, getMonitor());

			assertTrue(mirror.getResult().isOK());
			assertTrue(targetRepository.contains(key));
		} catch (Exception e) {
			fail("0.2", e);
		} finally {
			if (targetLocation != null)
				delete(targetLocation);
			getArtifactRepositoryManager().removeRepository(siteURI);
		}
	}

	/*
	 * Special mirror selector for testing which chooses mirrors in order
	 */
	protected class OrderedMirrorSelector extends MirrorSelector {
		private URI repoLocation;
		int index = 0;
		MirrorInfo[] mirrors;
		IArtifactRepository repo;

		OrderedMirrorSelector(IArtifactRepository repo, String testDataLocation) throws Exception {
			super(repo, getTransport());
			this.repo = repo;
			// Alternatively we could use reflect to change "location" of the repo
			setRepoSelector();
			getRepoLocation();
			mirrors = computeMirrors("file:///" + getTestData("Mirror Location", testDataLocation + '/' + repo.getProperties().get(IRepository.PROP_MIRRORS_URL)).toString().replace('\\', '/'));
		}

		private void setRepoSelector() throws Exception {
			Field delegate = UpdateSiteArtifactRepository.class.getDeclaredField("delegate");
			delegate.setAccessible(true);
			// Hijack the source repository's MirrorSelector with ours which provides mirrors in order.
			Field mirrorsField = SimpleArtifactRepository.class.getDeclaredField("mirrors");
			mirrorsField.setAccessible(true);
			mirrorsField.set(delegate.get(repo), this);

			// Setting this property forces SimpleArtifactRepository to use mirrors despite being a local repo
			Field properties = AbstractRepository.class.getDeclaredField("properties");
			properties.setAccessible(true);
			((Map) properties.get(delegate.get(repo))).put(SimpleArtifactRepository.PROP_FORCE_THREADING, String.valueOf(true));
		}

		// Overridden to prevent mirror sorting
		@Override
		public synchronized void reportResult(String toDownload, IStatus result) {
			return;
		}

		// We want to test each mirror once.
		@Override
		public synchronized boolean hasValidMirror() {
			return mirrors != null && index < mirrors.length;
		}

		@Override
		public synchronized URI getMirrorLocation(URI inputLocation, IProgressMonitor monitor) {
			return URIUtil.append(nextMirror(), repoLocation.relativize(inputLocation).getPath());
		}

		private URI nextMirror() {
			Field mirrorLocation = null;
			try {
				mirrorLocation = MirrorInfo.class.getDeclaredField("locationString");
				mirrorLocation.setAccessible(true);

				if (index < mirrors.length)
					return URIUtil.makeAbsolute(new URI((String) mirrorLocation.get(mirrors[index++])), repoLocation);
				return repoLocation;
			} catch (Exception e) {
				fail(Double.toString(0.4 + index), e);
				return null;
			}
		}

		private synchronized void getRepoLocation() {
			Field locationField = null;
			try {
				locationField = UpdateSiteArtifactRepository.class.getDeclaredField("location");
				locationField.setAccessible(true);
				repoLocation = (URI) locationField.get(repo);
			} catch (Exception e) {
				fail("0.3", e);
			}
		}

		private MirrorInfo[] computeMirrors(String mirrorsURL) {
			// Copied & modified from MirrorSelector
			try {
				DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = domFactory.newDocumentBuilder();
				Document document = builder.parse(mirrorsURL);
				if (document == null)
					return null;
				NodeList mirrorNodes = document.getElementsByTagName("mirror"); //$NON-NLS-1$
				int mirrorCount = mirrorNodes.getLength();
				MirrorInfo[] infos = new MirrorInfo[mirrorCount + 1];
				for (int i = 0; i < mirrorCount; i++) {
					Element mirrorNode = (Element) mirrorNodes.item(i);
					String infoURL = mirrorNode.getAttribute("url"); //$NON-NLS-1$
					infos[i] = new MirrorInfo(infoURL, i);
				}
				//p2: add the base site as the last resort mirror so we can track download speed and failure rate
				infos[mirrorCount] = new MirrorInfo(repoLocation.toString(), mirrorCount);
				return infos;
			} catch (Exception e) {
				// log if absolute url
				if (mirrorsURL != null && (mirrorsURL.startsWith("http://") //$NON-NLS-1$
						|| mirrorsURL.startsWith("https://") //$NON-NLS-1$
						|| mirrorsURL.startsWith("file://") //$NON-NLS-1$
						|| mirrorsURL.startsWith("ftp://") //$NON-NLS-1$
				|| mirrorsURL.startsWith("jar://"))) //$NON-NLS-1$
					fail("Error processing mirrors URL: " + mirrorsURL, e); //$NON-NLS-1$
				return null;
			}
		}
	}
}