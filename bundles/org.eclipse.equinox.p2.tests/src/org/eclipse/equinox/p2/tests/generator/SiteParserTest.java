/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.generator;

import java.io.IOException;
import org.eclipse.equinox.internal.p2.publisher.features.DefaultSiteParser;
import org.eclipse.equinox.internal.p2.publisher.features.SiteModel;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestData;
import org.xml.sax.SAXException;

/**
 * Test the site.xml parser included in the generator.
 */
public class SiteParserTest extends AbstractProvisioningTest {
	public void testEuropaSite() {
		DefaultSiteParser parser = new DefaultSiteParser();
		try {
			SiteModel site = parser.parse(TestData.get("generator", "Europa/site.xml"));
			System.out.println(site);
		} catch (SAXException e) {
			fail("4.99", e);
		} catch (IOException e) {
			fail("4.99", e);
		}
	}
}
