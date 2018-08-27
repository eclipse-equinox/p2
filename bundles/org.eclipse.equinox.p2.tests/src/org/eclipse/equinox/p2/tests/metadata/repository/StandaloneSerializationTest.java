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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import junit.framework.TestCase;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.io.IUDeserializer;
import org.eclipse.equinox.p2.metadata.io.IUSerializer;

public class StandaloneSerializationTest extends TestCase {
	public void testNothingToWrite() {
		try {
			File f = File.createTempFile(getName(), "iu");
			try (OutputStream os = new FileOutputStream(f)) {
				new IUSerializer(os).write(Collections.EMPTY_LIST);
			}
			assertTrue(f.length() > 0);
			f.delete();
		} catch (FileNotFoundException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (UnsupportedEncodingException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (IOException e) {
			fail("problem writing: " + e.getCause().getMessage());
		}
	}

	public void testNoContent() {
		//Write file w/o content
		File f = null;
		try {
			f = File.createTempFile(getName(), "iu");
			try (OutputStream os = new FileOutputStream(f)) {
				new IUSerializer(os).write(Collections.EMPTY_LIST);
			}
		} catch (FileNotFoundException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (UnsupportedEncodingException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (IOException e) {
			fail("problem writing: " + e.getCause().getMessage());
		}

		//Read file written
		boolean exceptionRaised = false;
		try (InputStream is = new FileInputStream(f)) {
			Collection<IInstallableUnit> ius = new IUDeserializer().read(is);
			assertEquals(0, ius.size());
		} catch (FileNotFoundException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (UnsupportedEncodingException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (IOException e) {
			exceptionRaised = true;
		}
		assertTrue(exceptionRaised);

		f.delete();
	}

	public void testWritingThenLoading() {
		MetadataFactory.InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId("foo");
		iu.setVersion(Version.create("1.0.0"));

		MetadataFactory.InstallableUnitDescription iu2 = new MetadataFactory.InstallableUnitDescription();
		iu2.setId("bar");
		iu2.setVersion(Version.create("1.0.0"));
		Collection<IInstallableUnit> ius = new ArrayList<>();
		ius.add(MetadataFactory.createInstallableUnit(iu));
		ius.add(MetadataFactory.createInstallableUnit(iu2));

		File f = null;
		try {
			f = File.createTempFile(getName(), "iu");
			try (OutputStream os = new FileOutputStream(f)) {
				new IUSerializer(os).write(ius);
			}
		} catch (FileNotFoundException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (UnsupportedEncodingException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (IOException e) {
			fail("problem writing: " + e.getCause().getMessage());
		}

		try (InputStream is = new FileInputStream(f)) {
			assertEquals(2, new IUDeserializer().read(is).size());
		} catch (FileNotFoundException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (UnsupportedEncodingException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} catch (IOException e) {
			fail("problem writing: " + e.getCause().getMessage());
		} finally {
			f.delete();
		}
	}
}
