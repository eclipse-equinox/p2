/*******************************************************************************
 * Copyright (c) 2007 Red Hat Incorporated
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import java.io.IOException;
import org.eclipse.core.runtime.IPath;

public class Permissions {
	public void chmod(String targetDir, String targetFile, String perms) {
		Runtime r = Runtime.getRuntime();
		try {
			r.exec("chmod " + perms + " " + targetDir + IPath.SEPARATOR + targetFile); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IOException e) {
			// FIXME:  we should probably throw some sort of error here
		}
	}

	public void chmod(String target, String targetFile, int perms) {
		chmod(target, targetFile, Integer.toString(perms));
	}
}
