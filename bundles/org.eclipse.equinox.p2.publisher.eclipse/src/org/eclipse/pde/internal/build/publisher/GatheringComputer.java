/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Set;
import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;

public class GatheringComputer implements IPathComputer {
	private static final String PROVIDED_PATH = ":PROVIDED:"; //$NON-NLS-1$
	private final LinkedHashMap<File, String> filesMap = new LinkedHashMap<>();

	@Override
	public IPath computePath(File source) {
		String prefix = filesMap.get(source);

		IPath result = null;
		if (prefix.startsWith(PROVIDED_PATH)) {
			// the desired path is provided in the map
			result = IPath.fromOSString(prefix.substring(10));
		} else {
			//else the map contains a prefix which must be stripped from the path
			result = IPath.fromOSString(source.getAbsolutePath());
			IPath rootPath = IPath.fromOSString(prefix);
			result = result.removeFirstSegments(rootPath.matchingFirstSegments(result));
		}
		return result.setDevice(null);
	}

	@Override
	public void reset() {
		// nothing

	}

	public void addAll(GatheringComputer computer) {
		filesMap.putAll(computer.filesMap);
	}

	public void addFiles(String prefix, String[] files) {
		for (String file : files) {
			filesMap.put(new File(prefix, file), prefix);
		}
	}

	public void addFile(String prefix, String file) {
		filesMap.put(new File(prefix, file), prefix);
	}

	public void addFile(String computedPath, File file) {
		filesMap.put(file, PROVIDED_PATH + computedPath);
	}

	public File[] getFiles() {
		Set<File> keys = filesMap.keySet();
		return keys.toArray(new File[keys.size()]);
	}

	public int size() {
		return filesMap.size();
	}
}