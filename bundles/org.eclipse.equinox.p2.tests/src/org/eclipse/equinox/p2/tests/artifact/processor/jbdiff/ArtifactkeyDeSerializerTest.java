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
package org.eclipse.equinox.p2.tests.artifact.processor.jbdiff;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.artifact.processors.jbdiff.ArtifactKeyDeSerializer;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.osgi.framework.Version;

/**
 * Test <code>ArtifactkeyDeSerializer</code>
 */
public class ArtifactkeyDeSerializerTest extends TestCase {

	public void testSerialize() {
		IArtifactKey key = new TestArtifactKey("namespace", "classifier", "identifier", new Version("1.0"));
		assertEquals("namespace,classifier,identifier,1.0.0", ArtifactKeyDeSerializer.serialize(key));
	}

	public void testSerializeEmptyNamespace() {
		IArtifactKey key = new TestArtifactKey("", "classifier", "identifier", new Version("1.0"));
		assertEquals(",classifier,identifier,1.0.0", ArtifactKeyDeSerializer.serialize(key));
	}

	public void testDeserialize() {
		IArtifactKey key = ArtifactKeyDeSerializer.deserialize("namespace,classifier,identifier,1.0.0");
		assertNotNull(key);
		assertEquals("namespace", key.getNamespace());
		assertEquals("classifier", key.getClassifier());
		assertEquals("identifier", key.getId());
		assertEquals(new Version("1.0"), key.getVersion());
	}

	public void testDeserializeEmptyNamespace() {
		IArtifactKey key = ArtifactKeyDeSerializer.deserialize(",classifier,identifier,1.0.0");
		assertNotNull(key);
		assertEquals("", key.getNamespace());
		assertEquals("classifier", key.getClassifier());
		assertEquals("identifier", key.getId());
		assertEquals(new Version("1.0"), key.getVersion());
	}

	public void testDeserializeEmptyClassifier() {
		IArtifactKey key = ArtifactKeyDeSerializer.deserialize("namespace,,identifier,1.0.0");
		assertNotNull(key);
		assertEquals("namespace", key.getNamespace());
		assertEquals("", key.getClassifier());
		assertEquals("identifier", key.getId());
		assertEquals(new Version("1.0"), key.getVersion());
	}

	public void testDeserializeEmptyIdentifier() {
		IArtifactKey key = ArtifactKeyDeSerializer.deserialize("namespace,classifier,,1.0.0");
		assertNotNull(key);
		assertEquals("namespace", key.getNamespace());
		assertEquals("classifier", key.getClassifier());
		assertEquals("", key.getId());
		assertEquals(new Version("1.0"), key.getVersion());
	}

	public void testDeserializeEmptyVersion() {
		IArtifactKey key = ArtifactKeyDeSerializer.deserialize("namespace,classifier,identifier,");
		assertNotNull(key);
		assertEquals("namespace", key.getNamespace());
		assertEquals("classifier", key.getClassifier());
		assertEquals("identifier", key.getId());
		assertEquals(new Version("0.0"), key.getVersion());
	}

	public void testDeserializeEmptyEverything() {
		IArtifactKey key = ArtifactKeyDeSerializer.deserialize(",,,");
		assertNotNull(key);
		assertEquals("", key.getNamespace());
		assertEquals("", key.getClassifier());
		assertEquals("", key.getId());
		assertEquals(new Version("0.0"), key.getVersion());
	}

	public void testDeserializeTooFewPartsI() {
		try {
			ArtifactKeyDeSerializer.deserialize(",,");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testDeserializeTooMuchPartsI() {
		try {
			ArtifactKeyDeSerializer.deserialize(",,,,");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testDeserializeTooFewPartsII() {
		try {
			ArtifactKeyDeSerializer.deserialize("namespace,classifier,1.0.0");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}
}
