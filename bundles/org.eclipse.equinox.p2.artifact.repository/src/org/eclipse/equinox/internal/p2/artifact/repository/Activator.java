/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.artifact.repository"; //$NON-NLS-1$
	public static final String REPO_PROVIDER_XPT = ID + '.' + "artifactRepositories"; //$NON-NLS-1$

	private Map<URI, Location> locationCache = null;

	private static BundleContext context;
	private static Activator instance;

	public static BundleContext getContext() {
		return Activator.context;
	}

	public void start(BundleContext aContext) throws Exception {
		Activator.context = aContext;
		Activator.instance = this;
		this.locationCache = new HashMap<URI, Location>();
	}

	public void stop(BundleContext aContext) throws Exception {
		Activator.context = null;
		Activator.instance = null;
		this.locationCache = null;
	}

	public static Activator getInstance() {
		return Activator.instance;
	}

	/**
	 * Returns the lock location for a given artifact repository
	 * @param repositoryLocation A URI pointing to an artifact repository.  Currently only
	 * file:// repositories are supported
	 * @return The Location that can be locked when using an artifact repository
	 * @throws IOException Thrown if a Location can not be created
	 */
	public synchronized Location getLockLocation(URI repositoryLocation) throws IOException {
		if (locationCache.containsKey(repositoryLocation)) {
			return locationCache.get(repositoryLocation);
		}
		Location anyLoc = (Location) ServiceHelper.getService(Activator.getContext(), Location.class.getName());
		File repositoryFile = URIUtil.toFile(repositoryLocation);
		Location location = anyLoc.createLocation(null, getLockFile(repositoryLocation).toURL(), !repositoryFile.canWrite());
		location.set(getLockFile(repositoryLocation).toURL(), false);
		locationCache.put(repositoryLocation, location);
		return location;
	}

	private File getLockFile(URI repositoryLocation) throws IOException {
		if (!URIUtil.isFileURI(repositoryLocation)) {
			throw new IOException("Cannot lock a non file based repository"); //$NON-NLS-1$
		}
		URI result = URIUtil.append(repositoryLocation, ".artifactlock"); //$NON-NLS-1$
		return URIUtil.toFile(result);
	}

}
