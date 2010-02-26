/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC
 *		EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.osgi.framework.Filter;

/**
 * A factory class for instantiating various p2 metadata objects.
 */
public class MetadataFactory {
	/**
	 * A description containing information about an installable unit. Once created,
	 * installable units are immutable. This description class allows a client to build
	 * up the state for an installable unit incrementally, and then finally product
	 * the resulting immutable unit.
	 */
	public static class InstallableUnitDescription {
		public static final String PROP_TYPE_GROUP = "org.eclipse.equinox.p2.type.group"; //$NON-NLS-1$

		protected InstallableUnit unit;

		/**
		 * A property key (value <code>"org.eclipse.equinox.p2.type.patch"</code>) for a 
		 * boolean property indicating that an installable unit is a group.
		 * 
		 */
		public static final String PROP_TYPE_PATCH = "org.eclipse.equinox.p2.type.patch"; //$NON-NLS-1$

		/**
		 * A property key (value <code>"org.eclipse.equinox.p2.type.fragment"</code>) for a 
		 * boolean property indicating that an installable unit is a fragment.
		 * 
		 */
		public static final String PROP_TYPE_FRAGMENT = "org.eclipse.equinox.p2.type.fragment"; //$NON-NLS-1$

		/**
		 * A property key (value <code>"org.eclipse.equinox.p2.type.category"</code>) for a 
		 * boolean property indicating that an installable unit is a category.
		 * 
		 */
		public static final String PROP_TYPE_CATEGORY = "org.eclipse.equinox.p2.type.category"; //$NON-NLS-1$

		public InstallableUnitDescription() {
			super();
		}

		public void addProvidedCapabilities(Collection<IProvidedCapability> additional) {
			if (additional == null || additional.size() == 0)
				return;
			Collection<IProvidedCapability> current = unit().getProvidedCapabilities();
			ArrayList<IProvidedCapability> all = new ArrayList<IProvidedCapability>(additional.size() + current.size());
			all.addAll(current);
			all.addAll(additional);
			unit().setCapabilities(all.toArray(new IProvidedCapability[all.size()]));
		}

		public void addRequiredCapabilities(Collection<IRequirement> additional) {
			if (additional == null || additional.size() == 0)
				return;
			List<IRequirement> current = unit().getRequiredCapabilities();
			ArrayList<IRequirement> all = new ArrayList<IRequirement>(additional.size() + current.size());
			all.addAll(current);
			all.addAll(additional);
			unit().setRequiredCapabilities(all.toArray(new IRequirement[all.size()]));
		}

		public void addTouchpointData(ITouchpointData data) {
			Assert.isNotNull(data);
			unit().addTouchpointData(data);
		}

		public String getId() {
			return unit().getId();
		}

		public Collection<IProvidedCapability> getProvidedCapabilities() {
			return unit().getProvidedCapabilities();
		}

		public List<IRequirement> getRequiredCapabilities() {
			return unit().getRequiredCapabilities();
		}

		public Collection<IRequirement> getMetaRequiredCapabilities() {
			return unit().getMetaRequirements();
		}

		/**
		 * Returns the current touchpoint data on this installable unit description. The
		 * touchpoint data may change if further data is added to the description.
		 * 
		 * @return The current touchpoint data on this description
		 */
		public Collection<ITouchpointData> getTouchpointData() {
			return unit().getTouchpointData();

		}

		public Version getVersion() {
			return unit().getVersion();
		}

		public void setArtifacts(IArtifactKey[] value) {
			unit().setArtifacts(value);
		}

		public void setCapabilities(IProvidedCapability[] exportedCapabilities) {
			unit().setCapabilities(exportedCapabilities);
		}

		public void setCopyright(ICopyright copyright) {
			unit().setCopyright(copyright);
		}

		public void setFilter(Filter filter) {
			unit().setFilter(filter);
		}

		public void setFilter(String filter) {
			unit().setFilter(filter);
		}

		public void setId(String id) {
			unit().setId(id);
		}

		public void setLicenses(ILicense[] licenses) {
			unit().setLicenses(licenses);
		}

		public void setProperty(String key, String value) {
			unit().setProperty(key, value);
		}

		public void setRequiredCapabilities(IRequirement[] requirements) {
			unit().setRequiredCapabilities(requirements);
		}

		public void setMetaRequiredCapabilities(IRequirement[] metaRequirements) {
			unit().setMetaRequiredCapabilities(metaRequirements);
		}

		public void setSingleton(boolean singleton) {
			unit().setSingleton(singleton);
		}

		public void setTouchpointType(ITouchpointType type) {
			unit().setTouchpointType(type);
		}

		public void setUpdateDescriptor(IUpdateDescriptor updateInfo) {
			unit().setUpdateDescriptor(updateInfo);
		}

		public void setVersion(Version newVersion) {
			unit().setVersion(newVersion);
		}

		InstallableUnit unit() {
			if (unit == null) {
				unit = new InstallableUnit();
				unit.setArtifacts(new IArtifactKey[0]);
			}
			return unit;
		}

