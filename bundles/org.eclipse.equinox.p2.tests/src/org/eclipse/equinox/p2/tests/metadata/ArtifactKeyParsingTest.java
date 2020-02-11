/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.junit.Test;

/**
 * Test <code>ArtifactkeyDeSerializer</code>
 */
public class ArtifactKeyParsingTest {
	@Test
	public void testSerialize() {
		IArtifactKey key = new ArtifactKey("classifier", "identifier", Version.create("1.0"));
		assertEquals("classifier,identifier,1.0.0", key.toExternalForm());
	}

	@Test
	public void testSerializeEmptyClassifier() {
		IArtifactKey key = new ArtifactKey("", "identifier", Version.create("1.0"));
		assertEquals(",identifier,1.0.0", key.toExternalForm());
	}

	@Test
	public void testDeserialize() {
		IArtifactKey key = ArtifactKey.parse("classifier,identifier,1.0.0");
		assertNotNull(key);
		assertEquals("classifier", key.getClassifier());
		assertEquals("identifier", key.getId());
		assertEquals(Version.create("1.0"), key.getVersion());
	}

	@Test
	public void testDeserializeEmptyClassifier() {
		IArtifactKey key = ArtifactKey.parse(",identifier,1.0.0");
		assertNotNull(key);
		assertEquals("", key.getClassifier());
		assertEquals("identifier", key.getId());
		assertEquals(Version.create("1.0"), key.getVersion());
	}

	@Test
	public void testDeserializeEmptyIdentifier() {
		IArtifactKey key = ArtifactKey.parse("classifier,,1.0.0");
		assertNotNull(key);
		assertEquals("classifier", key.getClassifier());
		assertEquals("", key.getId());
		assertEquals(Version.create("1.0"), key.getVersion());
	}

	@Test
	public void testDeserializeEmptyVersion() {
		IArtifactKey key = ArtifactKey.parse("classifier,identifier,");
		assertNotNull(key);
		assertEquals("classifier", key.getClassifier());
		assertEquals("identifier", key.getId());
		assertEquals(Version.create("0.0"), key.getVersion());
	}

	@Test
	public void testDeserializeEmptyEverything() {
		IArtifactKey key = ArtifactKey.parse(",,");
		assertNotNull(key);
		assertEquals("", key.getClassifier());
		assertEquals("", key.getId());
		assertEquals(Version.create("0.0"), key.getVersion());
	}

	@Test
	public void testDeserializeTooFewPartsI() {
		assertThrows(IllegalArgumentException.class, () -> ArtifactKey.parse(""));
	}

	@Test
	public void testDeserializeTooManyPartsI() {
		assertThrows(IllegalArgumentException.class, () -> ArtifactKey.parse(",,,,"));
	}

	@Test
	public void testDeserializeTooFewPartsII() {
		assertThrows(IllegalArgumentException.class, () -> ArtifactKey.parse("classifier"));
	}
}
