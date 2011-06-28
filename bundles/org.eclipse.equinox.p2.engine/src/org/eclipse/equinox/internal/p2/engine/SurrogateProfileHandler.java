/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.net.*;
import java.util.Iterator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ISurrogateProfileHandler;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

public class SurrogateProfileHandler implements ISurrogateProfileHandler {

	private static final String NATIVE_TOUCHPOINT_TYPE = "org.eclipse.equinox.p2.native"; //$NON-NLS-1$
	private static final String PROP_TYPE_ROOT = "org.eclipse.equinox.p2.type.root"; //$NON-NLS-1$
	private static final String P2_ENGINE_DIR = "p2/" + EngineActivator.ID + "/"; //$NON-NLS-1$//$NON-NLS-2$
	private static final String OSGI_INSTALL_AREA = "osgi.install.area"; //$NON-NLS-1$
	private static final String ECLIPSE_INI_IGNORED = "eclipse.ini.ignored"; //$NON-NLS-1$
	private static final String IU_LOCKED = Integer.toString(IInstallableUnit.LOCK_UNINSTALL | IInstallableUnit.LOCK_UPDATE);
	private static final String PROP_SURROGATE = "org.eclipse.equinox.p2.surrogate"; //$NON-NLS-1$
	private static final String PROP_SHARED_TIMESTAMP = "org.eclipse.equinox.p2.shared.timestamp"; //$NON-NLS-1$
	private static final String PROP_BASE = "org.eclipse.equinox.p2.base"; //$NON-NLS-1$
	private static final String PROP_RESOLVE = "org.eclipse.equinox.p2.resolve"; //$NON-NLS-1$
	private static final String OPTIONAL = "OPTIONAL"; //$NON-NLS-1$
	private static final String PROP_INCLUSION_RULES = "org.eclipse.equinox.p2.internal.inclusion.rules"; //$NON-NLS-1$

	private SimpleProfileRegistry profileRegistry;

	private static void addSharedProfileBaseIUs(final IProfile sharedProfile, final Profile userProfile) {
		Query rootIUQuery = new Query() {
			public boolean isMatch(Object candidate) {
				if (candidate instanceof IInstallableUnit) {
					IInstallableUnit iu = (IInstallableUnit) candidate;
					if (Boolean.valueOf(sharedProfile.getInstallableUnitProperty(iu, PROP_TYPE_ROOT)).booleanValue())
						return true;
					if (iu.getTouchpointType().getId().equals(NATIVE_TOUCHPOINT_TYPE))
						return true;
				}
				return false;
			}
		};
		Collector rootIUs = sharedProfile.query(rootIUQuery, new Collector(), null);
		for (Iterator iterator = rootIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			userProfile.addInstallableUnit(iu);
			userProfile.addInstallableUnitProperties(iu, sharedProfile.getInstallableUnitProperties(iu));
			userProfile.setInstallableUnitProperty(iu, IInstallableUnit.PROP_PROFILE_LOCKED_IU, IU_LOCKED);
			userProfile.setInstallableUnitProperty(iu, PROP_BASE, Boolean.TRUE.toString());
		}
	}

	private static void removeUserProfileBaseIUs(final Profile userProfile) {
		Query rootIUQuery = new Query() {
			public boolean isMatch(Object candidate) {
				if (candidate instanceof IInstallableUnit) {
					IInstallableUnit iu = (IInstallableUnit) candidate;
					if (Boolean.valueOf(userProfile.getInstallableUnitProperty(iu, PROP_BASE)).booleanValue())
						return true;
				}
				return false;
			}
		};
		Collector rootIUs = userProfile.query(rootIUQuery, new Collector(), null);
		for (Iterator iterator = rootIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			userProfile.removeInstallableUnit(iu);
		}
	}

	private static void markRootsOptional(final Profile userProfile) {
		Query rootIUQuery = new Query() {
			public boolean isMatch(Object candidate) {
				if (candidate instanceof IInstallableUnit) {
					IInstallableUnit iu = (IInstallableUnit) candidate;
					if (Boolean.valueOf(userProfile.getInstallableUnitProperty(iu, PROP_TYPE_ROOT)).booleanValue())
						return true;
				}
				return false;
			}
		};
		Collector rootIUs = userProfile.query(rootIUQuery, new Collector(), null);
		for (Iterator iterator = rootIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			userProfile.setInstallableUnitProperty(iu, PROP_INCLUSION_RULES, OPTIONAL);
		}
	}

