/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.updatesite;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.MergeResultsAction;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.StringBufferStream;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.TestMetadataRepository;

/**
 * Tests for {@link org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction}.
 */
public class SiteXMLActionTest extends AbstractProvisioningTest {
	private TestMetadataRepository metadataRepository;
	private IPublisherResult actionResult;
	private URI siteLocation;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		actionResult = new PublisherResult();
		PublisherInfo info = new PublisherInfo();
		metadataRepository = new TestMetadataRepository(getAgent(), new IInstallableUnit[0]);
		info.setMetadataRepository(metadataRepository);
		File siteLocationFile = TestData.getFile("updatesite", "SiteXMLActionTest/site.xml");
		siteLocation = siteLocationFile.toURI();

		FeaturesAction featuresAction = new FeaturesAction(new File[] {TestData.getFile("updatesite", "SiteXMLActionTest")});
		BundlesAction bundlesAction = new BundlesAction(new File[] {TestData.getFile("updatesite", "SiteXMLActionTest")});
		IPublisherAction publishAction = new MergeResultsAction(new IPublisherAction[] {bundlesAction, featuresAction}, IPublisherResult.MERGE_ALL_NON_ROOT);
		publishAction.perform(info, actionResult, new NullProgressMonitor());

		SiteXMLAction action = new SiteXMLAction(siteLocation, null);
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			action.perform(info, actionResult, getMonitor());
		} finally {
			System.setOut(out);
		}
	}

	public void testQualifier() {
		IQueryResult<IInstallableUnit> results = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		Iterator<IInstallableUnit> iter = results.iterator();
		while (iter.hasNext()) {
			IInstallableUnit unit = iter.next();
			String sitelocation = URIUtil.toUnencodedString(siteLocation);
			assertTrue("1.0", unit.getId().startsWith(sitelocation));
			assertEquals("2.0", "Test Category Label", unit.getProperty(IInstallableUnit.PROP_NAME));

			Collection<IProvidedCapability> provided = unit.getProvidedCapabilities();
			assertEquals(1, provided.size());
			assertTrue(provided.iterator().next().getName().startsWith(sitelocation));
			assertEquals(provided.iterator().next().getVersion(), unit.getVersion());
		}
	}

	/**
	 * Tests that associate sites are generated correctly.
	 */
	public void testAssociateSite() {
		Collection<IRepositoryReference> references = metadataRepository.getReferences();
		assertEquals("1.0", 2, references.size());
		boolean metadataFound = false, artifactFound = false;
		for (IRepositoryReference ref : references) {
			assertEquals("1.1", "https://download.eclipse.org/eclipse/updates/4.21", ref.getLocation().toString());
			assertEquals("1.2", IRepository.ENABLED, ref.getOptions());
			assertEquals("1.3", "Eclipse Project Update Site", ref.getNickname());

			if (ref.getType() == IRepository.TYPE_METADATA) {
				metadataFound = true;
			} else if (ref.getType() == IRepository.TYPE_ARTIFACT) {
				artifactFound = true;
			}
		}
		assertTrue("1.3", metadataFound);
		assertTrue("1.4", artifactFound);
	}

	public void testMirrorsURL() {
		String mirrorsURL = metadataRepository.getProperties().get(IRepository.PROP_MIRRORS_URL);
		assertEquals("1.0", "https://www.eclipse.org/downloads/download.php?file=/eclipse/updates/4.21&format=xml",
				mirrorsURL);
	}

	public void testBundleInCategory() {
		IQueryResult<IInstallableUnit> results = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		Iterator<IInstallableUnit> iter = results.iterator();

		IInstallableUnit unit = iter.next();
		IQuery<IInstallableUnit> memberQuery = QueryUtil.createIUCategoryMemberQuery(unit);
		IQueryResult<IInstallableUnit> categoryMembers = actionResult.query(memberQuery, new NullProgressMonitor());
		Set<String> membersId = new HashSet<>();
		for (IInstallableUnit iu : categoryMembers.toUnmodifiableSet()) {
			membersId.add(iu.getId());
		}
		assertEquals("1.0", 2, membersId.size());
		assertTrue("2.0", membersId.contains("test.bundle"));

	}
}
