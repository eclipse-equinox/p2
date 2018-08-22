/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 * 		IBM Corporation - initial API and implementation
 * 		Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.ICopyright;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.metadata.expression.IMemberProvider;

public class ResolvedInstallableUnit implements IInstallableUnit, IMemberProvider {
	private static IInstallableUnitFragment[] NO_IU = new IInstallableUnitFragment[0];

	private final IInstallableUnitFragment[] fragments;
	protected final IInstallableUnit original;

	public static final String MEMBER_ORIGINAL = "original"; //$NON-NLS-1$
	public static final String MEMBER_FRAGMENTS = "fragments"; //$NON-NLS-1$

	public ResolvedInstallableUnit(IInstallableUnit resolved) {
		this(resolved, null);
	}

	public ResolvedInstallableUnit(IInstallableUnit resolved, IInstallableUnitFragment[] fragments) {
		this.original = resolved;
		this.fragments = fragments == null ? NO_IU : fragments;
	}

	@Override
	public Collection<IInstallableUnitFragment> getFragments() {
		int fcount = fragments.length;
		if (fcount == 0)
			return Collections.emptyList();

		ArrayList<IInstallableUnitFragment> result = new ArrayList<>(fcount);
		result.addAll(Arrays.asList(fragments));
		for (int i = 0; i < fcount; i++) {
			IInstallableUnit fragment = fragments[i];
			if (fragment.isResolved())
				result.addAll(fragment.getFragments());
		}
		return result;
	}

	@Override
	public Collection<IArtifactKey> getArtifacts() {
		return original.getArtifacts();
	}

	@Override
	public IMatchExpression<IInstallableUnit> getFilter() {
		return original.getFilter();
	}

	@Override
	public String getId() {
		return original.getId();
	}

	@Override
	public String getProperty(String key) {
		return original.getProperty(key);
	}

	@Override
	public Map<String, String> getProperties() {
		return original.getProperties();
	}

	@Override
	public String getProperty(String key, String locale) {
		return original.getProperty(key, locale);
	}

	@Override
	public Collection<IProvidedCapability> getProvidedCapabilities() {
		Collection<IProvidedCapability> originalCapabilities = original.getProvidedCapabilities();
		if (fragments.length == 0)
			return originalCapabilities;

		ArrayList<IProvidedCapability> result = new ArrayList<>(originalCapabilities);
		for (int i = 0; i < fragments.length; i++)
			result.addAll(fragments[i].getProvidedCapabilities());
		return result;
	}

	@Override
	public Collection<IRequirement> getRequirements() {
		Collection<IRequirement> originalCapabilities = original.getRequirements();
		if (fragments.length == 0)
			return originalCapabilities;

		ArrayList<IRequirement> result = new ArrayList<>(originalCapabilities);
		for (int i = 0; i < fragments.length; i++)
			result.addAll(fragments[i].getRequirements());
		return result;
	}

	@Override
	public Collection<IRequirement> getMetaRequirements() {
		Collection<IRequirement> originalCapabilities = original.getMetaRequirements();
		if (fragments.length == 0)
			return originalCapabilities;

		ArrayList<IRequirement> result = new ArrayList<>(originalCapabilities);
		for (int i = 0; i < fragments.length; i++)
			result.addAll(fragments[i].getMetaRequirements());
		return result;
	}

	@Override
	public Collection<ITouchpointData> getTouchpointData() {
		Collection<ITouchpointData> originalTouchpointData = original.getTouchpointData();
		if (fragments.length == 0)
			return originalTouchpointData;

		ArrayList<ITouchpointData> result = new ArrayList<>(originalTouchpointData);
		for (int i = 0; i < fragments.length; i++)
			result.addAll(fragments[i].getTouchpointData());
		return result;
	}

	@Override
	public ITouchpointType getTouchpointType() {
		return original.getTouchpointType();
	}

	@Override
	public Version getVersion() {
		return original.getVersion();
	}

	@Override
	public boolean isSingleton() {
		return original.isSingleton();
	}

	@Override
	public boolean equals(Object obj) {
		//TODO This is pretty ugly....
		boolean result = original.equals(obj);
		if (result)
			return true;
		if (obj instanceof ResolvedInstallableUnit)
			return original.equals(((ResolvedInstallableUnit) obj).original);
		return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return original.hashCode();
	}

	@Override
	public String toString() {
		return "[R]" + original.toString(); //$NON-NLS-1$
	}

	public IInstallableUnit getOriginal() {
		return original;
	}

	@Override
	public int compareTo(IInstallableUnit other) {
		int cmp = getId().compareTo(other.getId());
		if (cmp == 0)
			cmp = getVersion().compareTo(other.getVersion());
		return cmp;
	}

	@Override
	public boolean isResolved() {
		return true;
	}

	@Override
	public IInstallableUnit unresolved() {
		return original.unresolved();
	}

	@Override
	public IUpdateDescriptor getUpdateDescriptor() {
		return original.getUpdateDescriptor();
	}

	@Override
	public Collection<ILicense> getLicenses() {
		return original.getLicenses();
	}

	@Override
	public Collection<ILicense> getLicenses(String locale) {
		return original.getLicenses(locale);
	}

	@Override
	public ICopyright getCopyright() {
		return original.getCopyright();
	}

	@Override
	public ICopyright getCopyright(String locale) {
		return original.getCopyright(locale);
	}

	@Override
	public boolean satisfies(IRequirement candidate) {
		return candidate.isMatch(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.metadata.expression.IMemberProvider#getMember(java.lang.String)
	 */
	@Override
	public Object getMember(String memberName) {
		if (MEMBER_FRAGMENTS == memberName)
			return fragments;
		if (MEMBER_ORIGINAL == memberName)
			return original;
		if (InstallableUnit.MEMBER_PROVIDED_CAPABILITIES == memberName)
			return getProvidedCapabilities();
		if (InstallableUnit.MEMBER_ID == memberName)
			return getId();
		if (InstallableUnit.MEMBER_VERSION == memberName)
			return getVersion();
		if (InstallableUnit.MEMBER_PROPERTIES == memberName)
			return getProperties();
		if (InstallableUnit.MEMBER_FILTER == memberName)
			return getFilter();
		if (InstallableUnit.MEMBER_ARTIFACTS == memberName)
			return getArtifacts();
		if (InstallableUnit.MEMBER_REQUIREMENTS == memberName)
			return getRequirements();
		if (InstallableUnit.MEMBER_LICENSES == memberName)
			return getLicenses();
		if (InstallableUnit.MEMBER_COPYRIGHT == memberName)
			return getCopyright();
		if (InstallableUnit.MEMBER_TOUCHPOINT_DATA == memberName)
			return getTouchpointData();
		if (InstallableUnit.MEMBER_TOUCHPOINT_TYPE == memberName)
			return getTouchpointType();
		if (InstallableUnit.MEMBER_UPDATE_DESCRIPTOR == memberName)
			return getUpdateDescriptor();
		if (InstallableUnit.MEMBER_SINGLETON == memberName)
			return Boolean.valueOf(isSingleton());
		throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
	}

}
