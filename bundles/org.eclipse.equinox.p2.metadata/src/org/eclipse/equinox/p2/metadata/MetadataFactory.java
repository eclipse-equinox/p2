/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.util.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * A factory class for instantiating various p2 metadata objects.
 */
public class MetadataFactory {
	public static class InstallableUnitDescription {
		protected InstallableUnit unit;

		public InstallableUnitDescription() {
			super();
		}

		public void addTouchpointData(TouchpointData data) {
			Assert.isNotNull(data);
			unit().addTouchpointData(data);
		}

		public void setApplicabilityFilter(String ldapFilter) {
			unit().setApplicabilityFilter(ldapFilter);
		}

		public void setArtifacts(IArtifactKey[] value) {
			unit().setArtifacts(value);
		}

		public void setCapabilities(ProvidedCapability[] exportedCapabilities) {
			unit().setCapabilities(exportedCapabilities);
		}

		public void setFilter(String filter) {
			unit().setFilter(filter);
		}

		public void setId(String id) {
			unit().setId(id);
		}

		public void setProperty(String key, String value) {
			unit().setProperty(key, value);
		}

		public void setRequiredCapabilities(RequiredCapability[] capabilities) {
			unit().setRequiredCapabilities(capabilities);
		}

		public void setSingleton(boolean singleton) {
			unit().setSingleton(singleton);
		}

		public void setTouchpointType(TouchpointType type) {
			unit().setTouchpointType(type);
		}

		public void setVersion(Version newVersion) {
			unit().setVersion(newVersion);
		}

		InstallableUnit unit() {
			if (unit == null)
				unit = new InstallableUnit();
			return unit;
		}

		IInstallableUnit unitCreate() {
			IInstallableUnit result = unit();
			this.unit = null;
			return result;
		}
	}

	public static class InstallableUnitFragmentDescription extends InstallableUnitDescription {
		public void setHost(String hostId, VersionRange hostRange) {
			((InstallableUnitFragment) unit()).setHost(hostId, hostRange);
		}

		InstallableUnit unit() {
			if (unit == null)
				unit = new InstallableUnitFragment();
			return unit;
		}
	}

	/**
	 * Singleton touchpoint data for a touchpoint with no instructions.
	 */
	private static final TouchpointData EMPTY_TOUCHPOINT_DATA = new TouchpointData(Collections.EMPTY_MAP);

	private static TouchpointType[] typeCache = new TouchpointType[5];

	private static int typeCacheOffset;

	/**
	 * Creates and returns an {@link IInstallableUnit} based on the given 
	 * description.  Once the installable unit has been created, the information is 
	 * discarded from the description object.
	 * 
	 * @param description The description of the unit to create
	 * @return The created installable unit or fragment
	 */
	public static IInstallableUnit createInstallableUnit(InstallableUnitDescription description) {
		Assert.isNotNull(description);
		return description.unitCreate();
	}

	/**
	 * Creates and returns an {@link IInstallableUnitFragment} based on the given 
	 * description.  Once the fragment has been created, the information is 
	 * discarded from the description object.
	 * 
	 * @param description The description of the unit to create
	 * @return The created installable unit or fragment
	 */
	public static IInstallableUnitFragment createInstallableUnitFragment(InstallableUnitFragmentDescription description) {
		Assert.isNotNull(description);
		return (IInstallableUnitFragment) description.unitCreate();
	}

	/**
	 * Creates and returns an {@link IInstallableUnit} that represents the given
	 * unit bound to the given fragments.
	 * 
	 * @see IInstallableUnit#isResolved()
	 * @param unit The unit to be bound
	 * @param fragments The fragments to be bound
	 * @return A resolved installable unit
	 */
	public static IInstallableUnit createResolvedInstallableUnit(IInstallableUnit unit, IInstallableUnitFragment[] fragments) {
		if (unit.isResolved())
			return unit;
		Assert.isNotNull(unit);
		Assert.isNotNull(fragments);
		return new ResolvedInstallableUnit(unit, fragments);

	}

	/**
	 * Creates an instance of {@link TouchpointData} with the given instructions.
	 * 
	 * @param instructions The instructions for the touchpoint data.
	 * @return The created touchpoint data
	 */
	public static TouchpointData createTouchpointData(Map instructions) {
		Assert.isNotNull(instructions);
		//copy the map to protect against subsequent change by caller
		return instructions.isEmpty() ? EMPTY_TOUCHPOINT_DATA : new TouchpointData(new LinkedHashMap(instructions));
	}

	/**
	 * Creates a touchpoint type with the given id and version.
	 * 
	 * @param id The touchpoint id
	 * @param version The touchpoint version
	 * @return A touchpoint type instance with the given id and version
	 */
	public static TouchpointType createTouchpointType(String id, Version version) {
		TouchpointType result = getCachedTouchpointType(id, version);
		if (result != null)
			return result;
		result = new TouchpointType(id, version);
		putCachedTouchpointType(result);
		return result;
	}

	private static TouchpointType getCachedTouchpointType(String id, Version version) {
		synchronized (typeCache) {
			for (int i = 0; i < typeCache.length; i++) {
				if (typeCache[i] != null && typeCache[i].getId().equals(id) && typeCache[i].getVersion().equals(version))
					return typeCache[i];
			}
		}
		return null;
	}

	private static void putCachedTouchpointType(TouchpointType result) {
		//simple rotating buffer
		typeCache[typeCacheOffset] = result;
		typeCacheOffset = (typeCacheOffset + 1) % typeCache.length;
	}
}
