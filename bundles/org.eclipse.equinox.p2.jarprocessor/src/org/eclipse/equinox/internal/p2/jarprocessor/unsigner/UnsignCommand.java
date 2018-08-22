/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.jarprocessor.unsigner;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.eclipse.equinox.internal.p2.jarprocessor.SignCommandStep;

public class UnsignCommand extends SignCommandStep {

	public UnsignCommand(Properties options, String command, boolean verbose) {
		super(options, command, verbose);
	}

	@Override
	public File postProcess(File input, File workingDirectory, List<Properties> containers) {
		if (command != null && input != null && shouldSign(input, containers)) {
			execute(input);
		}
		return null;
	}

	private void execute(File input) {
		Unsigner jarUnsigner = new Unsigner();
		jarUnsigner.setJar(input);
		jarUnsigner.setKeepManifestEntries(false);
		jarUnsigner.execute();
	}
}
