/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.File;
import org.eclipse.equinox.p2.directorywatcher.DirectoryWatcher;

/**
 * @since 1.0
 */
public class SiteWatcher extends DirectoryWatcher {

	static File PLUGINS_DIR;
	static File FEATURES_DIR;

	/**
	 * @param directory
	 */
	public SiteWatcher(File directory) {
		super(directory);
		PLUGINS_DIR = new File(directory, "plugins"); //$NON-NLS-1$
		FEATURES_DIR = new File(directory, "features"); //$NON-NLS-1$
	}

}