		IInstallableUnit unitCreate() {
			IInstallableUnit result = unit();
			this.unit = null;
			return result;
		}
	}

	public static class InstallableUnitFragmentDescription extends InstallableUnitDescription {
		public InstallableUnitFragmentDescription() {
			super();
			setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		}

		public void setHost(IRequirement[] hostRequirements) {
			((InstallableUnitFragment) unit()).setHost(hostRequirements);
		}

		InstallableUnit unit() {
			if (unit == null)
				unit = new InstallableUnitFragment();
			return unit;
		}
	}

	public static class InstallableUnitPatchDescription extends InstallableUnitDescription {

		public InstallableUnitPatchDescription() {
			super();
			setProperty(InstallableUnitDescription.PROP_TYPE_PATCH, Boolean.TRUE.toString());
		}

		public void setApplicabilityScope(IRequirement[][] applyTo) {
			if (applyTo == null)
				throw new IllegalArgumentException("A patch scope can not be null"); //$NON-NLS-1$
			((InstallableUnitPatch) unit()).setApplicabilityScope(applyTo);
		}

		public void setLifeCycle(IRequirement lifeCycle) {
			((InstallableUnitPatch) unit()).setLifeCycle(lifeCycle);
		}

		public void setRequirementChanges(IRequirementChange[] changes) {
			((InstallableUnitPatch) unit()).setRequirementsChange(changes);
		}

		InstallableUnit unit() {
			if (unit == null) {
				unit = new InstallableUnitPatch();
				((InstallableUnitPatch) unit()).setApplicabilityScope(new IRequirement[0][0]);
			}
			return unit;
		}
	}

	/**
	 * Singleton touchpoint data for a touchpoint with no instructions.
	 */
	private static final ITouchpointData EMPTY_TOUCHPOINT_DATA = new TouchpointData(CollectionUtils.<String, ITouchpointInstruction> emptyMap());

	private static ITouchpointType[] typeCache = new ITouchpointType[5];

	private static int typeCacheOffset;

	/**
	 * Returns an {@link IInstallableUnit} based on the given 
	 * description.  Once the installable unit has been created, the information is 
	 * discarded from the description object.
	 * 
	 * @param description The description of the unit to create
	 * @return The created installable unit
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
	 * @return The created installable unit fragment
	 */
	public static IInstallableUnitFragment createInstallableUnitFragment(InstallableUnitFragmentDescription description) {
		Assert.isNotNull(description);
		return (IInstallableUnitFragment) description.unitCreate();
	}

	/**
	 * Returns an {@link IInstallableUnitPatch} based on the given 
	 * description.  Once the patch installable unit has been created, the information is 
	 * discarded from the description object.
	 * 
	 * @param description The description of the unit to create
	 * @return The created installable unit patch
	 */
	public static IInstallableUnitPatch createInstallableUnitPatch(InstallableUnitPatchDescription description) {
		Assert.isNotNull(description);
		return (IInstallableUnitPatch) description.unitCreate();
	}

	/**
	 * Returns a {@link IProvidedCapability} with the given values.
	 * 
	 * @param namespace The capability namespace
	 * @param name The capability name
	 * @param version The capability version
	 */
	public static IProvidedCapability createProvidedCapability(String namespace, String name, Version version) {
		return new ProvidedCapability(namespace, name, version);
	}

	/**
	 * Returns a {@link IRequirement} with the given values.
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
	public static IRequiredCapability createRequiredCapability(String namespace, String name, VersionRange range, Filter filter, boolean optional, boolean multiple) {
		return new RequiredCapability(namespace, name, range, filter, optional ? 0 : 1, multiple ? Integer.MAX_VALUE : 1, true);
	}

	public static IRequirement createRequiredCapability(String namespace, String name, VersionRange range, Filter filter, int minCard, int maxCard, boolean greedy) {
		return new RequiredCapability(namespace, name, range, filter, minCard, maxCard, greedy);
	}

	public static IRequirement createRequirement(IMatchExpression<IInstallableUnit> requirement) {
		return new RequiredCapability(requirement);

	}

	public static IRequiredCapability createRequiredCapability(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple, boolean greedy) {
		return new RequiredCapability(namespace, name, range, filter, optional, multiple, greedy);
	}

	/**
	 * Returns a new requirement change.
	 * @param applyOn The source of the requirement change - the kind of requirement to apply the change to
	 * @param newValue The result of the requirement change - the requirement to replace the source requirement with
	 * @return a requirement change
	 */
	public static IRequirementChange createRequirementChange(IRequirement applyOn, IRequirement newValue) {
		if ((applyOn == null || applyOn instanceof IRequiredCapability) && (newValue == null || newValue instanceof IRequiredCapability))
			return new RequirementChange((IRequiredCapability) applyOn, (IRequiredCapability) newValue);
		throw new IllegalArgumentException();
	}

