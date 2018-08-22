/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.io.File;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;

public class RootFilesAdvice extends AbstractAdvice implements IRootFilesAdvice {
	private File root;
	private File[] excludedFiles;
	private File[] includedFiles;
	private String configSpec;

	public RootFilesAdvice(File root, File[] includedFiles, File[] excludedFiles, String configSpec) {
		this.root = root;
		this.excludedFiles = excludedFiles;
		this.includedFiles = includedFiles;
		this.configSpec = configSpec;
	}

	@Override
	public File getRoot() {
		return root;
	}

	@Override
	protected String getConfigSpec() {
		return configSpec;
	}

	@Override
	public File[] getExcludedFiles() {
		return excludedFiles;
	}

	@Override
	public File[] getIncludedFiles() {
		return includedFiles;
	}

}
