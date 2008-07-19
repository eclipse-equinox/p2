/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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

	public File getRoot() {
		return root;
	}

	protected String getConfigSpec() {
		return configSpec;
	}

	public File[] getExcludedFiles() {
		return excludedFiles;
	}

	public File[] getIncludedFiles() {
		return includedFiles;
	}

}
