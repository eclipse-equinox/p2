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

import java.io.*;
import java.util.*;
import org.apache.tools.ant.Project;
import org.eclipse.equinox.internal.p2.jarprocessor.unsigner.UnsignCommand;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessor;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessorExecutor;

public class AntBasedProcessorExecutor extends JarProcessorExecutor {
	private final Project project;
	private final Properties signArguments;
	private final String antTaskName;
	private List<File> inputFiles;
	private HashSet<File> filterSet = null;
	private FileFilter baseFilter = null;

	public AntBasedProcessorExecutor(Properties signArguments, Project project, String antTaskName) {
		this.signArguments = signArguments;
		this.project = project;
		this.antTaskName = antTaskName;
	}

	@Override
	protected FileFilter createFileFilter(Options opt) {
		baseFilter = super.createFileFilter(opt);
		if (inputFiles == null || inputFiles.size() == 0)
			return baseFilter;

		filterSet = new HashSet<>();
		filterSet.addAll(inputFiles);
		return pathname -> getFilterSet().contains(pathname);
	}

	protected HashSet<File> getFilterSet() {
		return filterSet;
	}

	@Override
	protected void processDirectory(File input, FileFilter filter, boolean verbose, JarProcessor processor, Properties packProperties) throws FileNotFoundException {
		if (filterSet != null && filterSet.contains(input)) {
			File[] files = input.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory() || baseFilter.accept(files[i]))
					filterSet.add(files[i]);
			}
		}
		super.processDirectory(input, filter, verbose, processor, packProperties);
	}

	@Override
	public void addSignStep(JarProcessor processor, Properties properties, Options opt) {
		if (signArguments.get(JarProcessorTask.UNSIGN) != null)
			processor.addProcessStep(new UnsignCommand(properties, opt.signCommand, opt.verbose));
		if (signArguments.get(JarProcessorTask.SIGN) != null)
			processor.addProcessStep(new AntSignCommand(properties, signArguments, project, antTaskName, opt.signCommand, opt.verbose));
	}

	public void setInputFiles(List<File> inputFiles) {
		this.inputFiles = inputFiles;
	}
}
