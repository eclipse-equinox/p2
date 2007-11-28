/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.garbagecollector;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * The main control point for the p2 garbage collector.  Takes a Profile and runs the CoreGarbageCollector with the
 * appropriate MarkSets for the repositories used by that Profile.
 * 
 * Takes the profile passed in and creates a set (rootSetMap) that maps the artifact repositories it uses to the
 * artifact keys its IUs hold.  This is done by getting MarkSets from all registered IMarkSetProviders.
 * 
 * Then, the MarkSets are obtained for every other registered Profile in a similar fashion.  Each MarkSet is
 * checked to see if its artifact repository is already a key in rootSetMap.  If so, that MarkSet's artifact keys 
 * are added to the list that is mapped to by the artifact repository. 
 */
public class GarbageCollector {
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private static final String PT_MARKSET = GCActivator.ID + ".marksetproviders"; //$NON-NLS-1$

	/**
	 * Maps IArtifactRepository objects to their respective "marked set" of IArtifactKeys
	 */
	private Map markSet;

	public void runGC(Profile profileToGC) {
		markSet = new HashMap();
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

			try {
				ParameterizedSafeRunnable providerExecutor = new ParameterizedSafeRunnable(runAttribute, profileToGC);
				SafeRunner.run(providerExecutor);
				MarkSet[] inProfileRootSets = providerExecutor.getResult();
				if (inProfileRootSets[0] == null)
					return;

				for (int j = 0; j < inProfileRootSets.length; j++) {
					if (inProfileRootSets[j] == null) {
						continue;
					}
					ArrayList keysList = (ArrayList) markSet.get(inProfileRootSets[j].getRepo());
					if (keysList == null) {
						keysList = new ArrayList();
						markSet.put(inProfileRootSets[j].getRepo(), keysList);
					}
					addKeys(keysList, inProfileRootSets[j].getKeys());
				}

			} catch (ClassCastException e) {
				LogHelper.log(new Status(IStatus.ERROR, GCActivator.ID, Messages.CoreGarbageCollector_0, e));
				continue;
			}
		}

		//Complete each MarkSet with the MarkSets provided by all of the other registered Profiles
		traverseRegisteredProfiles();

		//Run the GC on each MarkSet
		Iterator keyIterator = markSet.keySet().iterator();
		while (keyIterator.hasNext()) {
			IArtifactRepository nextRepo = (IArtifactRepository) keyIterator.next();
			IArtifactKey[] keys = (IArtifactKey[]) ((ArrayList) markSet.get(nextRepo)).toArray(new IArtifactKey[0]);
			MarkSet aRootSet = new MarkSet(keys, nextRepo);
			CoreGarbageCollector.cleanRootSet(aRootSet);
		}
	}

	private void traverseRegisteredProfiles() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] configElts = registry.getConfigurationElementsFor(PT_MARKSET);
		for (int i = 0; i < configElts.length; i++) {
			if (!(configElts[i].getName().equals("run"))) { //$NON-NLS-1$
				continue;
			}
			IConfigurationElement runAttribute = configElts[i];
			if (runAttribute == null) {
				continue;
			}

			IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(GCActivator.getContext(), IProfileRegistry.class.getName());
			if (profileRegistry == null)
				return;
			Profile[] registeredProfiles = profileRegistry.getProfiles();

			for (int j = 0; j < registeredProfiles.length; j++) {
				contributeRootSets(runAttribute, registeredProfiles[j]);
			}
		}
	}

	private class ParameterizedSafeRunnable implements ISafeRunnable {
		IConfigurationElement cfg;
		Profile aProfile;
		MarkSet[] aProfileRootSets;

		public ParameterizedSafeRunnable(IConfigurationElement runtAttribute, Profile profile) {
			cfg = runtAttribute;
			aProfile = profile;
		}

		public void handleException(Throwable exception) {
			LogHelper.log(new Status(IStatus.ERROR, GCActivator.ID, Messages.GarbageCollector_3, exception));
		}

		public void run() throws Exception {
			IMarkSetProvider aRootSetProvider = (IMarkSetProvider) cfg.createExecutableExtension(ATTRIBUTE_CLASS);
			if (aRootSetProvider == null) {
				aProfileRootSets = null;
				return;
			}
			aProfileRootSets = aRootSetProvider.getRootSets(aProfile);
		}

		public MarkSet[] getResult() {
			return aProfileRootSets;
		}
	}

	private void contributeRootSets(IConfigurationElement runAttribute, Profile aProfile) {
		try {
			ParameterizedSafeRunnable providerExecutor = new ParameterizedSafeRunnable(runAttribute, aProfile);
			SafeRunner.run(providerExecutor);
			MarkSet[] aProfileRootSets = providerExecutor.getResult();

			if (aProfileRootSets[0] == null)
				return;
			for (int j = 0; j < aProfileRootSets.length; j++) {
				if (aProfileRootSets[j] == null) {
					continue;
				}

				//contribute any keys that are relevant to the Profile being GC'ed
				if (markSet.containsKey(aProfileRootSets[j].getRepo())) {
					ArrayList keysList = (ArrayList) markSet.get(aProfileRootSets[j].getRepo());
					addKeys(keysList, aProfileRootSets[j].getKeys());
					markSet.put(aProfileRootSets[j].getRepo(), keysList);
				}
			}

		} catch (ClassCastException e) {
			LogHelper.log(new Status(IStatus.ERROR, GCActivator.ID, Messages.CoreGarbageCollector_0, e));
		}
	}

	private void addKeys(ArrayList keyList, IArtifactKey[] keyArray) {
		for (int i = 0; i < keyArray.length; i++) {
			if (!(keyList.contains(keyArray[i]))) {
				keyList.add(keyArray[i]);
			}
		}
	}
}
