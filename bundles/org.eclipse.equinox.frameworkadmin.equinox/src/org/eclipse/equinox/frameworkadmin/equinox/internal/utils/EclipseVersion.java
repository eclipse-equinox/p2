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
package org.eclipse.equinox.frameworkadmin.equinox.internal.utils;

import java.util.StringTokenizer;

public class EclipseVersion implements Comparable {
	int major = 0;
	int minor = 0;
	int service = 0;
	String qualifier = null;

	public EclipseVersion(String version) {
		StringTokenizer tok = new StringTokenizer(version, ".");
		if (!tok.hasMoreTokens())
			return;
		this.major = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.minor = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.service = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.qualifier = tok.nextToken();
	}

	public int compareTo(Object obj) {
		EclipseVersion target = (EclipseVersion) obj;
		if (target.major > this.major)
			return -1;
		if (target.major < this.major)
			return 1;
		if (target.minor > this.minor)
			return -1;
		if (target.minor < this.minor)
			return 1;
		if (target.service > this.service)
			return -1;
		if (target.service < this.service)
			return 1;
		return 0;
	}

}
