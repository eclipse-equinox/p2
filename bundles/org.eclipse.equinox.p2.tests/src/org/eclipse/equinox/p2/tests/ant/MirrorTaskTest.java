/*******************************************************************************
 *  Copyright (c) 2009, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ant;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.comparator.ArtifactChecksumComparator;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractAntProvisioningTest;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.NLS;

public class MirrorTaskTest extends AbstractAntProvisioningTest {
	private static final String DOWNLOAD_CHECKSUM = IArtifactDescriptor.DOWNLOAD_CHECKSUM + ".sha-256";
	private static final String MIRROR_TASK = "p2.mirror";
	private URI destinationRepo;
	private URI artifactRepo, sliceArtifactRepo, sliceRepo, sourceRepo2, zipRepo;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		// Get a random location to create a repository
		destinationRepo = (new File(getTestFolder(getName()), "destinationRepo")).toURI();
		artifactRepo = getTestData("error loading data", "testData/mirror/mirrorRepo").toURI();
		sourceRepo2 = getTestData("error loading data", "testData/mirror/mirrorSourceRepo2").toURI();
		sliceRepo = getTestData("error loading data", "testData/permissiveSlicer").toURI();
		sliceArtifactRepo = getTestData("error loading data", "testData/testRepos/updateSite").toURI();
		zipRepo = getTestData("error loading data", "/testData/mirror/zippedRepo.zip").toURI();
	}

	@Override
	public void tearDown() throws Exception {
		// Remove repository manager references
		getArtifactRepositoryManager().removeRepository(destinationRepo);
		getMetadataRepositoryManager().removeRepository(destinationRepo);
		getArtifactRepositoryManager().removeRepository(sliceRepo);
		getMetadataRepositoryManager().removeRepository(sliceRepo);
		getArtifactRepositoryManager().removeRepository(sourceRepo2);
		getMetadataRepositoryManager().removeRepository(sourceRepo2);
		getArtifactRepositoryManager().removeRepository(zipRepo);
		getMetadataRepositoryManager().removeRepository(zipRepo);
		// Cleanup disk
		delete(new File(destinationRepo).getParentFile());
		super.tearDown();
	}

	/*
	 * Test that it is possible to mirror by only specifying an Artifact repository
	 */
	public void testMirrorArtifactOnly() {
		AntTaskElement mirror = createMirrorTask(TYPE_ARTIFACT);
		mirror.addElement(createSourceElement(artifactRepo, null));
		runAntTask();

		assertEquals("Different number of Artifact Keys", getArtifactKeyCount(artifactRepo), getArtifactKeyCount(destinationRepo));
		assertEquals("Different number of ArtifactDescriptors", getArtifactDescriptorCount(artifactRepo), getArtifactDescriptorCount(destinationRepo));
	}

	/*
	 * Test that it is possible to mirror when only specifying a Metadata repository
	 */
	public void testMirrorMetadataOnly() {
		AntTaskElement mirror = createMirrorTask(TYPE_METADATA);
		mirror.addElement(createSourceElement(null, sourceRepo2));
		runAntTask();

		assertEquals("Different number of IUs", getIUCount(sourceRepo2), getIUCount(destinationRepo));
	}

	public void testMirrorEmptyBaseline() throws Exception {
		File folder = getTestFolder("MirrorEmptyBaseline");
		String baseline = "file:" + new File(folder, "base").getAbsolutePath();
		String dest = "file:" + new File(folder, "destination").getAbsolutePath();
		String logFile = new File(folder, "log.txt").getAbsolutePath();

		AntTaskElement mirror = new AntTaskElement("p2.artifact.mirror");
		mirror.addAttribute("source", URIUtil.toUnencodedString(sourceRepo2));
		mirror.addAttribute("baseline", baseline);
		mirror.addAttribute("comparatorId", "org.eclipse.equinox.p2.repository.tools.jar.comparator");
		mirror.addAttribute("destination", dest);
		mirror.addAttribute("log", logFile);
		addTask(mirror);
		runAntTask();
		assertLogContainsLines(new File(folder, "log.txt"),
				"No repository found at " + URIUtil.toUnencodedString(URIUtil.fromString(baseline)));
	}

	/*
	 * Test we can mirror from a zipped repository
	 */
	public void testMirrorFromZip() {
		URI localAddress = null;
		try {
			localAddress = URIUtil.fromString(new File(zipRepo).toString());
		} catch (URISyntaxException e) {
			fail("failed to convert zip repo location");
		}
		AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
		mirror.addElement(createSourceElement(localAddress, localAddress));
		runAntTask();

		assertEquals("Wrong number of ArtifactKeys", getArtifactKeyCount(URIUtil.toJarURI(zipRepo, null)), getArtifactKeyCount(destinationRepo));
		assertEquals("Wrong number of ArtifactDescriptors", getArtifactDescriptorCount(URIUtil.toJarURI(zipRepo, null)), getArtifactDescriptorCount(destinationRepo));
		assertEquals("Different number of IUs", getIUCount(sourceRepo2), getIUCount(destinationRepo));
	}

	/*
	 * Test that all IUs can be mirrored
	 */
	public void testMirrorAllIUSpecified() throws ProvisionException {
		AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
		mirror.addElement(createSourceElement(sourceRepo2, sourceRepo2));
		addAllIUs(mirror, getMetadataRepositoryManager().loadRepository(sourceRepo2, null));
		runAntTask();

		assertEquals("Different number of Artifact Keys", getArtifactKeyCount(sourceRepo2), getArtifactKeyCount(destinationRepo));
		assertContentEquals("IUs differ", getMetadataRepositoryManager().loadRepository(sourceRepo2, null), getMetadataRepositoryManager().loadRepository(destinationRepo, null));
		assertEquals("Different number of IUs", getIUCount(sourceRepo2), getIUCount(destinationRepo));
		assertContentEquals("Artifacts differ", getArtifactRepositoryManager().loadRepository(sourceRepo2, null), getArtifactRepositoryManager().loadRepository(destinationRepo, null));
	}

	/*
	 * Test that we only mirror specified IUs & Artifacts
	 */
	public void testMirrorSomeIUSpecified() {
		AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
		mirror.addElement(createSourceElement(sourceRepo2, sourceRepo2));
		mirror.addElement(createIUElement("anotherplugin", "1.0.0"));

		runAntTask();

		assertEquals("Wrong number of ArtifactKeys", 1, getArtifactKeyCount(destinationRepo));
		assertEquals("Wrong number of IUs", 1, getIUCount(destinationRepo));
	}

	/*
	 * Test what occurs with a missing IU
	 */
	public void testMirrorMissingIU() {
		String id = "My_Missing_IU";
		String version = "1.0.0";
		String iu = "Installable Unit [ id=" + id + " version=" + version + " ]";

		AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
		mirror.addElement(createSourceElement(sourceRepo2, sourceRepo2));
		mirror.addElement(createIUElement(id, version));
		Exception exception = null;
		try {
			runAntTaskWithExceptions();
		} catch (CoreException e) {
			exception = e;
		}
		if (exception == null)
			fail("No Exception was thrown");

		assertEquals("Unexpected message", NLS.bind(org.eclipse.equinox.p2.internal.repository.tools.Messages.AbstractRepositoryTask_unableToFind, iu), rootCause(exception).getMessage());
	}

	/*
	 * Test that the proper exception is thrown when no IU is provided
	 */
	public void testMirrorMetadataDestinationWithoutSource() {
		AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
		mirror.addElement(createSourceElement(sourceRepo2, null));

		Exception exception = null;
		try {
			runAntTaskWithExceptions();
		} catch (CoreException e) {
			exception = e;
		}
		if (exception == null)
			fail("No exception thrown");
		if (!(rootCause(exception) instanceof ProvisionException && rootCause(exception).getMessage().equals(org.eclipse.equinox.p2.internal.repository.tools.Messages.MirrorApplication_metadataDestinationNoSource)))
			fail("Exception is of an unexpected type or message", rootCause(exception));
	}

	/*
	 * Test that the proper exception is thrown when no IU is provided
	 */
	public void testMirrorArtifactDestinationWithoutSource() {
		AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
		mirror.addElement(createSourceElement(null, sourceRepo2));

		Exception exception = null;
		try {
			runAntTaskWithExceptions();
		} catch (CoreException e) {
			exception = e;
		}
		if (exception == null)
			fail("No exception thrown");
		if (!(rootCause(exception) instanceof ProvisionException) && rootCause(exception).getMessage().contains(org.eclipse.equinox.p2.internal.repository.tools.Messages.MirrorApplication_artifactDestinationNoSource))
			fail("Exception is of an unexpected type or message", rootCause(exception));
	}

	/*
	 * Test that all IUs are mirrored when none are specified
	 */
	public void testMirrorNoIUSpecified() {
		AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
		mirror.addElement(createSourceElement(sourceRepo2, sourceRepo2));

		runAntTask();

		try {
			assertEquals("Different number of Artifact Keys", getArtifactKeyCount(sourceRepo2), getArtifactKeyCount(destinationRepo));
			assertContentEquals("Artifacts differ", getArtifactRepositoryManager().loadRepository(sourceRepo2, null), getArtifactRepositoryManager().loadRepository(destinationRepo, null));
			assertEquals("Different number of IUs", getIUCount(sourceRepo2), getIUCount(destinationRepo));
			assertContentEquals("IUs differ", getMetadataRepositoryManager().loadRepository(sourceRepo2, null), getMetadataRepositoryManager().loadRepository(destinationRepo, null));
		} catch (ProvisionException e) {
			fail("Failed to compare results", e);
		}
	}

	/*
	 * Test the handling of invalid destinations with the mirror task
	 */
	public void testMirrorWithInvalidDestination() throws URISyntaxException {
		URI location = new URI("invalid:/scheme");

		AntTaskElement mirror = new AntTaskElement(MIRROR_TASK);
		mirror.addElement(getRepositoryElement(location, TYPE_BOTH));
		mirror.addElement(createSourceElement(sourceRepo2, sourceRepo2));
		addTask(mirror);

		Throwable exception = null;
		try {
			runAntTaskWithExceptions();
		} catch (Exception e) {
			exception = e;
		}
		if (exception == null)
			fail("No Exception thrown");

		while (exception.getCause() != null && !(exception instanceof ProvisionException))
			exception = exception.getCause();
		assertEquals("Unexpected error", NLS
				.bind(org.eclipse.equinox.p2.internal.repository.tools.Messages.exception_invalidDestination, location),
				exception.getMessage());
	}

	/*
	 * Test the behavior when a valid path is provided as source, but no repository is present at the location
	 */
	public void testMirrorWithNoRepositoryAtSource() {
		URI location = getTempFolder().toURI();
		try {
			AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
			mirror.addElement(createSourceElement(location, location));
			addTask(mirror);

			Throwable exception = null;
			try {
				runAntTaskWithExceptions();
			} catch (Exception e) {
				exception = e;
			}
			if (exception == null)
				fail("No Exception thrown");

			while (exception.getCause() != null && !(exception instanceof ProvisionException))
				exception = exception.getCause();
			assertEquals("Unexpected error", NLS.bind("No repository found at {0}.", location), exception.getMessage());
		} finally {
			delete(new File(location));
		}
	}

	/*
	 * Test the handling of invalid destinations with the mirror task
	 */
	public void testMirrorWithInvalidSource() throws URISyntaxException {
		URI location = new URI("unknown:/scheme2");

		AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
		mirror.addElement(createSourceElement(location, location));
		addTask(mirror);

		Throwable exception = null;
		try {
			runAntTaskWithExceptions();
		} catch (Exception e) {
			exception = e;
		}
		if (exception == null)
			fail("No Exception thrown");

		while (exception.getCause() != null && !(exception instanceof ProvisionException))
			exception = exception.getCause();
		assertTrue("Expecting a CoreException", exception instanceof CoreException);
		assertEquals("Unexpected error code.", ProvisionException.REPOSITORY_NOT_FOUND, ((CoreException) exception).getStatus().getCode());
	}

	/*
	 * Test slicing options
	 */
	public void testSlicingFollowStrict() {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(sliceRepo);
		} catch (ProvisionException e) {
			fail("Loading repository failed", e);
		}
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();

		AntTaskElement mirror = createMirrorTask(TYPE_METADATA);
		mirror.addElement(createSourceElement(null, sliceRepo));
		mirror.addElement(createSlicingOption(null, null, true, null, "win32,win32,x86", null));
		mirror.addElement(createIUElement(iu));

		runAntTask();

		Map<String, String> p = getSliceProperties();
		p.put("org.eclipse.update.install.features", String.valueOf(true));
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, true, true, true, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertEquals("Different number of IUs", queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())), getIUCount(destinationRepo));

		try {
			assertIUContentEquals("IUs differ", result, getMetadataRepositoryManager().loadRepository(destinationRepo, null));
		} catch (ProvisionException e) {
			fail("Failed to compare contents", e);
		}
	}

	public void testSlicingIncludeNonGreedy() {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(sliceRepo);
		} catch (ProvisionException e) {
			fail("Loading repository failed", e);
		}
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();

		AntTaskElement mirror = createMirrorTask(TYPE_METADATA);
		mirror.addElement(createSourceElement(null, sliceRepo));
		mirror.addElement(createSlicingOption(null, false, null, null, null, null));
		mirror.addElement(createIUElement(iu));

		runAntTask();

		PermissiveSlicer slicer = new PermissiveSlicer(repo, Collections.emptyMap(), true, false, true, false, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());

		assertEquals("Different number of IUs", queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())), getIUCount(destinationRepo));
		try {
			assertIUContentEquals("IUs differ", result, getMetadataRepositoryManager().loadRepository(destinationRepo, null));
		} catch (ProvisionException e) {
			fail("Failed to compare contents", e);
		}
	}

	public void testSlicingIncludeOptionalDependencies() {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(sliceRepo);
		} catch (ProvisionException e) {
			fail("Loading repository failed", e);
		}
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();

		AntTaskElement mirror = createMirrorTask(TYPE_METADATA);
		mirror.addElement(createSourceElement(null, sliceRepo));
		mirror.addElement(createSlicingOption(false, null, null, null, "win32,win32,x86", null));
		mirror.addElement(createIUElement(iu));

		runAntTask();

		Map<String, String> p = getSliceProperties();
		p.put("org.eclipse.update.install.features", String.valueOf(true));
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, false, true, true, false, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertEquals("Different number of IUs", queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())), getIUCount(destinationRepo));
		try {
			assertIUContentEquals("IUs differ", result, getMetadataRepositoryManager().loadRepository(destinationRepo, null));
		} catch (ProvisionException e) {
			fail("Failed to compare contents", e);
		}
	}

	public void testSlicingFilter() {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(sliceRepo);
		} catch (ProvisionException e) {
			fail("Loading repository failed", e);
		}
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();

		AntTaskElement mirror = createMirrorTask(TYPE_METADATA);
		mirror.addElement(createSourceElement(null, sliceRepo));
		mirror.addElement(createSlicingOption(null, null, null, null, "win32,win32,x86", "org.eclipse.update.install.features=false"));
		mirror.addElement(createIUElement(iu));

		runAntTask();

		Map<String, String> p = getSliceProperties();
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, true, true, false, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertEquals("Different number of IUs", queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())), getIUCount(destinationRepo));
		try {
			assertIUContentEquals("IUs differ", result, getMetadataRepositoryManager().loadRepository(destinationRepo, null));
		} catch (ProvisionException e) {
			fail("Failed to compare contents", e);
		}
	}

	/*
	 * Test the platform filter
	 */
	public void testSlicingPlatformFilter() {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(sliceRepo);
		} catch (ProvisionException e) {
			fail("Loading repository failed", e);
		}
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();

		AntTaskElement mirror = createMirrorTask(TYPE_METADATA);
		mirror.addElement(createSourceElement(null, sliceRepo));
		mirror.addElement(createSlicingOption(null, null, null, null, "win32,win32,x86", null));
		mirror.addElement(createIUElement(iu));

		runAntTask();

		Map<String, String> p = getSliceProperties();
		p.put("org.eclipse.update.install.features", String.valueOf(true));
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, true, true, false, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertEquals("Different number of IUs", queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())), getIUCount(destinationRepo));
		try {
			assertIUContentEquals("IUs differ", result, getMetadataRepositoryManager().loadRepository(destinationRepo, null));
		} catch (ProvisionException e) {
			fail("Failed to compare contents", e);
		}
	}

	/*
	 * Test disabling includeFeatures for SlicingOptions
	 */
	public void testSlicingIncludeFeaturesFalse() {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(sliceRepo);
		} catch (ProvisionException e) {
			fail("Loading repository failed", e);
		}
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();

		// Create task
		AntTaskElement mirror = createMirrorTask(TYPE_METADATA);
		mirror.addElement(createSourceElement(null, sliceRepo));
		mirror.addElement(createSlicingOption(null, null, null, false, "win32,win32,x86", null));
		mirror.addElement(createIUElement(iu));

		runAntTask();

		Map<String, String> p = getSliceProperties();
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, true, true, false, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertEquals("Different number of IUs", queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())), getIUCount(destinationRepo));
		try {
			assertIUContentEquals("IUs differ", result, getMetadataRepositoryManager().loadRepository(destinationRepo, null));
		} catch (ProvisionException e) {
			fail("Failed to compare contents", e);
		}
	}

	/*
	 * Tests the results of a slice are used to mirror artifacts
	 */
	public void testSlicingMetadataAndArtifactsMirrored() {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(sliceArtifactRepo);
		} catch (ProvisionException e) {
			fail("Loading repository failed", e);
		}
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("test.feature.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();

		// Create task
		AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
		mirror.addElement(createSourceElement(sliceArtifactRepo, sliceArtifactRepo));
		mirror.addElement(createSlicingOption(null, null, null, false, "win32,win32,x86", null));
		mirror.addElement(createIUElement(iu));

		runAntTask();

		Map<String, String> p = getSliceProperties();
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, true, true, false, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());

		assertEquals("Different number of IUs", queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())), getIUCount(destinationRepo));
		assertEquals("Different number of ArtifactKeys", getArtifactKeyCount(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())), getArtifactKeyCount(destinationRepo));
		try {
			assertArtifactKeyContentEquals("Different ArtifactKeys", result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()), destinationRepo);
			assertIUContentEquals("IUs differ", result, getMetadataRepositoryManager().loadRepository(destinationRepo, null));
		} catch (ProvisionException e) {
			fail("Failed to compare contents", e);
		}

	}

	/*
	 * Test the ability to slice an IU and mirror only the artifacts
	 */
	public void testMirrorSlicedIUtoArtifact() {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(sliceArtifactRepo);
		} catch (ProvisionException e) {
			fail("Loading repository failed", e);
		}
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("test.feature.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();

		// Create task
		AntTaskElement mirror = createMirrorTask(TYPE_ARTIFACT);
		mirror.addElement(createSourceElement(sliceArtifactRepo, sliceArtifactRepo));
		mirror.addElement(createSlicingOption(null, null, null, false, "win32,win32,x86", null));
		mirror.addElement(createIUElement(iu));

		runAntTask();

		Map<String, String> p = getSliceProperties();
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, true, true, false, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());

		assertEquals("Different number of ArtifactKeys", getArtifactKeyCount(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())), getArtifactKeyCount(destinationRepo));
		assertArtifactKeyContentEquals("Different ArtifactKeys", result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()), destinationRepo);
	}

	/*
	 * Test the ability to slice an IU and mirror only the artifacts
	 */
	public void testMirrorSlicedMultipleIUsToArtifact() {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(sliceArtifactRepo);
		} catch (ProvisionException e) {
			fail("Loading repository failed", e);
		}
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("test.feature.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();

		c = repo.query(QueryUtil.createIUQuery("RCP_Browser_Example.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu2 = c.iterator().next();

		// Create task
		AntTaskElement mirror = createMirrorTask(TYPE_ARTIFACT);
		mirror.addElement(createSourceElement(sliceArtifactRepo, sliceArtifactRepo));
		mirror.addElement(createSlicingOption(null, null, null, false, "win32,win32,x86", null));
		mirror.addElement(createIUElement(iu));
		mirror.addElement(createIUElement(iu2));

		runAntTask();

		Map<String, String> p = getSliceProperties();
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, true, true, false, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu, iu2), new NullProgressMonitor());

		assertEquals("Different number of ArtifactKeys", getArtifactKeyCount(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())), getArtifactKeyCount(destinationRepo));
		assertArtifactKeyContentEquals("Different ArtifactKeys", result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()), destinationRepo);
	}

	public void testMirrorCompareWithIgnore() throws Exception {
		File testFolder = getTestFolder("mirrorWithIgnore");
		File base = new File(testFolder, "base");
		File source = new File(testFolder, "source");
		File dest = new File(testFolder, "destination");

		//some content for our fake bundles
		Properties props = new Properties();
		props.put("key", "value");

		IArtifactKey key = new ArtifactKey("osgi.bundle", "a", Version.parseVersion("1.0.0"));
		IArtifactRepository[] repos = new IArtifactRepository[] {createArtifactRepository(base.toURI(), null), createArtifactRepository(source.toURI(), null)};
		for (int i = 0; i < 2; i++) {
			try (ZipOutputStream stream = new ZipOutputStream(repos[i].getOutputStream(repos[i].createArtifactDescriptor(key)))) {
				ZipEntry entry = new ZipEntry("file.properties");
				stream.putNextEntry(entry);
				props.store(stream, String.valueOf(i));
				stream.closeEntry();
			}
		}
		key = new ArtifactKey("osgi.bundle", "b", Version.parseVersion("1.0.0"));
		for (int i = 0; i < 2; i++) {
			try (ZipOutputStream stream = new ZipOutputStream(repos[i].getOutputStream(repos[i].createArtifactDescriptor(key)))) {
				ZipEntry entry = new ZipEntry("file.properties");
				stream.putNextEntry(entry);
				props.put("boo", String.valueOf(i));
				props.store(stream, String.valueOf(i));
				stream.closeEntry();
			}
		}

		AntTaskElement mirror = createMirrorTask(TYPE_ARTIFACT);
		mirror.addElement(new AntTaskElement("source", new String[] {"location", URIUtil.toUnencodedString(source.toURI()), "kind", "artifact"}));
		mirror.addElement(new AntTaskElement("destination", new String[] {"location", URIUtil.toUnencodedString(dest.toURI()), "kind", "artifact"}));
		AntTaskElement comparator = new AntTaskElement("comparator");
		comparator.addAttribute("comparator", "org.eclipse.equinox.p2.repository.tools.jar.comparator");
		comparator.addAttribute("comparatorLog", new File(testFolder, "log.txt").getAbsolutePath());
		comparator.addElement(new AntTaskElement("repository", new String[] {"location", URIUtil.toUnencodedString(base.toURI())}));
		AntTaskElement exclude = new AntTaskElement("exclude");
		exclude.addElement(new AntTaskElement("artifact", new String[] {"id", "b"}));
		comparator.addElement(exclude);
		mirror.addElement(comparator);
		runAntTask();
	}

	/*
	 * Test the ability to slice an IU and mirror only the artifacts
	 */
	public void testMirrorIUtoArtifact() {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(sliceArtifactRepo);
		} catch (ProvisionException e) {
			fail("Loading repository failed", e);
		}
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.ui.examples.readmetool"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();

		// Create task
		AntTaskElement mirror = createMirrorTask(TYPE_ARTIFACT);
		mirror.addElement(createSourceElement(sliceArtifactRepo, sliceArtifactRepo));
		mirror.addElement(createIUElement(iu));

		runAntTask();

		Collector<IInstallableUnit> collector = new Collector<>();
		collector.accept(iu);

		assertEquals("Different number of ArtifactKeys", getArtifactKeyCount(collector), getArtifactKeyCount(destinationRepo));
		assertArtifactKeyContentEquals("Different ArtifactKeys", collector, destinationRepo);
	}

	/*
	 * Test the result of a slice which results in no IUs
	 */
	public void testSlicingInvalid() {
		AntTaskElement mirror = createMirrorTask(TYPE_METADATA);
		mirror.addElement(createSourceElement(null, sliceRepo));
		mirror.addElement(createSlicingOption(null, null, null, null, "win32,win32,x86", null));

		Exception exception = null;
		try {
			runAntTaskWithExceptions();
		} catch (Exception e) {
			exception = e;
		}

		if (exception == null || !(rootCause(exception) instanceof ProvisionException)) {
			fail("Unexpected exception type", exception);
		}
	}

	/*
	 * Modified from org.eclipse.equinox.p2.tests.mirror.ArtifactMirrorApplicationTest
	 */
	public void testBaselineCompareUsingComparator() {
		// Setup create descriptors with different checksum values
		IArtifactKey dupKey = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
		File artifact1 = getTestData("0.0", "/testData/mirror/mirrorSourceRepo1 with space/content.xml");
		File artifact2 = getTestData("0.0", "/testData/mirror/mirrorSourceRepo2/content.xml");

		//Setup Copy the file to the baseline
		File repoLocation = getTestFolder(getUniqueString());
		File baselineLocation = getTestFolder(getUniqueString());
		File baselineBinaryDirectory = new File(baselineLocation, "binary");
		baselineBinaryDirectory.mkdir();
		File baselineContentLocation = new File(baselineBinaryDirectory, "testKeyId_1.2.3");
		AbstractProvisioningTest.copy("Copying File to baseline", artifact2, baselineContentLocation);

		IArtifactDescriptor descriptor1 = PublisherHelper.createArtifactDescriptor(dupKey, artifact1);
		IArtifactDescriptor descriptor2 = PublisherHelper.createArtifactDescriptor(dupKey, baselineContentLocation);

		assertEquals("Ensuring Descriptors are the same", descriptor1, descriptor2);
		assertNotEquals("Ensuring download checksums are different", descriptor1.getProperty(DOWNLOAD_CHECKSUM),
				descriptor2.getProperty(DOWNLOAD_CHECKSUM));

		//Setup make repositories
		IArtifactRepository repo = null;
		IArtifactRepository baseline = null;
		try {
			repo = createRepositoryWithIU(repoLocation.toURI(), descriptor1);
			baseline = createRepositoryWithIU(baselineLocation.toURI(), descriptor2);
		} catch (ProvisionException e) {
			fail("Error creating repositories", e);
		}

		//Comparator prints to stderr, redirect that to a file
		PrintStream oldErr = System.err;
		PrintStream newErr = null;
		PrintStream oldOut = System.out;
		PrintStream newOut = null;
		try {
			new File(destinationRepo).mkdir();
			newErr = new PrintStream(new FileOutputStream(new File(new File(destinationRepo), "sys.err")));
			newOut = new PrintStream(new FileOutputStream(new File(new File(destinationRepo), "sys.out")));
		} catch (FileNotFoundException e) {
			fail("Error redirecting outputs", e);
		}

		try {
			System.setErr(newErr);
			System.setOut(newOut);

			// Create task
			AntTaskElement mirror = createMirrorTask(TYPE_BOTH);
			// Add source
			mirror.addElement(createSourceElement(repoLocation.toURI(), repoLocation.toURI()));
			// set verbose
			mirror.addAttribute("verbose", String.valueOf(true));

			// Create a comparator element
			AntTaskElement comparator = new AntTaskElement("comparator");
			comparator.addAttribute("comparator", ArtifactChecksumComparator.COMPARATOR_ID + ".sha-256");
			comparator.addElement(getRepositoryElement(baselineLocation.toURI(), null));
			mirror.addElement(comparator);

			runAntTaskWithExceptions();
		} catch (Exception e) {
			fail("Running mirror application with baseline compare", rootCause(e));
		} finally {
			System.setErr(oldErr);
			newErr.close();
			System.setOut(oldOut);
			newOut.close();
		}

		IArtifactRepository destination = null;
		try {
			destination = getArtifactRepositoryManager().loadRepository(destinationRepo, null);
		} catch (ProvisionException e) {
			fail("Error loading destination", e);
		}

		IArtifactDescriptor[] destDescriptors = destination.getArtifactDescriptors(descriptor2.getArtifactKey());
		assertEquals("Ensuring destination has correct number of descriptors", 1, destDescriptors.length);
		assertEquals("Ensuring destination contains the descriptor from the baseline",
				descriptor2.getProperty(DOWNLOAD_CHECKSUM), destDescriptors[0].getProperty(DOWNLOAD_CHECKSUM));
		String msg = NLS.bind(Messages.warning_different_checksum,
				new Object[] { URIUtil.toUnencodedString(baseline.getLocation()),
						URIUtil.toUnencodedString(repo.getLocation()), "SHA-256", descriptor1 });

		assertLogContains(msg);
	}

	private Map<String, String> getSliceProperties() {
		Map<String, String> p = new HashMap<>();
		p.put("osgi.os", "win32");
		p.put("osgi.ws", "win32");
		p.put("osgi.arch", "x86");
		return p;
	}

	protected AntTaskElement createSlicingOption(Boolean includeOptional, Boolean includeNonGreedy, Boolean followStrict, Boolean includeFeatures, String platformFilter, String filter) {
		AntTaskElement slicing = new AntTaskElement("slicingoptions");
		if (followStrict != null)
			slicing.addAttribute("followstrict", followStrict.toString());
		if (includeFeatures != null)
			slicing.addAttribute("includefeatures", includeFeatures.toString());
		if (includeNonGreedy != null)
			slicing.addAttribute("includenongreedy", includeNonGreedy.toString());
		if (includeOptional != null)
			slicing.addAttribute("includeoptional", includeOptional.toString());
		if (platformFilter != null)
			slicing.addAttribute("platformfilter", platformFilter);
		if (filter != null)
			slicing.addAttribute("filter", filter);
		return slicing;
	}

	/*
	 * Create an IU for a descriptor and the IU+descriptor to the specified repo
	 */
	protected IArtifactRepository createRepositoryWithIU(URI repoLocation, IArtifactDescriptor descriptor) throws ProvisionException {
		IArtifactRepository artifactRepository = getArtifactRepositoryManager().createRepository(repoLocation, "Repo 1", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		artifactRepository.addDescriptor(descriptor, new NullProgressMonitor());

		IMetadataRepository metaRepo = getMetadataRepositoryManager().createRepository(repoLocation, "Repo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		InstallableUnit iu = new InstallableUnit();
		iu.setId(descriptor.getArtifactKey().getId() + "IU");
		iu.setVersion(descriptor.getArtifactKey().getVersion());
		iu.setArtifacts(new IArtifactKey[] {descriptor.getArtifactKey()});
		metaRepo.addInstallableUnits(Arrays.asList((IInstallableUnit) iu));

		return artifactRepository;
	}

	/*
	 * Get the number of ArtifactKeys in a repository
	 */
	protected int getArtifactKeyCount(IQueryResult<IInstallableUnit> ius) {
		int count = 0;
		for (IInstallableUnit iu : ius)
			count += iu.getArtifacts().size();
		return count;
	}

	/*
	 * Get the number of IUs in a repository
	 */
	protected int getIUCount(URI location) {
		try {
			return queryResultSize(getMetadataRepositoryManager().loadRepository(location, null).query(QueryUtil.createIUAnyQuery(), null));
		} catch (ProvisionException e) {
			fail("Failed to load repository " + URIUtil.toUnencodedString(location) + " for ArtifactDescriptor count");
			return -1;
		}
	}

	/*
	 * Add all IUs to the parent element
	 */
	protected void addAllIUs(AntTaskElement parent, IMetadataRepository repo) {
		IQueryResult<IInstallableUnit> queryResult = repo.query(QueryUtil.createIUAnyQuery(), null);

		for (IInstallableUnit iu : queryResult) {
			parent.addElement(createIUElement(iu));
		}
	}

	/*
	 * Create the base mirror task & add it to the script
	 */
	protected AntTaskElement createMirrorTask(String type) {
		AntTaskElement mirror = new AntTaskElement(MIRROR_TASK);
		mirror.addElement(getRepositoryElement(destinationRepo, type));
		addTask(mirror);
		return mirror;
	}

	/*
	 * Create a source element with the specified repositories
	 */
	protected AntTaskElement createSourceElement(URI artifact, URI metadata) {
		AntTaskElement source = new AntTaskElement("source");
		if (artifact != null)
			source.addElement(getRepositoryElement(artifact, AbstractAntProvisioningTest.TYPE_ARTIFACT));
		if (metadata != null)
			source.addElement(getRepositoryElement(metadata, AbstractAntProvisioningTest.TYPE_METADATA));
		return source;
	}
}
