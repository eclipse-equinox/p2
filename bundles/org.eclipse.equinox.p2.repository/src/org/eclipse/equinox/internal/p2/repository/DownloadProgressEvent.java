/*******************************************************************************
 * Copyright (c) 2012 Wind River and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.net.URI;
import java.util.EventObject;

public class DownloadProgressEvent extends EventObject {

	private static final long serialVersionUID = -7880532297074721824L;
	private ProgressStatistics stat;

	public DownloadProgressEvent(ProgressStatistics stat) {
		super(stat);
		this.stat = stat;
	}

	public String getFileName() {
		return stat.m_fileName;
	}

	public URI getDownloadURI() {
		return stat.m_uri;
	}

	public long getAverageSpeed() {
		return stat.getAverageSpeed();
	}

	public long getRecentSpeed() {
		return stat.getRecentSpeed();
	}

	public long getFinished() {
		return stat.m_current;
	}

	public double getPercentage() {
		return stat.getPercentage();
	}
}
