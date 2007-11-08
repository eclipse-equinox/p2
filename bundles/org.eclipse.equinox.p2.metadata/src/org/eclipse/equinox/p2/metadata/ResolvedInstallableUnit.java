/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.InternalInstallableUnit;
import org.osgi.framework.Version;

public class ResolvedInstallableUnit implements IResolvedInstallableUnit, InternalInstallableUnit {
	private static IInstallableUnit[] NO_IU = new IInstallableUnit[0];

	private IInstallableUnit[] fragments = NO_IU;
	protected IInstallableUnit resolved;

	public ResolvedInstallableUnit(IInstallableUnit resolved) {
		this.resolved = resolved;
	}

	public void setFragments(IResolvedInstallableUnit[] fragments) {
		this.fragments = fragments;
	}

	public IInstallableUnitFragment[] getFragments() {
		ArrayList result = new ArrayList();
		if (fragments != null)
			result.addAll(Arrays.asList(fragments));
		for (int i = 0; i < result.size(); i++) {
			result.addAll(Arrays.asList(((IResolvedInstallableUnit) result.get(i)).getFragments()));
		}
		return (IInstallableUnitFragment[]) result.toArray(new IInstallableUnitFragment[result.size()]);
	}

	public String getApplicabilityFilter() {
		return resolved.getApplicabilityFilter();
	}

	public IArtifactKey[] getArtifacts() {
		return resolved.getArtifacts();
	}

	public String getFilter() {
		return resolved.getFilter();
	}

	public String getId() {
		return resolved.getId();
	}

	public String getProperty(String key) {
		return resolved.getProperty(key);
	}

	public Map getProperties() {
		return resolved.getProperties();
	}

	public ProvidedCapability[] getProvidedCapabilities() {
		ArrayList result = new ArrayList();
		result.addAll(Arrays.asList(resolved.getProvidedCapabilities()));
		for (int i = 0; i < fragments.length; i++) {
			result.addAll(Arrays.asList(fragments[i].getProvidedCapabilities()));
		}
		return resolved.getProvidedCapabilities();
	}

	public RequiredCapability[] getRequiredCapabilities() {
		ArrayList result = new ArrayList();
		result.addAll(Arrays.asList(resolved.getRequiredCapabilities()));
		for (int i = 0; i < fragments.length; i++) {
			result.addAll(Arrays.asList(fragments[i].getRequiredCapabilities()));
		}
		return (RequiredCapability[]) result.toArray(new RequiredCapability[result.size()]);

	}

	public TouchpointData[] getTouchpointData() {
		ArrayList result = new ArrayList();
		result.addAll(Arrays.asList(resolved.getTouchpointData()));
		for (int i = 0; i < fragments.length; i++) {
			TouchpointData[] data = fragments[i].getTouchpointData();
			for (int j = 0; j < data.length; j++) {
				result.add(data[j]);
			}
		}
		return (TouchpointData[]) result.toArray(new TouchpointData[result.size()]);
	}

	public TouchpointType getTouchpointType() {
		return resolved.getTouchpointType();
	}

	public Version getVersion() {
		return resolved.getVersion();
	}

	public boolean isFragment() {
		return resolved.isFragment();
	}

	public boolean isSingleton() {
		return resolved.isSingleton();
	}

	public void accept(IMetadataVisitor visitor) {
		visitor.visitInstallableUnit(this);
	}

	public boolean equals(Object obj) {
		//TODO This is pretty ugly....
		boolean result = resolved.equals(obj);
		if (result)
			return true;
		if (obj instanceof ResolvedInstallableUnit)
			return resolved.equals(((ResolvedInstallableUnit) obj).resolved);
		return false;
	}

	public int hashCode() {
		// TODO Auto-generated method stub
		return resolved.hashCode();
	}

	public String toString() {
		return "[R]" + resolved.toString(); //$NON-NLS-1$
	}

	public IInstallableUnit getOriginal() {
		return resolved;
	}

	public int compareTo(Object toCompareTo) {
		if (!(toCompareTo instanceof IInstallableUnit)) {
			return -1;
		}
		IInstallableUnit other = (IInstallableUnit) toCompareTo;
		if (getId().compareTo(other.getId()) == 0)
			return (getVersion().compareTo(other.getVersion()));
		return getId().compareTo(other.getId());
	}

	public IResolvedInstallableUnit getResolved() {
		return this;
	}

}
