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

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.RepositoryReference;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.tests.*;

/**
 * Tests for {@link org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction}.
 */
public class SiteXMLActionTest extends AbstractProvisioningTest {
	private TestMetadataRepository metadataRepository;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IPublisherResult result = new PublisherResult();
		PublisherInfo info = new PublisherInfo();
		metadataRepository = new TestMetadataRepository(new IInstallableUnit[0]);
		info.setMetadataRepository(metadataRepository);
		URI siteLocation = TestData.getFile("updatesite", "SiteXMLActionTest/site.xml").toURI();
		SiteXMLAction action = new SiteXMLAction(siteLocation);
		action.perform(info, result, getMonitor());
	}

	/**
	 * Tests that associate sites are generated correctly.
	 */
	public void testAssociateSite() {
		Collection references = metadataRepository.getReferences();
		assertEquals("1.0", 2, references.size());
		boolean metadataFound = false, artifactFound = false;
		for (Iterator it = references.iterator(); it.hasNext();) {
			RepositoryReference ref = (RepositoryReference) it.next();
			assertEquals("1.1", "http://download.eclipse.org/eclipse/updates/3.5", ref.Location.toString());
			assertEquals("1.2", IRepository.ENABLED, ref.Options);
			if (ref.Type == IRepository.TYPE_METADATA)
				metadataFound = true;
			else if (ref.Type == IRepository.TYPE_ARTIFACT)
				artifactFound = true;
		}
		assertTrue("1.3", metadataFound);
		assertTrue("1.4", artifactFound);
	}

	public void testMirrorsURL() {
		String mirrorsURL = (String) metadataRepository.getProperties().get(IRepository.PROP_MIRRORS_URL);
		assertEquals("1.0", "http://www.eclipse.org/downloads/download.php?file=/eclipse/updates/3.4&format=xml", mirrorsURL);
	}
}
