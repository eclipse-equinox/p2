/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.util.*;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * A preference implementation that stores preferences in the engine's profile
 * data area. There is one preference file per profile, with an additional file
 * that is used when there is no currently running profile.
 */
public class ProfilePreferences extends EclipsePreferences {
	private class SaveJob extends Job {
		SaveJob() {
			super(Messages.ProfilePreferences_saving);
			setSystem(true);
		}

		public boolean belongsTo(Object family) {
			return family == PROFILE_SAVE_JOB_FAMILY;
		}

		protected IStatus run(IProgressMonitor monitor) {
			try {
				doSave();
			} catch (BackingStoreException e) {
				LogHelper.log(new Status(IStatus.WARNING, EngineActivator.ID, "Exception saving profile preferences", e)); //$NON-NLS-1$
			}
			return Status.OK_STATUS;
		}
	}

	// cache which nodes have been loaded from disk
	private static Set loadedNodes = Collections.synchronizedSet(new HashSet());

	public static final Object PROFILE_SAVE_JOB_FAMILY = new Object();

	private static final long SAVE_SCHEDULE_DELAY = 500;
	//private IPath location;
	private IEclipsePreferences loadLevel;
	private Object profileLock;
	private String qualifier;

	private SaveJob saveJob;
	private int segmentCount;

	public ProfilePreferences() {
		this(null, null);
	}

	public ProfilePreferences(EclipsePreferences nodeParent, String nodeName) {
		super(nodeParent, nodeName);

		// cache the segment count
		String path = absolutePath();
		segmentCount = getSegmentCount(path);

		if (segmentCount <= 1)
			return;

		if (segmentCount == 2)
			profileLock = new Object();

		if (segmentCount < 3)
			return;
		// cache the qualifier
		qualifier = getSegment(path, 2);
	}

	private boolean containsProfile(String profileId) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(EngineActivator.getContext(), IProfileRegistry.class.getName());
		if (profileId == null || profileRegistry == null)
			return false;
		return profileRegistry.containsProfile(profileId);
	}

	/*
	 * (non-Javadoc)
	 * Create an Engine phase to save profile preferences
	 */
	protected void doSave() throws BackingStoreException {
		synchronized (((ProfilePreferences) parent).profileLock) {
			String profileId = getSegment(absolutePath(), 1);
			if (!containsProfile(profileId)) {
				//use the default location for the self profile, otherwise just do nothing and return
				if (IProfileRegistry.SELF.equals(profileId)) {
					IPath location = getDefaultLocation();
					if (location != null) {
						super.save(location);
						return;
					}
				}
				if (Tracing.DEBUG_PROFILE_PREFERENCES)
					Tracing.debug("Not saving preferences since there is no file for node: " + absolutePath()); //$NON-NLS-1$
				return;
			}
			super.save(getProfileLocation(profileId));
		}
	}

	/**
	 * Returns the preference file to use when there is no active profile.
	 */
	private IPath getDefaultLocation() {
		//use engine agent location for preferences if there is no self profile
		AgentLocation location = (AgentLocation) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.SERVICE_NAME);
		if (location == null) {
			LogHelper.log(new Status(IStatus.WARNING, EngineActivator.ID, "Agent location service not available", new RuntimeException())); //$NON-NLS-1$
			return null;
		}
		IPath dataArea = new Path(URLUtil.toFile(location.getDataArea(EngineActivator.ID)).getAbsolutePath());
		return computeLocation(dataArea, qualifier);
	}

	protected IEclipsePreferences getLoadLevel() {
		if (loadLevel == null) {
			if (qualifier == null)
				return null;
			// Make it relative to this node rather than navigating to it from the root.
			// Walk backwards up the tree starting at this node.
			// This is important to avoid a chicken/egg thing on startup.
			IEclipsePreferences node = this;
			for (int i = 3; i < segmentCount; i++)
				node = (EclipsePreferences) node.parent();
			loadLevel = node;
		}
		return loadLevel;
	}

	/**
	 * Returns the location of the preference file for the given profile.
	 */
	private IPath getProfileLocation(String profileId) {
		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(EngineActivator.getContext(), IProfileRegistry.class.getName());
		File profileDataDirectory = profileRegistry.getProfileDataDirectory(profileId);
		return computeLocation(new Path(profileDataDirectory.getAbsolutePath()), qualifier);
	}

	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new ProfilePreferences(nodeParent, nodeName);
	}

	protected boolean isAlreadyLoaded(IEclipsePreferences node) {
		return loadedNodes.contains(node.absolutePath());
	}

	protected boolean isAlreadyLoaded(String path) {
		return loadedNodes.contains(path);
	}

	/*
	 * (non-Javadoc)
	 * Create an Engine phase to load profile preferences
	 */
	protected void load() throws BackingStoreException {
		synchronized (((ProfilePreferences) parent).profileLock) {
			String profileId = getSegment(absolutePath(), 1);
			if (!containsProfile(profileId)) {
				//use the default location for the self profile, otherwise just do nothing and return
				if (IProfileRegistry.SELF.equals(profileId)) {
					IPath location = getDefaultLocation();
					if (location != null) {
						load(location);
						return;
					}
				}
				if (Tracing.DEBUG_PROFILE_PREFERENCES)
					Tracing.debug("Not loading preferences since there is no file for node: " + absolutePath()); //$NON-NLS-1$
				return;
			}
			load(getProfileLocation(profileId));
		}
	}

	protected void loaded() {
		loadedNodes.add(name());
	}

	public void removeNode() throws BackingStoreException {
		super.removeNode();
		loadedNodes.remove(this.absolutePath());
	}

	/**
	 * Schedules the save job. This method is synchronized to protect lazily initialization 
	 * of the save job instance.
	 */
	protected synchronized void save() {
		if (saveJob == null)
			saveJob = new SaveJob();
		//only schedule a save if the engine bundle is still running
		BundleContext context = EngineActivator.getContext();
		if (context == null)
			return;
		try {
			if (context.getBundle().getState() == Bundle.ACTIVE)
				saveJob.schedule(SAVE_SCHEDULE_DELAY);
		} catch (IllegalStateException e) {
			//bundle has been stopped concurrently, so don't save
		}
	}
}
