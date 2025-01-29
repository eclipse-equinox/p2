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
 *   IBM Corporation - Ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import java.io.File;
import java.util.*;

public class FileSetDescriptor {
	private final String key;
	private String configSpec = null;
	private final HashSet<File> fileset = new HashSet<>();
	private final ArrayList<String[]> permissions = new ArrayList<>();
	private String links = ""; //$NON-NLS-1$

	public FileSetDescriptor(String key, String configSpec) {
		this.key = key;
		this.configSpec = configSpec;
	}

	public void addFiles(File[] files) {
		fileset.addAll(Arrays.asList(files));
	}

	// a permission spec is { <perm>, file patterns }
	public void addPermissions(String[] property) {
		permissions.add(property);
	}

	public void setLinks(String property) {
		links = property;
	}

	public String getConfigSpec() {
		return configSpec;
	}

	public String getKey() {
		return key;
	}

	public String getLinks() {
		return links;
	}

	public String[][] getPermissions() {
		return permissions.toArray(new String[permissions.size()][]);
	}

	public File[] getFiles() {
		return fileset.toArray(new File[fileset.size()]);
	}

	public int size() {
		return fileset.size();
	}
}
