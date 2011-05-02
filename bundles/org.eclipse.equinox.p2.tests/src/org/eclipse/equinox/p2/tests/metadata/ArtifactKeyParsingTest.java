/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Test <code>ArtifactkeyDeSerializer</code>
 */
public class ArtifactKeyParsingTest extends TestCase {

	public void testSerialize() {
		IArtifactKey key = new ArtifactKey("classifier", "identifier", Version.create("1.0"));
		assertEquals("classifier,identifier,1.0.0", key.toExternalForm());
	}

	public void testSerializeEmptyClassifier() {
		IArtifactKey key = new ArtifactKey("", "identifier", Version.create("1.0"));
		assertEquals(",identifier,1.0.0", key.toExternalForm());
	}

	public void testDeserialize() {
		IArtifactKey key = ArtifactKey.parse("classifier,identifier,1.0.0");
		assertNotNull(key);
		assertEquals("classifier", key.getClassifier());
		assertEquals("identifier", key.getId());
		assertEquals(Version.create("1.0"), key.getVersion());
	}

	public void testDeserializeEmptyClassifier() {
		IArtifactKey key = ArtifactKey.parse(",identifier,1.0.0");
		assertNotNull(key);
		assertEquals("", key.getClassifier());
		assertEquals("identifier", key.getId());
		assertEquals(Version.create("1.0"), key.getVersion());
	}

	public void testDeserializeEmptyIdentifier() {
		IArtifactKey key = ArtifactKey.parse("classifier,,1.0.0");
		assertNotNull(key);
		assertEquals("classifier", key.getClassifier());
		assertEquals("", key.getId());
		assertEquals(Version.create("1.0"), key.getVersion());
	}

	public void testDeserializeEmptyVersion() {
		IArtifactKey key = ArtifactKey.parse("classifier,identifier,");
		assertNotNull(key);
		assertEquals("classifier", key.getClassifier());
		assertEquals("identifier", key.getId());
		assertEquals(Version.create("0.0"), key.getVersion());
	}

	public void testDeserializeEmptyEverything() {
		IArtifactKey key = ArtifactKey.parse(",,");
		assertNotNull(key);
		assertEquals("", key.getClassifier());
		assertEquals("", key.getId());
		assertEquals(Version.create("0.0"), key.getVersion());
	}

	public void testDeserializeTooFewPartsI() {
		try {
			ArtifactKey.parse("");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testDeserializeTooManyPartsI() {
		try {
			ArtifactKey.parse(",,,,");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testDeserializeTooFewPartsII() {
		try {
			ArtifactKey.parse("classifier");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}
}
