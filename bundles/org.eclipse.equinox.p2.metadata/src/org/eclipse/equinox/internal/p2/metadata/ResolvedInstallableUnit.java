/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 * 		IBM Corporation - initial API and implementation
 * 		Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.osgi.framework.Version;

public class ResolvedInstallableUnit implements IInstallableUnit {
	private static IInstallableUnit[] NO_IU = new IInstallableUnit[0];

	private IInstallableUnit[] fragments = NO_IU;
	protected IInstallableUnit original;

	public ResolvedInstallableUnit(IInstallableUnit resolved) {
		this.original = resolved;
	}

	public ResolvedInstallableUnit(IInstallableUnit resolved, IInstallableUnitFragment[] fragments) {
		this.original = resolved;
		this.fragments = fragments;
	}

	public IInstallableUnitFragment[] getFragments() {
		ArrayList result = new ArrayList();
		if (fragments != null)
			result.addAll(Arrays.asList(fragments));
		for (int i = 0; i < result.size(); i++) {
			IInstallableUnit fragment = (IInstallableUnit) result.get(i);
			if (fragment.isResolved())
				result.addAll(Arrays.asList(fragment.getFragments()));
		}
		return (IInstallableUnitFragment[]) result.toArray(new IInstallableUnitFragment[result.size()]);
	}

	public IArtifactKey[] getArtifacts() {
		return original.getArtifacts();
	}

	public String getFilter() {
		return original.getFilter();
	}

	public String getId() {
		return original.getId();
	}

	public String getProperty(String key) {
		return original.getProperty(key);
	}

	public Map getProperties() {
		return original.getProperties();
	}

	public ProvidedCapability[] getProvidedCapabilities() {
		ArrayList result = new ArrayList();
		result.addAll(Arrays.asList(original.getProvidedCapabilities()));
		for (int i = 0; i < fragments.length; i++) {
			result.addAll(Arrays.asList(fragments[i].getProvidedCapabilities()));
		}
		return original.getProvidedCapabilities();
	}

	public RequiredCapability[] getRequiredCapabilities() {
		ArrayList result = new ArrayList();
		result.addAll(Arrays.asList(original.getRequiredCapabilities()));
		for (int i = 0; i < fragments.length; i++) {
			result.addAll(Arrays.asList(fragments[i].getRequiredCapabilities()));
		}
		return (RequiredCapability[]) result.toArray(new RequiredCapability[result.size()]);

	}

	public TouchpointData[] getTouchpointData() {
		ArrayList result = new ArrayList();
		result.addAll(Arrays.asList(original.getTouchpointData()));
		for (int i = 0; i < fragments.length; i++) {
			TouchpointData[] data = fragments[i].getTouchpointData();
			for (int j = 0; j < data.length; j++) {
				result.add(data[j]);
			}
		}
		return (TouchpointData[]) result.toArray(new TouchpointData[result.size()]);
	}

	public TouchpointType getTouchpointType() {
		return original.getTouchpointType();
	}

	public Version getVersion() {
		return original.getVersion();
	}

	public boolean isFragment() {
		return original.isFragment();
	}

	public boolean isSingleton() {
		return original.isSingleton();
	}

	public boolean equals(Object obj) {
		//TODO This is pretty ugly....
		boolean result = original.equals(obj);
		if (result)
			return true;
		if (obj instanceof ResolvedInstallableUnit)
			return original.equals(((ResolvedInstallableUnit) obj).original);
		return false;
	}

	public int hashCode() {
		// TODO Auto-generated method stub
		return original.hashCode();
	}

	public String toString() {
		return "[R]" + original.toString(); //$NON-NLS-1$
	}

	public IInstallableUnit getOriginal() {
		return original;
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

	public boolean isResolved() {
		return true;
	}

	public IInstallableUnit unresolved() {
		return original.unresolved();
	}

	public IUpdateDescriptor getUpdateDescriptor() {
		return original.getUpdateDescriptor();
	}

	public License getLicense() {
		return original.getLicense();
	}

	public Copyright getCopyright() {
		return original.getCopyright();
	}

	public boolean satisfies(RequiredCapability candidate) {
		ProvidedCapability[] provides = getProvidedCapabilities();
		for (int i = 0; i < provides.length; i++)
			if (provides[i].satisfies(candidate))
				return true;
		return false;
	}

}
