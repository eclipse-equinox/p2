/*******************************************************************************
 * Copyright (c) 2004, 2008, 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;

public class ProfilePreferences extends EclipsePreferences {
	private int segmentCount;
	private String qualifier;
	//private IPath location;
	private IEclipsePreferences loadLevel;
	// cache which nodes have been loaded from disk
	private static Set loadedNodes = new HashSet();

	private Object profileLock;

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

		// ensure profile exists
		computeProfile(path);

		if (segmentCount < 3)
			return;
		// cache the qualifier
		qualifier = getSegment(path, 2);
	}

	private IProfile computeProfile(String path) {
		String profileName = getSegment(path, 1);
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(EngineActivator.getContext(), IProfileRegistry.class.getName());
		IProfile result = null;
		if (profileName != null && profileRegistry != null)
			result = profileRegistry.getProfile(profileName);
		if (result == null && !profileName.equals(IProfileRegistry.SELF))
			throw new IllegalArgumentException(NLS.bind(Messages.ProfilePreferences_Profile_not_found, profileName));
		return result;
	}

	public void removeNode() throws BackingStoreException {
		super.removeNode();
		loadedNodes.remove(this.absolutePath());
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

	protected synchronized boolean isAlreadyLoaded(IEclipsePreferences node) {
		return loadedNodes.contains(node.absolutePath());
	}

	protected synchronized boolean isAlreadyLoaded(String path) {
		return loadedNodes.contains(path);
	}

	protected synchronized void loaded() {
		loadedNodes.add(name());
	}

	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new ProfilePreferences(nodeParent, nodeName);
	}

	/*
	 * (non-Javadoc)
	 * Create an Engine phase to load profile preferences
	 */
	protected void load() throws BackingStoreException {
		synchronized (((ProfilePreferences) parent).profileLock) {
			IProfile profile = computeProfile(absolutePath());
			//if there is no self profile, use a default location for preferences
			if (profile == null) {
				load(getDefaultLocation());
				return;
			}
			IEngine engine = (IEngine) ServiceHelper.getService(EngineActivator.getContext(), IEngine.SERVICE_NAME);
			if (engine == null) {
				throw new BackingStoreException(NLS.bind(Messages.ProfilePreferences_load_failed, profile.getProfileId()));
			}
			PhaseSet set = new ProfilePreferencePhaseSet(new PreferenceLoad());
			Exception failure;
			try {
				IStatus status = engine.perform(profile, set, new Operand[0], null, null);
				// Check return status.
				if (status.isOK())
					return;
				failure = (Exception) status.getException();
			} catch (IllegalStateException e) {
				failure = e;
			}
			if (failure != null && failure instanceof BackingStoreException)
				throw (BackingStoreException) failure;
			throw new BackingStoreException(NLS.bind(Messages.ProfilePreferences_load_failed, profile.getProfileId()), failure);
		}
	}

	private File getDefaultLocation() {
		//use engine agent location for preferences if there is no self profile
		AgentLocation location = (AgentLocation) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.SERVICE_NAME);
		return URLUtil.toFile(location.getDataArea(EngineActivator.ID));
	}

	/*
	 * (non-Javadoc)
	 * Create an Engine phase to save profile preferences
	 */
	protected void save() throws BackingStoreException {
		synchronized (((ProfilePreferences) parent).profileLock) {
			IProfile profile = computeProfile(absolutePath());
			//if there is no self profile, use a default location for preferences
			if (profile == null) {
				save(getDefaultLocation());
				return;
			}
			IEngine engine = (IEngine) ServiceHelper.getService(EngineActivator.getContext(), IEngine.SERVICE_NAME);
			if (engine == null) {
				throw new BackingStoreException(NLS.bind(Messages.ProfilePreferences_save_failed, profile.getProfileId()));
			}
			PhaseSet set = new ProfilePreferencePhaseSet(new PreferenceFlush());
			Exception failure;
			try {
				IStatus status = engine.perform(profile, set, new Operand[0], null, null);
				// Check return status.
				if (status.isOK())
					return;
				failure = (Exception) status.getException();
			} catch (IllegalStateException e) {
				failure = e;
			}
			if (failure != null && failure instanceof BackingStoreException)
				throw (BackingStoreException) failure;
			throw new BackingStoreException(NLS.bind(Messages.ProfilePreferences_save_failed, profile.getProfileId()), failure);
		}
	}

	public void load(File directory) throws BackingStoreException {
		super.load(computeLocation(new Path(directory.getAbsolutePath()), qualifier));
	}

	public void save(File directory) throws BackingStoreException {
		super.save(computeLocation(new Path(directory.getAbsolutePath()), qualifier));
	}

	// Simple PhaseSet used when loading or saving profile preferences
	private class ProfilePreferencePhaseSet extends PhaseSet {

		public ProfilePreferencePhaseSet(Phase phase) {
			super(new Phase[] {phase});
		}
	}

	private class PreferenceFlush extends Phase {
		private static final String PHASE_ID = "preferenceFlush"; //$NON-NLS-1$

		public PreferenceFlush() {
			super(PHASE_ID, 1);
		}

		protected ProvisioningAction[] getActions(Operand operand) {
			return null;
		}

		protected IStatus completePhase(IProgressMonitor monitor, IProfile phaseProfile, Map parameters) {
			File dataDirectory = (File) parameters.get(PARM_PROFILE_DATA_DIRECTORY);
			try {
				save(dataDirectory);
			} catch (BackingStoreException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage(), e);
			}
			return Status.OK_STATUS;
		}
	}

	private class PreferenceLoad extends Phase {
		private static final String PHASE_ID = "preferenceLoad"; //$NON-NLS-1$

		public PreferenceLoad() {
			super(PHASE_ID, 1);
		}

		protected ProvisioningAction[] getActions(Operand operand) {
			return null;
		}

		protected IStatus completePhase(IProgressMonitor monitor, IProfile phaseProfile, Map parameters) {
			File dataDirectory = (File) parameters.get(PARM_PROFILE_DATA_DIRECTORY);
			try {
				load(dataDirectory);
			} catch (BackingStoreException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage(), e);
			}
			return Status.OK_STATUS;
		}
	}
}
