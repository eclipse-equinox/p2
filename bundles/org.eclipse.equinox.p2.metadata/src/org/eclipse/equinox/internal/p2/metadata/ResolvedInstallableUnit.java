/*******************************************************************************
 *  Copyright (c) 2007, 2023 IBM Corporation and others.
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
	private final List<IInstallableUnitFragment> fragments;
	protected final IInstallableUnit original;

	public static final String MEMBER_ORIGINAL = "original"; //$NON-NLS-1$
	public static final String MEMBER_FRAGMENTS = "fragments"; //$NON-NLS-1$

	public ResolvedInstallableUnit(IInstallableUnit resolved) {
		this(resolved, List.of());
	}

	public ResolvedInstallableUnit(IInstallableUnit resolved, List<IInstallableUnitFragment> fragments) {
		this.original = resolved;
		this.fragments = fragments;
	}

	@Override
	public Collection<IInstallableUnitFragment> getFragments() {
		return withFragmentElements(fragments, f -> f.isResolved() ? f.getFragments() : List.of());
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
		return withFragmentElements(original.getProvidedCapabilities(), IInstallableUnit::getProvidedCapabilities);
	}

	@Override
	public Collection<IRequirement> getRequirements() {
		return withFragmentElements(original.getRequirements(), IInstallableUnit::getRequirements);
	}

	@Override
	public Collection<IRequirement> getMetaRequirements() {
		return withFragmentElements(original.getMetaRequirements(), IInstallableUnit::getMetaRequirements);
	}

	@Override
	public Collection<ITouchpointData> getTouchpointData() {
		return withFragmentElements(original.getTouchpointData(), IInstallableUnit::getTouchpointData);
	}

	private <T> Collection<T> withFragmentElements(Collection<T> elements,
			Function<IInstallableUnit, Collection<T>> getter) {
		if (fragments.isEmpty()) {
			return elements;
		}
		return Stream.concat(elements.stream(), fragments.stream().map(getter).flatMap(Collection::stream))
				.collect(Collectors.toCollection(ArrayList::new));
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
		// TODO This is pretty ugly....
		boolean result = original.equals(obj);
		if (result) {
			return true;
		}
		return obj instanceof ResolvedInstallableUnit unit && original.equals(unit.original);
	}

	@Override
	public int hashCode() {
		return original.hashCode();
	}

	@Override
	public String toString() {
		return "[R]" + original; //$NON-NLS-1$
	}

	public IInstallableUnit getOriginal() {
		return original;
	}

	@Override
	public int compareTo(IInstallableUnit other) {
		return InstallableUnit.ID_FIRST_THEN_VERSION.compare(this, other);
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

	@Override
	public Object getMember(String memberName) {
		return switch (memberName) {
		case MEMBER_FRAGMENTS -> fragments.toArray(IInstallableUnitFragment[]::new);
		case MEMBER_ORIGINAL -> original;
		case InstallableUnit.MEMBER_PROVIDED_CAPABILITIES -> getProvidedCapabilities();
		case InstallableUnit.MEMBER_ID -> getId();
		case InstallableUnit.MEMBER_VERSION -> getVersion();
		case InstallableUnit.MEMBER_PROPERTIES -> getProperties();
		case InstallableUnit.MEMBER_FILTER -> getFilter();
		case InstallableUnit.MEMBER_ARTIFACTS -> getArtifacts();
		case InstallableUnit.MEMBER_REQUIREMENTS -> getRequirements();
		case InstallableUnit.MEMBER_LICENSES -> getLicenses();
		case InstallableUnit.MEMBER_COPYRIGHT -> getCopyright();
		case InstallableUnit.MEMBER_TOUCHPOINT_DATA -> getTouchpointData();
		case InstallableUnit.MEMBER_TOUCHPOINT_TYPE -> getTouchpointType();
		case InstallableUnit.MEMBER_UPDATE_DESCRIPTOR -> getUpdateDescriptor();
		case InstallableUnit.MEMBER_SINGLETON -> isSingleton();
		default -> throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
		};
	}

}