	private static void updateProperties(final IProfile sharedProfile, Profile userProfile) {
		userProfile.setProperty(PROP_SHARED_TIMESTAMP, Long.toString(sharedProfile.getTimestamp()));
		Location installLocation = (Location) ServiceHelper.getService(EngineActivator.getContext(), Location.class.getName(), Location.INSTALL_FILTER);
		File installFolder = new File(installLocation.getURL().getPath());

		if (Boolean.valueOf(sharedProfile.getProperty(IProfile.PROP_ROAMING)).booleanValue()) {
			userProfile.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.getAbsolutePath());
			userProfile.setProperty(IProfile.PROP_SHARED_CACHE, installFolder.getAbsolutePath());
			userProfile.setProperty(IProfile.PROP_ROAMING, Boolean.FALSE.toString());
		} else {
			String cache = sharedProfile.getProperty(IProfile.PROP_CACHE);
			if (cache != null)
				userProfile.setProperty(IProfile.PROP_SHARED_CACHE, cache);
		}

		Location configurationLocation = (Location) ServiceHelper.getService(EngineActivator.getContext(), Location.class.getName(), Location.CONFIGURATION_FILTER);
		File configurationFolder = new File(configurationLocation.getURL().getPath());

		userProfile.setProperty(IProfile.PROP_CACHE, configurationFolder.getParentFile().getAbsolutePath());
		userProfile.setProperty(IProfile.PROP_CONFIGURATION_FOLDER, configurationFolder.getAbsolutePath());

		File launcherConfigFile = new File(configurationFolder, ECLIPSE_INI_IGNORED);
		userProfile.setProperty(IProfile.PROP_LAUNCHER_CONFIGURATION, launcherConfigFile.getAbsolutePath());
	}

	private synchronized SimpleProfileRegistry getProfileRegistry() {
		if (profileRegistry == null) {
			String installArea = EngineActivator.getContext().getProperty(OSGI_INSTALL_AREA);
			try {
				URL registryURL = new URL(installArea + P2_ENGINE_DIR + SimpleProfileRegistry.DEFAULT_STORAGE_DIR);
				File sharedRegistryDirectory = URIUtil.toFile(URIUtil.toURI(registryURL));
				profileRegistry = new SimpleProfileRegistry(sharedRegistryDirectory, null, false);
			} catch (MalformedURLException e) {
				//this is not possible because we know the above URL is valid
			} catch (URISyntaxException e) {
				//this is not possible because we know the above URL is valid
			}
		}
		return profileRegistry;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.engine.ISurrogateProfileHandler#createProfile(java.lang.String)
	 */
	public Profile createProfile(String id) {
		final IProfile sharedProfile = getProfileRegistry().getProfile(id);
		if (sharedProfile == null)
			return null;

		Profile userProfile = new Profile(id, null, sharedProfile.getProperties());
		userProfile.setProperty(PROP_SURROGATE, Boolean.TRUE.toString());
		userProfile.setSurrogateProfileHandler(this);
		updateProperties(sharedProfile, userProfile);
		addSharedProfileBaseIUs(sharedProfile, userProfile);
		return userProfile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.engine.ISurrogateProfileHandler#isSurrogate(org.eclipse.equinox.internal.provisional.p2.engine.IProfile)
	 */
	public boolean isSurrogate(IProfile profile) {
		return Boolean.valueOf(profile.getProperty(PROP_SURROGATE)).booleanValue();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.engine.ISurrogateProfileHandler#queryProfile(org.eclipse.equinox.internal.provisional.p2.engine.IProfile, org.eclipse.equinox.internal.provisional.p2.query.Query, org.eclipse.equinox.internal.provisional.p2.query.Collector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Collector queryProfile(IProfile profile, Query query, Collector collector, IProgressMonitor monitor) {
		IProfile sharedProfile = getProfileRegistry().getProfile(profile.getProfileId());
		if (sharedProfile != null)
			sharedProfile.query(query, collector, monitor);

		return profile.query(query, collector, monitor);
	}

	public boolean updateProfile(Profile userProfile) {
		final IProfile sharedProfile = getProfileRegistry().getProfile(userProfile.getProfileId());
		if (sharedProfile == null)
			throw new IllegalStateException(NLS.bind(Messages.shared_profile_not_found, userProfile.getProfileId()));

		String sharedTimeStamp = Long.toString(sharedProfile.getTimestamp());
		String userSharedTimeStamp = userProfile.getProperty(PROP_SHARED_TIMESTAMP);

		if (userSharedTimeStamp != null && userSharedTimeStamp.equals(sharedTimeStamp))
			return false;

		updateProperties(sharedProfile, userProfile);
		removeUserProfileBaseIUs(userProfile);
		if (!userProfile.query(InstallableUnitQuery.ANY, new Collector(), null).isEmpty()) {
			userProfile.setProperty(PROP_RESOLVE, Boolean.TRUE.toString());
			markRootsOptional(userProfile);
		}
		addSharedProfileBaseIUs(sharedProfile, userProfile);
		return true;
	}
}
