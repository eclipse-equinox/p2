/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import org.eclipse.core.runtime.Status;

/**
 * A status object that optionally reports additional information about the
 * result of a download.
 */
public class DownloadStatus extends Status {
	public static final long UNKNOWN_RATE = -1;

	private long speed = UNKNOWN_RATE;

	/**
	 * Constructs a new DownloadStatus with the given attributes.
	 */
	public DownloadStatus(int severity, String pluginId, int code, String message, Throwable exception) {
		super(severity, pluginId, code, message, exception);
	}

	/**
	 * Constructs a new DownloadStatus with the given attributes.
	 */
	public DownloadStatus(int severity, String pluginId, String message) {
		super(severity, pluginId, message);
	}

	/**
	 * Constructs a new DownloadStatus with the given attributes.
	 */
	public DownloadStatus(int severity, String pluginId, String message, Throwable exception) {
		super(severity, pluginId, message, exception);
	}

	/**
	 * Returns the download rate in bytes per second.  If the rate is unknown,
	 * @{link {@link #UNKNOWN_RATE}} is returned.
	 * @return the download rate in bytes per second
	 */
	public long getTransferRate() {
		return speed;
	}

	/**
	 * Sets the download rate of the transfer in bytes per second.
	 * @param rate The download rate in bytes per second
	 */
	public void setTransferRate(long rate) {
		this.speed = rate;
	}
}
