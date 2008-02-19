/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;

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

		public void setLicense(License license) {
			unit().setLicense(license);
		}

		public void setCopyright(Copyright copyright) {
			unit().setCopyright(copyright);
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

		public void setUpdateDescriptor(IUpdateDescriptor updateInfo) {
			unit().setUpdateDescriptor(updateInfo);
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
	 * Returns an {@link IInstallableUnit} based on the given 
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
	 * Returns an {@link IInstallableUnitFragment} based on the given 
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
	 * Returns a {@link ProvidedCapability} with the given values.
	 * 
	 * @param namespace The capability namespace
	 * @param name The capability name
	 * @param version The capability version
	 */
	public static ProvidedCapability createProvidedCapability(String namespace, String name, Version version) {
		return new ProvidedCapability(namespace, name, version);
	}

	/**
	 * Returns a {@link RequiredCapability} with the given values.
	 * 
	 * @param namespace The capability namespace
	 * @param name The required capability name
	 * @param range The range of versions that are required, or <code>null</code>
	 * to indicate that any version will do.
	 * @param filter The filter used to evaluate whether this capability is applicable in the
	 * current environment, or <code>null</code> to indicate this capability is always applicable
	 * @param optional <code>true</code> if this required capability is optional,
	 * and <code>false</code> otherwise.
	 * @param multiple <code>true</code> if this capability can be satisfied by multiple provided capabilities, or it requires exactly one match
	 */
	public static RequiredCapability createRequiredCapability(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple) {
		return new RequiredCapability(namespace, name, range, filter, optional, multiple);
	}

	public static RequiredCapability createRequiredCapability(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple, boolean greedy) {
		return new RequiredCapability(namespace, name, range, filter, optional, multiple, greedy);
	}

	/**
	 * Returns an {@link IInstallableUnit} that represents the given
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
	 * Returns an instance of {@link TouchpointData} with the given instructions.
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
	 * Returns a {@link TouchpointType} with the given id and version.
	 * 
	 * @param id The touchpoint id
	 * @param version The touchpoint version
	 * @return A touchpoint type instance with the given id and version
	 */
	public static TouchpointType createTouchpointType(String id, Version version) {
		Assert.isNotNull(id);
		Assert.isNotNull(version);
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

	public static IUpdateDescriptor createUpdateDescriptor(String id, VersionRange range, int severity, String description) {
		return new UpdateDescriptor(id, range, severity, description);
	}

	public static License createLicense(String url, String licenseBody) {
		return new License(url, licenseBody);
	}

	public static Copyright createCopyright(String url, String copyrightBody) {
		return new Copyright(url, copyrightBody);
	}

}
