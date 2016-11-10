/*******************************************************************************
 * Copyright (c) 2007, 2018 Mykola Nikishov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mykola Nikishov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.checksum;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.artifact.processors.md5.MD5Verifier;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public class ChecksumUtilities {
	/**
	* When enabled, extract property from the artifact descriptor, create instance of MD5Verifier and add it to steps.
	*/
	public static void addChecksumVerificationStep(boolean enabled, String property, IArtifactDescriptor descriptor, ArrayList<ProcessingStep> steps) {
		if (enabled && descriptor.getProperty(property) != null) {
			MD5Verifier checksumVerifier = new MD5Verifier(descriptor.getProperty(property));
			if (checksumVerifier.getStatus().isOK())
				steps.add(checksumVerifier);
		}
	}

}
