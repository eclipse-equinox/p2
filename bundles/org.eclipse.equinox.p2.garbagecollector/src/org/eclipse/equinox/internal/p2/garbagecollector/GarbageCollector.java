/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.garbagecollector;

import org.eclipse.equinox.p2.metadata.IArtifactKey;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

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
public class GarbageCollector {
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private static final String PT_MARKSET = GCActivator.ID + ".marksetproviders"; //$NON-NLS-1$

	/**
	 * Maps IArtifactRepository objects to their respective "marked set" of IArtifactKeys
	 */
	private Map markSet;
	final IProvisioningAgent agent;

	public GarbageCollector() {
		// we need to use DS to create an Agent Listener here
		agent = (IProvisioningAgent) GCActivator.getContext().getService(GCActivator.getContext().getServiceReference(IProvisioningAgent.class.getName()));
	}

	public void runGC(IProfile profile) {
		markSet = new HashMap();
		if (!traverseMainProfile(profile))
			return;

		//Complete each MarkSet with the MarkSets provided by all of the other registered Profiles
		traverseRegisteredProfiles();

		//Run the GC on each MarkSet
		invokeCoreGC();
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

	private void invokeCoreGC() {
		Iterator keyIterator = markSet.keySet().iterator();
		while (keyIterator.hasNext()) {
			IArtifactRepository nextRepo = (IArtifactRepository) keyIterator.next();
			IArtifactKey[] keys = (IArtifactKey[]) ((Collection) markSet.get(nextRepo)).toArray(new IArtifactKey[0]);
			MarkSet aMarkSet = new MarkSet(keys, nextRepo);
			new CoreGarbageCollector().clean(aMarkSet.getKeys(), aMarkSet.getRepo());
		}
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

			IProfileRegistry profileRegistry = (IProfileRegistry) GCActivator.getService(GCActivator.getContext(), IProfileRegistry.SERVICE_NAME);
			if (profileRegistry == null)
				return;
			IProfile[] registeredProfiles = profileRegistry.getProfiles();

			for (int j = 0; j < registeredProfiles.length; j++) {
				contributeMarkSets(runAttribute, registeredProfiles[j], false);
			}
		}
	}

	private class ParameterizedSafeRunnable implements ISafeRunnable {
		IConfigurationElement cfg;
		IProfile aProfile;
		MarkSet[] aProfileMarkSets;

		public ParameterizedSafeRunnable(IConfigurationElement runtAttribute, IProfile profile) {
			cfg = runtAttribute;
			aProfile = profile;
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

		public MarkSet[] getResult() {
			return aProfileMarkSets;
		}
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
			Collection keys = (Collection) markSet.get(aProfileMarkSets[i].getRepo());
			if (keys == null) {
				if (addRepositories) {
					keys = new HashSet();
					markSet.put(aProfileMarkSets[i].getRepo(), keys);
					addKeys(keys, aProfileMarkSets[i].getKeys());
				}
			} else {
				addKeys(keys, aProfileMarkSets[i].getKeys());
			}
		}
	}

	private void addKeys(Collection keyList, IArtifactKey[] keyArray) {
		for (int i = 0; i < keyArray.length; i++)
			keyList.add(keyArray[i]);
	}
}
