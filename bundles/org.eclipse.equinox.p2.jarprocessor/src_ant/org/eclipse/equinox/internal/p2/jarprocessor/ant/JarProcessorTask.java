/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessorExecutor.Options;

/**
 * This task provides massaging facilities for jar files. It supports: signing,
 * unsigning, normalization, packing -
 */
public class JarProcessorTask extends Task {
	private final Options options = new Options();
	private final Properties signArgs = new Properties();
	private final ArrayList<File> inputFiles = new ArrayList<>();

	public static final String ALIAS = "alias"; //$NON-NLS-1$
	public static final String KEYSTORE = "keystore"; //$NON-NLS-1$
	public static final String STOREPASS = "storepass"; //$NON-NLS-1$
	public static final String KEYPASS = "keypass"; //$NON-NLS-1$
	public static final String UNSIGN = "unsign"; //$NON-NLS-1$
	public static final String SIGN = "sign"; //$NON-NLS-1$

	private static final String FAKE_COMMAND = "fake"; //$NON-NLS-1$

	public void setAlias(String alias) {
		signArgs.setProperty(ALIAS, alias);
	}

	public void setKeystore(String keystore) {
		signArgs.setProperty(KEYSTORE, keystore);
	}

	public void setJar(File jar) {
		options.input = jar;
		options.outputDir = jar.getParentFile().getAbsolutePath();
	}

	public void setInputFolder(File folder) {
		options.input = folder;
		options.outputDir = folder.getAbsolutePath();
	}

	public void setStorepass(String storepass) {
		signArgs.setProperty(STOREPASS, storepass);
	}

	public void setKeypass(String keypass) {
		if (keypass != null && keypass.length() > 0 && !keypass.startsWith("${")) //$NON-NLS-1$
			signArgs.setProperty(KEYPASS, keypass);
	}

	public void setUnsign(boolean unsign) {
		if (unsign) {
			signArgs.put(UNSIGN, Boolean.TRUE.toString());
			options.signCommand = FAKE_COMMAND;
		}
	}

	public void setSign(boolean sign) {
		if (sign) {
			signArgs.put(SIGN, Boolean.TRUE.toString());
			options.signCommand = FAKE_COMMAND;
		}
	}

	private void adjustAndValidateConfiguration() {
		// Check that alias, and storepass are set
		if (options.signCommand != null && signArgs.getProperty(UNSIGN) == null) {
			if (signArgs.getProperty(ALIAS) == null)
				throw new BuildException("Alias must be set"); //$NON-NLS-1$

			if (signArgs.getProperty(STOREPASS) == null)
				throw new BuildException("Storepass must be set"); //$NON-NLS-1$
		}
	}

	@Override
	public void execute() {
		options.processAll = true;
		adjustAndValidateConfiguration();
		AntBasedProcessorExecutor executor = new AntBasedProcessorExecutor(signArgs, getProject(), getTaskName());
		if (inputFiles.size() > 0)
			executor.setInputFiles(inputFiles);
		executor.runJarProcessor(options);
	}

	public void setVerbose(boolean verbose) {
		options.verbose = verbose;
	}

	public void addInputFile(File file) {
		inputFiles.add(file);
	}

	public void addConfiguredFile(FileSet fileset) {
		DirectoryScanner scanner = fileset.getDirectoryScanner(getProject());
		String[] included = scanner.getIncludedFiles();
		for (String included1 : included) {
			addInputFile(new File(scanner.getBasedir(), included1));
		}
		included = scanner.getIncludedDirectories();
		for (String included1 : included) {
			addInputFile(new File(scanner.getBasedir(), included1));
		}
	}
}
