/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumUtilities;
import org.junit.Test;

public class ProduceChecksumTest {

	@Test
	public void testChecksums() throws IOException {
		File tempFile = File.createTempFile("testArtifact", ".tmp");
		tempFile.deleteOnExit();
		try (FileOutputStream fout = new FileOutputStream(tempFile);
				InputStream resource = getClass().getResourceAsStream("testArtifact")) {
			resource.transferTo(fout);
		}
		HashMap<String, String> hashMap = new HashMap<>();
		IStatus status = ChecksumUtilities.calculateChecksums(new File(tempFile.toURI()), hashMap,
				Collections.emptyList());
		assertTrue(status.toString(), status.isOK());
		String md5sum = hashMap.get("md5");
		assertNull("MD5 was computed but should be disabled!", md5sum);
		String sha256sum = hashMap.get("sha-256");
		assertNotNull("SHA256 was not computed!", sha256sum);
		assertEquals("SHA256 mismatch", "39d083c8c75eac51b2c4566cca299b41cc93d5b0313906f5979fbebf1104ff49", sha256sum);
	}
}