	/**
	 * Returns a new {@link ICopyright}.
	 * @param location the location of a document containing the copyright notice, or <code>null</code>
	 * @param body the copyright body, cannot be <code>null</code>
	 * @throws IllegalArgumentException when the <code>body</code> is <code>null</code>
	 */
	public static ICopyright createCopyright(URI location, String body) {
		return new Copyright(location, body);
	}

	/**
	 * Return a new {@link ILicense}
	 * The body should contain either the full text of the license or an summary for a license
	 * fully specified in the given location.
	 * 
	 * @param location the location of a document containing the full license, or <code>null</code>
	 * @param body the license body, cannot be <code>null</code>
	 * @throws IllegalArgumentException when the <code>body</code> is <code>null</code>
	 */
	public static ILicense createLicense(URI location, String body) {
		return new License(location, body, null);
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
	 * Returns an instance of {@link ITouchpointData} with the given instructions.
	 * 
	 * @param instructions The instructions for the touchpoint data.
	 * @return The created touchpoint data
	 */
	public static ITouchpointData createTouchpointData(Map<String, ? extends Object> instructions) {
		Assert.isNotNull(instructions);
		//copy the map to protect against subsequent change by caller
		if (instructions.isEmpty())
			return EMPTY_TOUCHPOINT_DATA;

		Map<String, ITouchpointInstruction> result = new LinkedHashMap<String, ITouchpointInstruction>(instructions.size());

		for (Entry<String, ? extends Object> entry : instructions.entrySet()) {
			Object value = entry.getValue();
			ITouchpointInstruction instruction;
			if (value == null || value instanceof String)
				instruction = createTouchpointInstruction((String) value, null);
			else
				instruction = (ITouchpointInstruction) value;
			result.put(entry.getKey(), instruction);
		}
		return new TouchpointData(result);
	}

	/**
	 * Merge the given touchpoint instructions with a pre-existing touchpoint data
	 * @param initial - the initial ITouchpointData
	 * @param incomingInstructions - Map of ITouchpointInstructions to merge into the initial touchpoint data
	 * @return the merged ITouchpointData
	 */
	public static ITouchpointData mergeTouchpointData(ITouchpointData initial, Map<String, ITouchpointInstruction> incomingInstructions) {
		if (incomingInstructions == null || incomingInstructions.size() == 0)
			return initial;

		Map<String, ITouchpointInstruction> resultInstructions = new HashMap<String, ITouchpointInstruction>(initial.getInstructions());
		for (String key : incomingInstructions.keySet()) {
			ITouchpointInstruction instruction = incomingInstructions.get(key);
			ITouchpointInstruction existingInstruction = resultInstructions.get(key);

			if (existingInstruction != null) {
				String body = existingInstruction.getBody();
				if (body == null || body.length() == 0)
					body = instruction.getBody();
				else if (instruction.getBody() != null) {
					if (!body.endsWith(";")) //$NON-NLS-1$
						body += ';';
					body += instruction.getBody();
				}

				String importAttribute = existingInstruction.getImportAttribute();
				if (importAttribute == null || importAttribute.length() == 0)
					importAttribute = instruction.getImportAttribute();
				else if (instruction.getImportAttribute() != null) {
					if (!importAttribute.endsWith(",")) //$NON-NLS-1$
						importAttribute += ',';
					importAttribute += instruction.getBody();
				}
				instruction = createTouchpointInstruction(body, importAttribute);
			}
			resultInstructions.put(key, instruction);
		}
		return createTouchpointData(resultInstructions);
	}

	public static ITouchpointInstruction createTouchpointInstruction(String body, String importAttribute) {
		return new TouchpointInstruction(body, importAttribute);
	}

	/**
	 * Returns a {@link TouchpointType} with the given id and version.
	 * 
	 * @param id The touchpoint id
	 * @param version The touchpoint version
	 * @return A touchpoint type instance with the given id and version
	 */
	public static ITouchpointType createTouchpointType(String id, Version version) {
		Assert.isNotNull(id);
		Assert.isNotNull(version);

		if (id.equals(ITouchpointType.NONE.getId()) && version.equals(ITouchpointType.NONE.getVersion()))
			return ITouchpointType.NONE;

		synchronized (typeCache) {
			ITouchpointType result = getCachedTouchpointType(id, version);
			if (result != null)
				return result;
			result = new TouchpointType(id, version);
			putCachedTouchpointType(result);
			return result;
		}
	}

	public static IUpdateDescriptor createUpdateDescriptor(String id, VersionRange range, int severity, String description) {
		return new UpdateDescriptor(id, range, severity, description);
	}

	private static ITouchpointType getCachedTouchpointType(String id, Version version) {
		for (int i = 0; i < typeCache.length; i++) {
			if (typeCache[i] != null && typeCache[i].getId().equals(id) && typeCache[i].getVersion().equals(version))
				return typeCache[i];
		}
		return null;
	}

	private static void putCachedTouchpointType(ITouchpointType result) {
		//simple rotating buffer
		typeCache[typeCacheOffset] = result;
		typeCacheOffset = (typeCacheOffset + 1) % typeCache.length;
	}
}
