/*******************************************************************************
 * Copyright (c) 2011, 2018 Sonatype, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sonatype, Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.io.IUDeserializer;
import org.eclipse.equinox.p2.metadata.io.IUSerializer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class StandaloneSerializationTest {
	@Rule
	public TestName name = new TestName();

	@Test
	public void testNothingToWrite() throws IOException {
		File f = File.createTempFile(name.getMethodName(), "iu");
		try (OutputStream os = Files.newOutputStream(f.toPath())) {
			new IUSerializer(os).write(Collections.emptyList());
		}
		assertTrue(f.length() > 0);
		f.delete();
	}

	@Test
	public void testNoContent() throws IOException {
		// Write file w/o content
		File f = File.createTempFile(name.getMethodName(), "iu");
		try (OutputStream os = Files.newOutputStream(f.toPath())) {
			new IUSerializer(os).write(Collections.emptyList());
		}

		// Read file written
		boolean exceptionRaised = false;
		try (InputStream is = Files.newInputStream(f.toPath())) {
			Collection<IInstallableUnit> ius = new IUDeserializer().read(is);
			assertEquals(0, ius.size());
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (IOException e) {
			exceptionRaised = true;
		}
		assertTrue(exceptionRaised);

		f.delete();
	}

	@Test
	public void testWritingThenLoading() throws FileNotFoundException, IOException {
		MetadataFactory.InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId("foo");
		iu.setVersion(Version.create("1.0.0"));

		MetadataFactory.InstallableUnitDescription iu2 = new MetadataFactory.InstallableUnitDescription();
		iu2.setId("bar");
		iu2.setVersion(Version.create("1.0.0"));
		Collection<IInstallableUnit> ius = new ArrayList<>();
		ius.add(MetadataFactory.createInstallableUnit(iu));
		ius.add(MetadataFactory.createInstallableUnit(iu2));

		File f = File.createTempFile(name.getMethodName(), "iu");
		try (OutputStream os = Files.newOutputStream(f.toPath())) {
			new IUSerializer(os).write(ius);
		}

		try (InputStream is = Files.newInputStream(f.toPath())) {
			assertEquals(2, new IUDeserializer().read(is).size());
		} finally {
			f.delete();
		}
	}
}
