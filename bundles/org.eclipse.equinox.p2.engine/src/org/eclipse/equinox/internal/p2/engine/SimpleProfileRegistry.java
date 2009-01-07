/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SimpleProfileRegistry implements IProfileRegistry {

	static class Lock {

		private Thread lockHolder;
		private int lockedCount;

		protected void lock(Object monitor) {
			Thread current = Thread.currentThread();
			if (lockHolder != current) {
				boolean interrupted = false;
				try {
					while (lockedCount != 0)
						try {
							monitor.wait();
						} catch (InterruptedException e) {
							// although we don't handle an interrupt we should still 
							// save and restore the interrupt for others further up the stack
							interrupted = true;
						}
				} finally {
					if (interrupted)
						current.interrupt(); // restore interrupted status
				}
			}
			lockedCount++;
			lockHolder = current;
		}

		protected void unlock(Object monitor) {
			Thread current = Thread.currentThread();
			if (lockHolder != current)
				throw new IllegalStateException(Messages.thread_not_owner);

			lockedCount--;
			if (lockedCount == 0) {
				lockHolder = null;
				monitor.notifyAll();
			}
		}

		protected synchronized void checkLocked() {
			Thread current = Thread.currentThread();
			if (lockHolder != current)
				throw new IllegalStateException(Messages.thread_not_owner);
		}

	}

	private static final String PROFILE_EXT = ".profile"; //$NON-NLS-1$
	public static final String DEFAULT_STORAGE_DIR = "profileRegistry"; //$NON-NLS-1$
	/**
	 * Reference to Map of String(Profile id)->Profile. 
	 */
	private SoftReference profiles;
	private Map profileLocks = new HashMap();

	private String self;

	//Whether the registry should update the self profile when the registry is restored
	private boolean updateSelfProfile;

	private File store;

	ISurrogateProfileHandler surrogateProfileHandler;

	public SimpleProfileRegistry() {
		this(null, new SurrogateProfileHandler(), true);
	}

	public SimpleProfileRegistry(File registryDirectory, ISurrogateProfileHandler handler, boolean updateSelfProfile) {
		store = (registryDirectory != null) ? registryDirectory : getDefaultRegistryDirectory();
		surrogateProfileHandler = handler;
		self = EngineActivator.getContext().getProperty("eclipse.p2.profile"); //$NON-NLS-1$
		this.updateSelfProfile = updateSelfProfile;
	}

	private static File getDefaultRegistryDirectory() {
		File registryDirectory = null;
		AgentLocation agent = (AgentLocation) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.class.getName());
		if (agent == null)
			throw new IllegalStateException("Profile Registry inialization failed: Agent Location is not available"); //$NON-NLS-1$
		try {
			URL registryURL = new URL(agent.getDataArea(EngineActivator.ID), DEFAULT_STORAGE_DIR);
			registryDirectory = new File(registryURL.getPath());
			registryDirectory.mkdirs();

		} catch (MalformedURLException e) {
			//this is not possible because we know the above URL is valid
		}
		return registryDirectory;
	}

	/**
	 * If the current profile for self is marked as a roaming profile, we need
	 * to update its install and bundle pool locations.
	 */
	private void updateSelfProfile(Map profileMap) {
		if (profileMap == null)
			return;
		Profile selfProfile = (Profile) profileMap.get(self);
		if (selfProfile == null)
			return;

		boolean changed = false;
		//only update if self is a roaming profile
		if (Boolean.valueOf(selfProfile.getProperty(IProfile.PROP_ROAMING)).booleanValue())
			changed = updateRoamingProfile(selfProfile);

		if (surrogateProfileHandler != null && surrogateProfileHandler.isSurrogate(selfProfile))
			changed = changed || surrogateProfileHandler.updateProfile(selfProfile);

		if (changed)
			saveProfile(selfProfile);
	}

	private boolean updateRoamingProfile(Profile selfProfile) {
		Location installLocation = (Location) ServiceHelper.getService(EngineActivator.getContext(), Location.class.getName(), Location.INSTALL_FILTER);
		File location = new File(installLocation.getURL().getPath());
		boolean changed = false;
		if (!location.equals(new File(selfProfile.getProperty(IProfile.PROP_INSTALL_FOLDER)))) {
			selfProfile.setProperty(IProfile.PROP_INSTALL_FOLDER, location.getAbsolutePath());
			changed = true;
		}
		if (!location.equals(new File(selfProfile.getProperty(IProfile.PROP_CACHE)))) {
			selfProfile.setProperty(IProfile.PROP_CACHE, location.getAbsolutePath());
			changed = true;
		}
		return changed;
	}

	public synchronized String toString() {
		return getProfileMap().toString();
	}

	public synchronized IProfile getProfile(String id) {
		Profile profile = internalGetProfile(id);
		if (profile == null)
			return null;
		return profile.snapshot();
	}

	public IProfile getProfile(String id, long timestamp) {
		IProfile profile = getProfile(id);
		if (profile != null && profile.getTimestamp() == timestamp)
			return profile;

		File profileDirectory = new File(store, escape(id) + PROFILE_EXT);
		if (!profileDirectory.isDirectory())
			return null;

		File profileFile = new File(profileDirectory, Long.toString(timestamp) + PROFILE_EXT);
		if (!profileFile.exists())
			return null;

		Parser parser = new Parser(EngineActivator.getContext(), EngineActivator.ID);
		try {
			parser.parse(profileFile);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.error_parsing_profile, profileFile), e));
		}
		return (IProfile) parser.getProfileMap().get(id);
	}

	public long[] listProfileTimestamps(String id) {
		File profileDirectory = new File(store, escape(id) + PROFILE_EXT);
		if (!profileDirectory.isDirectory())
			return new long[0];

		File[] profileFiles = profileDirectory.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(PROFILE_EXT) && pathname.isFile();
			}
		});

		long[] timestamps = new long[profileFiles.length];
		for (int i = 0; i < profileFiles.length; i++) {
			String filename = profileFiles[i].getName();
			int extensionIndex = filename.lastIndexOf(PROFILE_EXT);
			try {
				timestamps[i] = Long.parseLong(filename.substring(0, extensionIndex));
			} catch (NumberFormatException e) {
				throw new IllegalStateException("Incompatible profile file name. Expected format is {timestamp}" + PROFILE_EXT + " but was " + filename + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		Arrays.sort(timestamps);
		return timestamps;
	}

	private Profile internalGetProfile(String id) {
		if (SELF.equals(id))
			id = self;
		Profile profile = (Profile) getProfileMap().get(id);
		if (profile == null && self != null && self.equals(id))
			profile = createSurrogateProfile(id);

		return profile;
	}

	private Profile createSurrogateProfile(String id) {
		if (surrogateProfileHandler == null)
			return null;

		Profile profile = (Profile) surrogateProfileHandler.createProfile(id);
		if (profile == null)
			return null;

		saveProfile(profile);

		// reset profile cache
		profiles = null;
		updateSelfProfile = true;
		return (Profile) getProfileMap().get(id);
	}

	public synchronized IProfile[] getProfiles() {
		Map profileMap = getProfileMap();
		Profile[] result = new Profile[profileMap.size()];
		int i = 0;
		for (Iterator it = profileMap.values().iterator(); it.hasNext(); i++) {
			Profile profile = (Profile) it.next();
			result[i] = profile.snapshot();
		}
		return result;
	}

	/**
	 * Returns an initialized map of String(Profile id)->Profile. 
	 */
	protected Map getProfileMap() {
		if (profiles != null) {
			Map result = (Map) profiles.get();
			if (result != null)
				return result;
		}
		Map result = restore();
		if (result == null)
			result = new LinkedHashMap(8);
		profiles = new SoftReference(result);
		if (updateSelfProfile) {
			//update self profile on first load
			updateSelfProfile = false;
			updateSelfProfile(result);
		}
		publishRepositoryReferences(result);
		return result;
	}

	/**
	 * Publishes any repository references for the currently running profile in this registry.
	 */
	private void publishRepositoryReferences(Map profileMap) {
		Profile profile = (Profile) profileMap.get(self);
		if (profile == null)
			return;
		String repos = profile.getProperty(IProfile.PROP_METADATA_REPOSITORIES);
		if (repos != null) {
			IRepositoryManager manager = (IRepositoryManager) ServiceHelper.getService(EngineActivator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
			doPublishRepositories(manager, repos);
		}
		repos = profile.getProperty(IProfile.PROP_ARTIFACT_REPOSITORIES);
		if (repos != null) {
			IRepositoryManager manager = (IRepositoryManager) ServiceHelper.getService(EngineActivator.getContext(), IArtifactRepositoryManager.SERVICE_NAME);
			doPublishRepositories(manager, repos);
		}
	}

	/**
	 * Adds the given list of repositories to the given repository manager.
	 */
	private void doPublishRepositories(IRepositoryManager manager, String repos) {
		StringTokenizer tokens = new StringTokenizer(repos, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens())
			manager.addRepository(decodeURI(tokens.nextToken()));
	}

	/**
	 * Decodes a URI that has had literal comma characters encoded.
	 */
	private URI decodeURI(String locationString) {
		//convert escaped commas back to literal commas
		int nextComma = locationString.indexOf("${#44}"); //$NON-NLS-1$
		while (nextComma > 0) {
			locationString = locationString.substring(0, nextComma) + ',' + locationString.substring(nextComma + 6);
			nextComma = locationString.indexOf("${#44}");//$NON-NLS-1$
		}
		return URI.create(locationString);
	}

	public synchronized void updateProfile(Profile profile) {
		String id = profile.getProfileId();
		Profile current = internalGetProfile(id);
		if (current == null)
			throw new IllegalArgumentException(NLS.bind(Messages.profile_does_not_exist, id));

		Lock lock = (Lock) profileLocks.get(id);
		lock.checkLocked();

		current.clearLocalProperties();
		current.clearInstallableUnits();

		current.addProperties(profile.getLocalProperties());
		Collector collector = profile.query(InstallableUnitQuery.ANY, new Collector(), null);
		for (Iterator collectorIt = collector.iterator(); collectorIt.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) collectorIt.next();
			current.addInstallableUnit(iu);
			Map iuProperties = profile.getInstallableUnitProperties(iu);
			if (iuProperties != null)
				current.addInstallableUnitProperties(iu, iuProperties);
		}
		saveProfile(current);
		profile.clearOrphanedInstallableUnitProperties();
		profile.setTimestamp(current.getTimestamp());
		broadcastChangeEvent(id, ProfileEvent.CHANGED);
	}

	public IProfile addProfile(String id) throws ProvisionException {
		return addProfile(id, null, null);
	}

	public IProfile addProfile(String id, Map profileProperties) throws ProvisionException {
		return addProfile(id, profileProperties, null);
	}

	public synchronized IProfile addProfile(String id, Map profileProperties, String parentId) throws ProvisionException {
		if (SELF.equals(id))
			id = self;
		Map profileMap = getProfileMap();
		if (profileMap.get(id) != null)
			throw new ProvisionException(NLS.bind(Messages.Profile_Duplicate_Root_Profile_Id, id));

		Profile parent = null;
		if (parentId != null) {
			if (SELF.equals(parentId))
				parentId = self;
			parent = (Profile) profileMap.get(parentId);
			if (parent == null)
				throw new ProvisionException(NLS.bind(Messages.Profile_Parent_Not_Found, parentId));
		}

		Profile profile = new Profile(id, parent, profileProperties);
		if (surrogateProfileHandler != null && surrogateProfileHandler.isSurrogate(profile))
			profile.setSurrogateProfileHandler(surrogateProfileHandler);
		profileMap.put(id, profile);
		saveProfile(profile);
		broadcastChangeEvent(id, ProfileEvent.ADDED);
		return profile.snapshot();
	}

	public synchronized void removeProfile(String profileId) {
		if (SELF.equals(profileId))
			profileId = self;
		//note we need to maintain a reference to the profile map until it is persisted to prevent gc
		Map profileMap = getProfileMap();
		Profile profile = (Profile) profileMap.get(profileId);
		if (profile == null)
			return;

		String[] subProfileIds = profile.getSubProfileIds();
		for (int i = 0; i < subProfileIds.length; i++) {
			removeProfile(subProfileIds[i]);
		}
		internalLockProfile(profile);
		try {
			profile.setParent(null);
		} finally {
			internalUnlockProfile(profile);
		}
		profileMap.remove(profileId);
		profileLocks.remove(profileId);
		deleteProfile(profileId);
		broadcastChangeEvent(profileId, ProfileEvent.REMOVED);
	}

	private void broadcastChangeEvent(String profileId, byte reason) {
		((IProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), IProvisioningEventBus.class.getName())).publishEvent(new ProfileEvent(profileId, reason));
	}

	/**
	 * Restores the profile registry from disk, and returns the loaded profile map.
	 * Returns <code>null</code> if unable to read the registry.
	 */
	private Map restore() {

		if (store == null || !store.isDirectory())
			throw new IllegalStateException(Messages.reg_dir_not_available);

		Parser parser = new Parser(EngineActivator.getContext(), EngineActivator.ID);
		File[] profileDirectories = store.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(PROFILE_EXT) && pathname.isDirectory();
			}
		});
		for (int i = 0; i < profileDirectories.length; i++) {
			File profileFile = findLatestProfileFile(profileDirectories[i]);
			if (profileFile != null) {
				try {
					parser.parse(profileFile);
				} catch (IOException e) {
					LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.error_parsing_profile, profileFile), e));
				}
			}
		}
		return parser.getProfileMap();
	}

	private File findLatestProfileFile(File profileDirectory) {

		File latest = null;
		long latestTimestamp = 0;
		File[] profileFiles = profileDirectory.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(PROFILE_EXT) && !pathname.isDirectory();
			}
		});
		for (int i = 0; i < profileFiles.length; i++) {
			File profileFile = profileFiles[i];
			String fileName = profileFile.getName();
			try {
				long timestamp = Long.parseLong(fileName.substring(0, fileName.indexOf(PROFILE_EXT)));
				if (timestamp > latestTimestamp) {
					latestTimestamp = timestamp;
					latest = profileFile;
				}
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return latest;
	}

	private void saveProfile(Profile profile) {

		File profileDirectory = new File(store, escape(profile.getProfileId()) + PROFILE_EXT);
		profileDirectory.mkdir();

		long previousTimestamp = profile.getTimestamp();
		long currentTimestamp = System.currentTimeMillis();
		if (currentTimestamp <= previousTimestamp)
			currentTimestamp = previousTimestamp + 1;
		File profileFile = new File(profileDirectory, Long.toString(currentTimestamp) + PROFILE_EXT);

		profile.setTimestamp(currentTimestamp);
		profile.setChanged(false);
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(profileFile));
			Writer writer = new Writer(os);
			writer.writeProfile(profile);
		} catch (IOException e) {
			profile.setTimestamp(previousTimestamp);
			profileFile.delete();
			LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.error_persisting_profile, profile.getProfileId()), e));
		} finally {
			try {
				if (os != null)
					os.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	private void deleteProfile(String profileId) {
		File profileDirectory = new File(store, escape(profileId) + PROFILE_EXT);
		FileUtils.deleteAll(profileDirectory);
	}

	private static String escape(String toEscape) {
		StringBuffer buffer = new StringBuffer();
		int length = toEscape.length();
		for (int i = 0; i < length; ++i) {
			char ch = toEscape.charAt(i);
			switch (ch) {
				case '\\' :
				case '/' :
				case ':' :
				case '*' :
				case '?' :
				case '"' :
				case '<' :
				case '>' :
				case '|' :
				case '%' :
					buffer.append("%" + (int) ch + ";"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				default :
					buffer.append(ch);
			}
		}
		return buffer.toString();
	}

	static class Writer extends ProfileWriter {

		public Writer(OutputStream output) throws IOException {
			super(output, new ProcessingInstruction[] {ProcessingInstruction.makeClassVersionInstruction(PROFILE_TARGET, Profile.class, ProfileXMLConstants.CURRENT_VERSION)});
		}
	}

	/*
	 * 	Parser for the contents of a SimpleProfileRegistry,
	 * 	as written by the Writer class.
	 */
	class Parser extends ProfileParser {
		private final Map profileHandlers = new HashMap();

		public Parser(BundleContext context, String bundleId) {
			super(context, bundleId);
		}

		public void parse(File file) throws IOException {
			parse(new BufferedInputStream(new FileInputStream(file)));
		}

		public synchronized void parse(InputStream stream) throws IOException {
			this.status = null;
			try {
				// TODO: currently not caching the parser since we make no assumptions
				//		 or restrictions on concurrent parsing
				getParser();
				ProfileHandler profileHandler = new ProfileHandler();
				xmlReader.setContentHandler(new ProfileDocHandler(PROFILE_ELEMENT, profileHandler));
				xmlReader.parse(new InputSource(stream));
				profileHandlers.put(profileHandler.getProfileId(), profileHandler);
			} catch (SAXException e) {
				throw new IOException(e.getMessage());
			} catch (ParserConfigurationException e) {
				throw new IOException(e.getMessage());
			} finally {
				stream.close();
			}
		}

		protected Object getRootObject() {
			return this;
		}

		public Map getProfileMap() {
			Map profileMap = new HashMap();
			for (Iterator it = profileHandlers.keySet().iterator(); it.hasNext();) {
				String profileId = (String) it.next();
				addProfile(profileId, profileMap);
			}
			return profileMap;
		}

		private void addProfile(String profileId, Map profileMap) {
			if (profileMap.containsKey(profileId))
				return;

			ProfileHandler profileHandler = (ProfileHandler) profileHandlers.get(profileId);
			Profile parentProfile = null;

			String parentId = profileHandler.getParentId();
			if (parentId != null) {
				addProfile(parentId, profileMap);
				parentProfile = (Profile) profileMap.get(parentId);
			}

			Profile profile = new Profile(profileId, parentProfile, profileHandler.getProperties());
			if (surrogateProfileHandler != null && surrogateProfileHandler.isSurrogate(profile))
				profile.setSurrogateProfileHandler(surrogateProfileHandler);

			profile.setTimestamp(profileHandler.getTimestamp());

			IInstallableUnit[] ius = profileHandler.getInstallableUnits();
			if (ius != null) {
				for (int i = 0; i < ius.length; i++) {
					IInstallableUnit iu = ius[i];
					profile.addInstallableUnit(iu);
					Map iuProperties = profileHandler.getIUProperties(iu);
					if (iuProperties != null) {
						for (Iterator it = iuProperties.entrySet().iterator(); it.hasNext();) {
							Entry entry = (Entry) it.next();
							String key = (String) entry.getKey();
							String value = (String) entry.getValue();
							profile.setInstallableUnitProperty(iu, key, value);
						}
					}
				}
			}
			profile.setChanged(false);
			profileMap.put(profileId, profile);
		}

		private final class ProfileDocHandler extends DocHandler {

			public ProfileDocHandler(String rootName, RootHandler rootHandler) {
				super(rootName, rootHandler);
			}

			public void processingInstruction(String target, String data) throws SAXException {
				if (ProfileXMLConstants.PROFILE_TARGET.equals(target)) {
					Version repositoryVersion = extractPIVersion(target, data);
					if (!ProfileXMLConstants.XML_TOLERANCE.isIncluded(repositoryVersion)) {
						throw new SAXException(NLS.bind(Messages.SimpleProfileRegistry_Parser_Has_Incompatible_Version, repositoryVersion, ProfileXMLConstants.XML_TOLERANCE));
					}
				}
			}
		}

		protected String getErrorMessage() {
			return Messages.SimpleProfileRegistry_Parser_Error_Parsing_Registry;
		}

		public String toString() {
			// TODO:
			return null;
		}

	}

	public synchronized void lockProfile(Profile profile) {
		Profile internalProfile = internalGetProfile(profile.getProfileId());
		if (internalProfile == null)
			throw new IllegalArgumentException(NLS.bind(Messages.profile_not_registered, profile.getProfileId()));

		if (profile.isChanged() || !checkTimestamps(profile, internalProfile))
			throw new IllegalArgumentException(NLS.bind(Messages.profile_not_current, profile.getProfileId()));

		internalLockProfile(internalProfile);
	}

	private void internalLockProfile(IProfile profile) {
		Lock lock = (Lock) profileLocks.get(profile.getProfileId());
		if (lock == null) {
			lock = new Lock();
			profileLocks.put(profile.getProfileId(), lock);
		}
		lock.lock(this);
		if (profile.getParentProfile() != null)
			internalLockProfile(profile.getParentProfile());
	}

	private boolean checkTimestamps(IProfile profile, IProfile internalProfile) {
		if (profile.getTimestamp() == internalProfile.getTimestamp()) {
			if (profile.getParentProfile() == null)
				return true;

			return checkTimestamps(profile.getParentProfile(), internalProfile.getParentProfile());
		}
		return false;
	}

	public synchronized void unlockProfile(IProfile profile) {
		Profile internalProfile = internalGetProfile(profile.getProfileId());
		if (internalProfile == null)
			throw new IllegalArgumentException(NLS.bind(Messages.profile_not_registered, profile.getProfileId()));
		internalUnlockProfile(internalProfile);
	}

	private void internalUnlockProfile(IProfile profile) {
		if (profile.getParentProfile() != null)
			internalUnlockProfile(profile.getParentProfile());

		Lock lock = (Lock) profileLocks.get(profile.getProfileId());
		lock.unlock(this);
	}

	public Profile validate(IProfile candidate) {
		if (candidate instanceof Profile)
			return (Profile) candidate;

		throw new IllegalArgumentException("Profile incompatible: expected " + Profile.class.getName() + " but was " + ((candidate != null) ? candidate.getClass().getName() : "null") + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
