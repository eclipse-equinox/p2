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

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.MergeResultsAction;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.tests.*;

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
		IQueryResult results = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		Iterator iter = results.iterator();
		while (iter.hasNext()) {
			IInstallableUnit unit = (IInstallableUnit) iter.next();
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
		Collection references = metadataRepository.getReferences();
		assertEquals("1.0", 2, references.size());
		boolean metadataFound = false, artifactFound = false;
		for (Iterator it = references.iterator(); it.hasNext();) {
			IRepositoryReference ref = (IRepositoryReference) it.next();
			assertEquals("1.1", "http://download.eclipse.org/eclipse/updates/3.5", ref.getLocation().toString());
			assertEquals("1.2", IRepository.ENABLED, ref.getOptions());
			assertEquals("1.3", "Eclipse Project Update Site", ref.getNickname());

			if (ref.getType() == IRepository.TYPE_METADATA)
				metadataFound = true;
			else if (ref.getType() == IRepository.TYPE_ARTIFACT)
				artifactFound = true;
		}
		assertTrue("1.3", metadataFound);
		assertTrue("1.4", artifactFound);
	}

	public void testMirrorsURL() {
		String mirrorsURL = metadataRepository.getProperties().get(IRepository.PROP_MIRRORS_URL);
		assertEquals("1.0", "http://www.eclipse.org/downloads/download.php?file=/eclipse/updates/3.4&format=xml", mirrorsURL);
	}

	public void testBundleInCategory() {
		IQueryResult results = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		Iterator iter = results.iterator();

		IInstallableUnit unit = (IInstallableUnit) iter.next();
		IQuery<IInstallableUnit> memberQuery = QueryUtil.createIUCategoryMemberQuery(unit);
		IQueryResult<IInstallableUnit> categoryMembers = actionResult.query(memberQuery, new NullProgressMonitor());
		Set<String> membersId = new HashSet<String>();
		for (IInstallableUnit iu : categoryMembers.toUnmodifiableSet()) {
			membersId.add(iu.getId());
		}
		assertEquals("1.0", 2, membersId.size());
		assertTrue("2.0", membersId.contains("test.bundle"));

	}
}
