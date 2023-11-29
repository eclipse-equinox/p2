/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher;

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;

public class FeatureRootAdvice extends AbstractAdvice implements IFeatureRootAdvice {
	private static final int IDX_COMPUTER = 0;
	private static final int IDX_DESCRIPTOR = 1;

	// String config -> Object[] { GatheringComputer, Map: permission -> Set, String }
	private final Map<String, Object[]> advice = new HashMap<>();
	private String featureId;
	private Version featureVersion;

	@Override
	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		if (featureId != null && !featureId.equals(id))
			return false;
		if (featureVersion != null && !featureVersion.equals(version))
			return false;

		if (configSpec != null && !advice.containsKey(configSpec))
			return false;

		return true;
	}

	/**
	 * Return the configs for which we have advice
	 * @return String[]
	 */
	public String[] getConfigs() {
		return advice.keySet().toArray(new String[advice.size()]);
	}

	/**
	 * Return the GatheringComputer containing the set of rootfiles to include for the given config
	 * Returns null if we have no advice for the given config.
	 * @return GatheringComputer
	 */
	@Override
	public IPathComputer getRootFileComputer(String config) {
		if (advice.containsKey(config))
			return (GatheringComputer) advice.get(config)[IDX_COMPUTER];
		return null;
	}

	public void addRootfiles(String config, GatheringComputer computer) {
		Object[] configAdvice = getConfigAdvice(config);

		if (configAdvice[IDX_COMPUTER] == null)
			configAdvice[IDX_COMPUTER] = computer;
		else {
			GatheringComputer existing = (GatheringComputer) configAdvice[IDX_COMPUTER];
			existing.addAll(computer);
		}
		FileSetDescriptor descriptor = getDescriptor(config);
		descriptor.addFiles(computer.getFiles());
	}

	public void addPermissions(String config, String permissions, String[] files) {
		FileSetDescriptor descriptor = getDescriptor(config);
		for (String file : files) {
			descriptor.addPermissions(new String[]{permissions, file});
		}
	}

	public void addLinks(String config, String links) {
		FileSetDescriptor descriptor = getDescriptor(config);
		descriptor.setLinks(links);
	}

	private Object[] getConfigAdvice(String config) {
		Object[] configAdvice = advice.get(config);
		if (configAdvice == null) {
			configAdvice = new Object[3];
			advice.put(config, configAdvice);
		}
		return configAdvice;
	}

	@Override
	public FileSetDescriptor getDescriptor(String config) {
		Object[] configAdvice = getConfigAdvice(config);
		FileSetDescriptor descriptor = null;

		if (configAdvice[IDX_DESCRIPTOR] != null)
			descriptor = (FileSetDescriptor) configAdvice[IDX_DESCRIPTOR];
		else {
			String key = "root"; //$NON-NLS-1$
			if (config.length() > 0)
				key += "." + config; //$NON-NLS-1$
			descriptor = new FileSetDescriptor(key, config);
			configAdvice[IDX_DESCRIPTOR] = descriptor;
		}
		return descriptor;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public void setFeatureVersion(Version featureVersion) {
		this.featureVersion = featureVersion;
	}

	@Override
	public String[] getConfigurations() {
		Set<String> keys = advice.keySet();
		return keys.toArray(new String[keys.size()]);
	}

}
