/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.jarprocessor.ant;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.SignJar;
import org.eclipse.equinox.internal.p2.jarprocessor.SignCommandStep;

public class AntSignCommand extends SignCommandStep {
	private final Project project;
	private final Properties jarSignerArguments;
	private final String antTaskName;

	public AntSignCommand(Properties options, Properties signArguments, Project project, String antTaskName, String command, boolean verbose) {
		super(options, command, verbose);
		this.project = project;
		this.jarSignerArguments = signArguments;
		this.antTaskName = antTaskName;
	}

	@Override
	public File postProcess(File input, File workingDirectory, List<Properties> containers) {
		if (command != null && input != null && shouldSign(input, containers)) {
			execute(input);
		}
		return null;
	}

	private void execute(File input) {
		try {
			SignJar jarSigner = new SignJar();
			jarSigner.setJar(input);
			jarSigner.setAlias(jarSignerArguments.getProperty(JarProcessorTask.ALIAS));
			jarSigner.setKeystore(jarSignerArguments.getProperty(JarProcessorTask.KEYSTORE));
			jarSigner.setStorepass(jarSignerArguments.getProperty(JarProcessorTask.STOREPASS));
			jarSigner.setKeypass(jarSignerArguments.getProperty(JarProcessorTask.KEYPASS));
			jarSigner.setProject(project);
			jarSigner.setTaskName(antTaskName);
			jarSigner.execute();
		} catch (BuildException e) {
			if (e.getCause() instanceof IOException) {
				throw new BuildException("The jarsigner could not be found. Make sure to run with the build with a JDK.", e); //$NON-NLS-1$
			}
			throw e;
		}
	}
}
