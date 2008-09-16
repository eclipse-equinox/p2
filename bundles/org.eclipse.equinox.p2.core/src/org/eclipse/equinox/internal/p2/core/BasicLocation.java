/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;

/**
 * Internal class.
 */
public class BasicLocation implements AgentLocation {

	private URL location = null;
	private URL defaultValue;

	public BasicLocation(String property, URL defaultValue, boolean isReadOnly) {
		super();
		this.defaultValue = defaultValue;
	}

	public synchronized URL getURL() {
		if (location == null && defaultValue != null)
			setURL(defaultValue, false);
		return location;
	}

	/**
	 * @deprecated
	 */
	public synchronized boolean setURL(URL value, boolean lock) {
		//		if (location != null)
		//			throw new IllegalStateException(Messages.ECLIPSE_CANNOT_CHANGE_LOCATION);
		////		File file = null;
		////		if (value.getProtocol().equalsIgnoreCase("file")) { //$NON-NLS-1$
		////			try {
		////				String basePath = new File(value.getFile()).getCanonicalPath();
		////				value = new URL("file:" + basePath); //$NON-NLS-1$
		////			} catch (IOException e) {
		////				// do nothing just use the original value
		////			}
		////			file = new File(value.getFile(), LOCK_FILENAME);
		////		}
		//		lock = lock && !isReadOnly;
		//		if (lock) {
		//			try {
		//				if (!lock(file))
		//					return false;
		//			} catch (IOException e) {
		//				return false;
		//			}
		//		}
		//		lockFile = file;
		location = value;
		//		LocationManager.buildURL(value.toExternalForm(), true);
		//		if (property != null)
		//			System.setProperty(property, location.toExternalForm());
		return lock;
	}

	public boolean set(URL value, boolean lock) {
		location = value;
		return lock;
	}

	public URL getArtifactRepositoryURL() {
		//the cache is a co-located repository
		return getMetadataRepositoryURL();
	}

	public URL getMetadataRepositoryURL() {
		try {
			return new URL(getDataArea(Activator.ID), "cache/"); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}

	public URL getDataArea(String touchpointId) {
		try {
			return new URL(getURL(), touchpointId + '/');
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
}
