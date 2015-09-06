/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.garbagecollector;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentService;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.osgi.service.prefs.Preferences;

/**
 * The main control point for the p2 garbage collector.  Takes a Profile and runs the CoreGarbageCollector with the
 * appropriate MarkSets for the repositories used by that Profile.
 * 
 * Takes the profile passed in and creates a set (markSet) that maps the artifact repositories it uses to the
 * artifact keys its IUs hold.  This is done by getting MarkSets from all registered IMarkSetProviders.
 * 
 * Then, the MarkSets are obtained for every other registered Profile in a similar fashion.  Each MarkSet is
 * checked to see if its artifact repository is already a key in markSet.  If so, that MarkSet's artifact keys 
 * are added to the list that is mapped to by the artifact repository. 
 */
public class GarbageCollector implements SynchronousProvisioningListener, IAgentService {
	/**
	 * Service name constant for the garbage collection service.
	 */
	public static final String SERVICE_NAME = GarbageCollector.class.getName();

	private class ParameterizedSafeRunnable implements ISafeRunnable {
		IProfile aProfile;
		MarkSet[] aProfileMarkSets;
		IConfigurationElement cfg;

		public ParameterizedSafeRunnable(IConfigurationElement runtAttribute, IProfile profile) {
			cfg = runtAttribute;
			aProfile = profile;
		}

		public MarkSet[] getResult() {
			return aProfileMarkSets;
		}

		public void handleException(Throwable exception) {
			LogHelper.log(new Status(IStatus.ERROR, GCActivator.ID, Messages.Error_in_extension, exception));
		}

		public void run() throws Exception {
			MarkSetProvider aMarkSetProvider = (MarkSetProvider) cfg.createExecutableExtension(ATTRIBUTE_CLASS);
			if (aMarkSetProvider == null) {
				aProfileMarkSets = null;
				return;
			}
			aProfileMarkSets = aMarkSetProvider.getMarkSets(agent, aProfile);
		}
	}

	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

	private static final String PT_MARKSET = GCActivator.ID + ".marksetproviders"; //$NON-NLS-1$
	final IProvisioningAgent agent;

	//The GC is triggered when an uninstall event occurred during a "transaction" and the transaction is committed.   
	String uninstallEventProfileId = null;

	/**
	 * Maps IArtifactRepository objects to their respective "marked set" of IArtifactKeys
	 */
	private Map<IArtifactRepository, Collection<IArtifactKey>> markSet;

	public GarbageCollector(IProvisioningAgent agent) {
		this.agent = agent;
	}

	private void addKeys(Collection<IArtifactKey> keyList, IArtifactKey[] keyArray) {
		for (int i = 0; i < keyArray.length; i++)
			keyList.add(keyArray[i]);
	}

	private void contributeMarkSets(IConfigurationElement runAttribute, IProfile profile, boolean addRepositories) {
		ParameterizedSafeRunnable providerExecutor = new ParameterizedSafeRunnable(runAttribute, profile);
		SafeRunner.run(providerExecutor);
		MarkSet[] aProfileMarkSets = providerExecutor.getResult();
		if (aProfileMarkSets == null || aProfileMarkSets.length == 0 || aProfileMarkSets[0] == null)
			return;

		for (int i = 0; i < aProfileMarkSets.length; i++) {
			if (aProfileMarkSets[i] == null) {
				continue;
			}
			Collection<IArtifactKey> keys = markSet.get(aProfileMarkSets[i].getRepo());
			if (keys == null) {
				if (addRepositories) {
					keys = new HashSet<IArtifactKey>();
					markSet.put(aProfileMarkSets[i].getRepo(), keys);
					addKeys(keys, aProfileMarkSets[i].getKeys());
				}
			} else {
				addKeys(keys, aProfileMarkSets[i].getKeys());
			}
		}
	}

