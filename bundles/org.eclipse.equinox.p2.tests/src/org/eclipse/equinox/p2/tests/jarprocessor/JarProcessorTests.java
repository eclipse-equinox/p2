/*******************************************************************************
 *  Copyright (c) 2005, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.jarprocessor;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.jarprocessor.verifier.Verifier;
import org.eclipse.equinox.internal.p2.jarprocessor.verifier.VerifyStep;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessor;

@SuppressWarnings("removal")
public class JarProcessorTests extends AbstractProvisioningTest {

	public void testVerifyStep() throws Exception {
		if (!VerifyStep.canVerify())
			return;

		// run verify on
		File workingDir = getTestFolder("testVerifyStep");

		Verifier verifier = new Verifier() {
			@Override
			public void verify(File workingDirectory, String[] input) {
				options = new Options();
				options.verbose = false;
				options.pack = false; // we are verifying during the pack phase.
				options.outputDir = workingDirectory.toString();
				options.input = workingDirectory;

				JarProcessor processor = new JarProcessor();
				processor.setWorkingDirectory(workingDirectory.getAbsolutePath());

				FileFilter filter = pathname -> {
					String name = pathname.getName();
					if (pathname.isFile() && name.endsWith(".jar"))
						if ((!name.contains("source")) && name.startsWith("org.eclipse.equinox.p2"))
							return true;
					return false;
				};
				for (String filename : input) {
					File inputFile = new File(filename);
					if (inputFile.exists()) {
						try {
							process(inputFile, filter, true, processor, null);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					}
				}
			}
		};

		String install = Platform.getInstallLocation().getURL().getPath();
		File plugins = new File(install, "plugins");

		PrintStream oldOut = System.out;
		try (PrintStream newOut = new PrintStream(new FileOutputStream(workingDir + "/out.out"))) {
			System.setOut(newOut);

			verifier.verify(workingDir, new String[] {plugins.getAbsolutePath()});
		} finally {
			System.setOut(oldOut);
		}

	}

}
