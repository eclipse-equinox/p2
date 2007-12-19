/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;

public class SimpleArtifactRepositoryTest extends TestCase {

	public void testGetActualLocation1() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository");
		assertEquals(new URL(base + "/artifacts.xml"), SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocation2() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository/");
		assertEquals(new URL(base + "artifacts.xml"), SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocation3() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository/artifacts.xml");
		assertEquals(base, SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocationGzip1() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository");
		assertEquals(new URL(base + "/artifacts.xml.gzip"), SimpleArtifactRepository.getActualLocation(base, true));
	}

	public void testGetActualLocationGzip2() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository/");
		assertEquals(new URL(base + "artifacts.xml.gzip"), SimpleArtifactRepository.getActualLocation(base, true));
	}

	public void testGetActualLocationGzip3() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository/artifacts.xml.gzip");
		assertEquals(base, SimpleArtifactRepository.getActualLocation(base, true));
	}

}