	protected boolean getBooleanPreference(String key, boolean defaultValue) {
		IPreferencesService prefService = GCActivator.getService(IPreferencesService.class);
		if (prefService == null)
			return defaultValue;
		List<IEclipsePreferences> nodes = new ArrayList<IEclipsePreferences>();
		// todo we should look in the instance scope as well but have to be careful that the instance location has been set
		nodes.add(ConfigurationScope.INSTANCE.getNode(GCActivator.ID));
		nodes.add(DefaultScope.INSTANCE.getNode(GCActivator.ID));
		return Boolean.parseBoolean(prefService.get(key, Boolean.toString(defaultValue), nodes.toArray(new Preferences[nodes.size()])));
	}

	private void invokeCoreGC() {
		for (IArtifactRepository nextRepo : markSet.keySet()) {
			IArtifactKey[] keys = markSet.get(nextRepo).toArray(new IArtifactKey[0]);
			MarkSet aMarkSet = new MarkSet(keys, nextRepo);
			new CoreGarbageCollector().clean(aMarkSet.getKeys(), aMarkSet.getRepo());
		}
	}

	public void notify(EventObject o) {
		if (o instanceof InstallableUnitEvent) {
			InstallableUnitEvent event = (InstallableUnitEvent) o;
			if (event.isUninstall() && event.isPost()) {
				uninstallEventProfileId = event.getProfile().getProfileId();
			}
		} else if (o instanceof CommitOperationEvent) {
			if (uninstallEventProfileId != null) {
				CommitOperationEvent event = (CommitOperationEvent) o;
				if (uninstallEventProfileId.equals(event.getProfile().getProfileId()) && getBooleanPreference(GCActivator.GC_ENABLED, true))
					runGC(event.getProfile());
				uninstallEventProfileId = null;
			}
		} else if (o instanceof RollbackOperationEvent) {
			if (uninstallEventProfileId != null && uninstallEventProfileId.equals(((RollbackOperationEvent) o).getProfile().getProfileId()))
				uninstallEventProfileId = null;
		}
	}

	public void runGC(IProfile profile) {
		markSet = new HashMap<IArtifactRepository, Collection<IArtifactKey>>();
		if (!traverseMainProfile(profile))
			return;

		//Complete each MarkSet with the MarkSets provided by all of the other registered Profiles
		traverseRegisteredProfiles();

		//Run the GC on each MarkSet
		invokeCoreGC();
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.core.spi.IAgentService#start()
	 */
	public void start() {
		IProvisioningEventBus eventBus = (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);
		if (eventBus == null)
			return;
		eventBus.addListener(this);
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.core.spi.IAgentService#stop()
	 */
	public void stop() {
		IProvisioningEventBus eventBus = (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);
		if (eventBus != null)
			eventBus.removeListener(this);
	}

	private boolean traverseMainProfile(IProfile profile) {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IConfigurationElement[] configElts = registry.getConfigurationElementsFor(PT_MARKSET);

		//First we collect all repos and keys for the profile being GC'ed
		for (int i = 0; i < configElts.length; i++) {
			if (!(configElts[i].getName().equals("run"))) { //$NON-NLS-1$
				continue;
			}
			IConfigurationElement runAttribute = configElts[i];
			if (runAttribute == null) {
				continue;
			}

			contributeMarkSets(runAttribute, profile, true);
		}
		return true;
	}

	private void traverseRegisteredProfiles() {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IConfigurationElement[] configElts = registry.getConfigurationElementsFor(PT_MARKSET);
		for (int i = 0; i < configElts.length; i++) {
			if (!(configElts[i].getName().equals("run"))) { //$NON-NLS-1$
				continue;
			}
			IConfigurationElement runAttribute = configElts[i];
			if (runAttribute == null) {
				continue;
			}

			IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
			if (profileRegistry == null)
				return;
			IProfile[] registeredProfiles = profileRegistry.getProfiles();

			for (int j = 0; j < registeredProfiles.length; j++) {
				contributeMarkSets(runAttribute, registeredProfiles[j], false);
			}
		}
	}
}
