/*******************************************************************************
 *  Copyright (c) 2015, 2018 Mykola Nikishov.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Mykola Nikishov - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.repository;

import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.OSGiVersion;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumHelper;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ChecksumHelperTest {
	@Parameters
	public static Collection<Object[]> generateData() {
		return Arrays.asList(new Object[][] {{IArtifactDescriptor.ARTIFACT_CHECKSUM}, {IArtifactDescriptor.DOWNLOAD_CHECKSUM}});
	}

	@Parameter(0)
	public String property;

	@Test
	public void testGetChecksums() {
		String checksumId = "checksumAlgo";
		String checksumValue = "value";
		ArtifactDescriptor descriptor = new ArtifactDescriptor(new ArtifactKey("", "", new OSGiVersion(1, 1, 1, "")));
		descriptor.setProperty(property.concat(".").concat(checksumId), checksumValue);
		descriptor.setProperty("download.size", "1234");
		Map<String, String> expectedChecksums = new HashMap<>();
		expectedChecksums.put(checksumId, checksumValue);

		Map<String, String> checksums = ChecksumHelper.getChecksums(descriptor, property);

		Assert.assertEquals(expectedChecksums, checksums);
	}

}
