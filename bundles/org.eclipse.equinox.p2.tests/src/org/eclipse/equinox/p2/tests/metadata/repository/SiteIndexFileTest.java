/******************************************************************************* 
* Copyright (c) 2010, 2015 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   Red Hat Inc. - Bug 460967
******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.*;
import org.eclipse.equinox.internal.p2.repository.helpers.LocationProperties;
import org.eclipse.equinox.internal.p2.updatesite.metadata.UpdateSiteMetadataRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.*;

/**
 * These tests are used to verify the index.p2 file in a variety 
 * of different situations
 */
public class SiteIndexFileTest extends AbstractProvisioningTest {

	class VisibleMetadataRepositoryManager extends MetadataRepositoryManager {
		public VisibleMetadataRepositoryManager() {
			super(ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgent.class));
		}

		public String[] sortSuffixes(String[] suffixes, URI location, String[] preferredOrder) {
			return super.sortSuffixes(suffixes, location, preferredOrder);
		}
	}

	private void assertArrayOrder(String[] expected, String[] actual) {
		assertEquals(expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], actual[i]);
		}
	}

	/*
	 * Tests that the sort method works in a simple case
	 */
	public void testSortOrderSimple() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"first", "second", "third"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), null);
		assertArrayOrder(new String[] {"first", "second", "third"}, suffixes);
	}

	/*
	 * Tests that the sort method works in a simple case
	 */
	public void testSortOrderSimple2() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"first", "second", "third"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"first", "second", "third"});
		assertArrayOrder(new String[] {"first", "second", "third"}, suffixes);
	}

	/*
	 * Tests that the sort method works in a simple case
	 */
	public void testSortOrderSimple3() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"first"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"first", "second", "third"});
		assertArrayOrder(new String[] {"first"}, suffixes);
	}

	/*
	 * Tests that sort method works in a simple case
	 */
	public void testSortOrderSimple4() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"first", "second", "third"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"first"});
		assertArrayOrder(new String[] {"first", "second", "third"}, suffixes);
	}

	/*
	 * Tests that the sort method works in a simple case
	 */
	public void testSortOrderSimple5() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"first", "second", "third"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"foo", "bar"});
		assertArrayOrder(new String[] {"first", "second", "third"}, suffixes);
	}

	/*
	 * Tests that the sort method works when the elements are reversed
	 */
	public void testSortOrderReverse() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"first", "second", "third"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"third", "second", "first"});
		assertArrayOrder(new String[] {"third", "second", "first"}, suffixes);
	}

	/*
	 * Tests that the sort method works when the elements are reversed
	 */
	public void testSortOrderReverse2() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"first", "second", "third"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"third"});
		assertArrayOrder(new String[] {"third", "first", "second"}, suffixes);
	}

	/*
	 * Tests the sort method for a more complex case
	 */
	public void testSortOrderComplex() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"a", "b", "c", "d", "e", "f"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"b", "e", "f"});
		assertArrayOrder(new String[] {"b", "e", "f", "a", "c", "d"}, suffixes);
	}

	/*
	 * Tests that when extra elements are in the preferred order, they are ignored
	 */
	public void testSortOrderAdditional() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"a", "b", "c", "d", "e", "f"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"first", "b", "second", "e", "third", "f"});
		assertArrayOrder(new String[] {"b", "e", "f", "a", "c", "d"}, suffixes);
	}

	/*
	 * Tests that when duplicate elements are listed, they are handled properly
	 */
	public void testSortOrderDuplicate() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"a", "b", "c", "d", "e", "f"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"a", "b", "a"});
		assertArrayOrder(new String[] {"a", "b", "c", "d", "e", "f"}, suffixes);
	}

	/*
	 * Tests that the STOP token is handled properly
	 */
	public void testSortOrderStop() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"a", "b", "c", "d", "e", "f"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"a", "!"});
		assertArrayOrder(new String[] {"a"}, suffixes);
	}

	/*
	 * Tests that the STOP token is handled properly when it's the first element in the list
	 */
	public void testSortOrderStopEmpty() throws URISyntaxException {
		VisibleMetadataRepositoryManager repositoryManager = new VisibleMetadataRepositoryManager();
		String[] suffixes = {"a", "b", "c", "d", "e", "f"};
		suffixes = repositoryManager.sortSuffixes(suffixes, new URI("http://foo"), new String[] {"!"});
		assertArrayOrder(new String[] {}, suffixes);
	}

	/*
	 * Tests that when no index.p2 file is specified, things work just fine
	 */
	public void testNoIndex() throws Exception {
		LocationProperties locationProperties = LocationProperties.create(null);
		assertNotNull(locationProperties);
		assertFalse(locationProperties.exists());
	}

	/*
	 * Tests that when a bad index.p2 file is specified, things work just fine
	 */
	public void testBadIndex1() throws Exception {
		File indexFile = TestData.getFile("metadataRepo/indexfiles", "badIndex.p2");
		InputStream inputStream = new FileInputStream(indexFile);

		PrintStream out = System.out;
		LocationProperties locationProperties = null;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			locationProperties = LocationProperties.create(inputStream);
		} finally {
			System.setOut(out);
		}
		assertNotNull(locationProperties);
		assertFalse(locationProperties.exists());
	}

	/*
	 * Tests that when a bad index.p2 file is specified, things work just fine
	 */
	public void testBadIndex2() throws Exception {
		File indexFile = TestData.getFile("metadataRepo/indexfiles", "badIndex2.p2");
		InputStream inputStream = new FileInputStream(indexFile);
		LocationProperties locationProperties = LocationProperties.create(inputStream);
		assertNotNull(locationProperties);
		assertFalse(locationProperties.exists());
	}

	/*
	 * Tests that a simple index.p2 file is read and used properly
	 */
	public void testSimpleIndexV1() throws Exception {
		File indexFile = TestData.getFile("metadataRepo/indexfiles", "simpleIndexV1.p2");
		InputStream inputStream = new FileInputStream(indexFile);
		LocationProperties locationProperties = LocationProperties.create(inputStream);
		assertNotNull("1.0", locationProperties);
		assertEquals("1.1", Version.createOSGi(1, 0, 0), locationProperties.getVersion());
		assertEquals("1.2", 0, locationProperties.getMetadataFactorySearchOrder().length);
	}

	/*
	 * Tests that a simple index.p2 file is read and used properly
	 */
	public void testSimpleIndex2V1() throws Exception {
		File indexFile = TestData.getFile("metadataRepo/indexfiles", "simpleIndex2_V1.p2");
		InputStream inputStream = new FileInputStream(indexFile);
		LocationProperties locationProperties = LocationProperties.create(inputStream);
		assertNotNull("1.0", locationProperties);
		assertEquals("1.1", Version.createOSGi(1, 0, 0), locationProperties.getVersion());
		assertEquals("1.2", 0, locationProperties.getMetadataFactorySearchOrder().length);
	}

	/*
	 * Tests that a simple index.p2 file is read and used properly
	 */
	public void testSimpleIndex3V1() throws Exception {
		File indexFile = TestData.getFile("metadataRepo/indexfiles", "simpleIndex3_V1.p2");
		InputStream inputStream = new FileInputStream(indexFile);
		LocationProperties locationProperties = LocationProperties.create(inputStream);
		assertNotNull("1.0", locationProperties);
		assertEquals("1.1", Version.createOSGi(1, 0, 0), locationProperties.getVersion());
		assertEquals("1.2", 3, locationProperties.getMetadataFactorySearchOrder().length);
		assertEquals("1.4", 3, locationProperties.getArtifactFactorySearchOrder().length);
		assertArrayOrder(new String[] {"bar", "foo", "!"}, locationProperties.getMetadataFactorySearchOrder());
		assertArrayOrder(new String[] {"foo", "bar", "!"}, locationProperties.getArtifactFactorySearchOrder());
	}

	/*
	 * Tests that the metadata repository manager can read a simple index.p2 file
	 */
	public void testSingleRepository1() throws Exception {
		URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "test1").toURI();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			assertTrue(repository instanceof UpdateSiteMetadataRepository);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Adds a query parameter to the end of the URI and tests that p2 can properly 
	 * find the p2.index file.
	 */
	public void testSingleRepositoryWithQueryParams() throws Exception {
		URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "test1").toURI();
		URI repositoryLocationWithParams = new URI(repositoryLocation.toString() + "?parameter=foo");
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocationWithParams, new NullProgressMonitor());
			assertTrue(repository instanceof UpdateSiteMetadataRepository);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Tests that metadata repository manager can read a simple index.p2 file
	 */
	public void testSingleRepository2() throws Exception {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "test2").toURI();
			IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			assertTrue(repository instanceof LocalMetadataRepository);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Tests that when a bad index.p2 file is specified, the metadata repository manager can work just fine
	 */
	public void testBadIndexFileInRepository() throws Exception {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "badtest1").toURI();
			IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			assertTrue(repository instanceof LocalMetadataRepository || repository instanceof UpdateSiteMetadataRepository);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Tests that when a simple index.p2 file is specified, the metadata repository manager can read and handle it just fine
	 */
	public void testMultiRepository1() throws Exception {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "test3").toURI();
			IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			assertTrue(repository instanceof UpdateSiteMetadataRepository);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Tests that when a simple index.p2 file is specified, the metadata repository manager can read and handle it just fine
	 */
	public void testMultiRepository2() throws Exception {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "test4").toURI();
			IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			assertTrue(repository instanceof UpdateSiteMetadataRepository);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Tests that when a simple index.p2 file is specified, the metadata repository manager can read and handle it just fine
	 */
	public void testDuplicateEntries() throws Exception {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "test5").toURI();
			IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			assertTrue(repository instanceof UpdateSiteMetadataRepository);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Tests that the STOP token is handled properly by the metadata repository manager
	 */
	public void testStop1() throws Exception {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "stop1").toURI();
			IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			assertTrue(repository instanceof UpdateSiteMetadataRepository);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Tests that the STOP token is handled properly by the metadata repository manager
	 */
	public void testStop2() throws Exception {
		URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "stop2").toURI();
		try {
			getMetadataRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
		} catch (ProvisionException e) {
			// expected path
			return;
		}
		fail("We should have not been able to load the repository.");
	}

	/*
	 * Tests a composite repository to ensure it loads fine as well as all its children
	 */
	public void testCompositeRepo() throws Exception {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "compositeRepo").toURI();
			IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			assertTrue("1.0", repository instanceof CompositeMetadataRepository);
			CompositeMetadataRepository compositeMetadataRepository = (CompositeMetadataRepository) repository;
			assertEquals("1.1", 2, compositeMetadataRepository.getChildren().size());
			IMetadataRepository child1 = getMetadataRepositoryManager().loadRepository(compositeMetadataRepository.getChildren().get(0), new NullProgressMonitor());
			IMetadataRepository child2 = getMetadataRepositoryManager().loadRepository(compositeMetadataRepository.getChildren().get(1), new NullProgressMonitor());
			assertTrue("1.2", child1 instanceof UpdateSiteMetadataRepository);
			assertTrue("1.2", child2 instanceof LocalMetadataRepository);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Tests that an artifact repository manager can handle an index.p2 file
	 */
	public void testSimpleArtifact() throws Exception {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "artifactTest1").toURI();
			IArtifactRepository repository = getArtifactRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			assertTrue(repository instanceof SimpleArtifactRepository);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Tests that an artifact repository manager can handle an index.p2 file
	 */
	public void testSimpleArtifact2() throws Exception {
		URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "artifactTest2").toURI();
		try {
			getArtifactRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
		} catch (ProvisionException e) {
			// expected path
			return;
		}
		fail("We should have not been able to load the repository.");
	}

	/*
	 * Tests that a location which specifies both an artifact and a metadata repository w/ an index.p2 file 
	 * can fully loaded
	 */
	public void testFullRepository() throws Exception {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI repositoryLocation = TestData.getFile("metadataRepo/multipleRepos", "fullRepository").toURI();
			IMetadataRepository metadataRepository = getMetadataRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			IArtifactRepository artifactRepository = getArtifactRepositoryManager().loadRepository(repositoryLocation, new NullProgressMonitor());
			assertTrue(metadataRepository instanceof UpdateSiteMetadataRepository);
			assertTrue(artifactRepository instanceof SimpleArtifactRepository);
		} finally {
			System.setOut(out);
		}
	}
}
