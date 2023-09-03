/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC
 *		EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.metadata.Copyright;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnitFragment;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnitPatch;
import org.eclipse.equinox.internal.p2.metadata.License;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredPropertiesMatch;
import org.eclipse.equinox.internal.p2.metadata.Requirement;
import org.eclipse.equinox.internal.p2.metadata.RequirementChange;
import org.eclipse.equinox.internal.p2.metadata.ResolvedInstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.TouchpointData;
import org.eclipse.equinox.internal.p2.metadata.TouchpointInstruction;
import org.eclipse.equinox.internal.p2.metadata.TouchpointType;
import org.eclipse.equinox.internal.p2.metadata.UpdateDescriptor;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IFilterExpression;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;

/**
 * A factory class for instantiating various p2 metadata objects.
 * 
 * @since 2.0
 */
public final class MetadataFactory {
	/**
	 * A description containing information about an installable unit. Once created,
	 * installable units are immutable. This description class allows a client to
	 * build up the state for an installable unit incrementally, and then finally
	 * produce the resulting immutable unit.
	 */
	public static class InstallableUnitDescription {
		InstallableUnit unit;

		/**
		 * A property key (value <code>"org.eclipse.equinox.p2.type.patch"</code>) for a
		 * boolean property indicating that an installable unit is a group.
		 * 
		 */
		public static final String PROP_TYPE_GROUP = "org.eclipse.equinox.p2.type.group"; //$NON-NLS-1$

		/**
		 * A property key (value <code>"org.eclipse.equinox.p2.type.patch"</code>) for a
		 * boolean property indicating that an installable unit is a patch.
		 * 
		 */
		public static final String PROP_TYPE_PATCH = "org.eclipse.equinox.p2.type.patch"; //$NON-NLS-1$

		/**
		 * A property key (value <code>"org.eclipse.equinox.p2.type.fragment"</code>)
		 * for a boolean property indicating that an installable unit is a fragment.
		 * 
		 */
		public static final String PROP_TYPE_FRAGMENT = "org.eclipse.equinox.p2.type.fragment"; //$NON-NLS-1$

		/**
		 * A property key (value <code>"org.eclipse.equinox.p2.type.category"</code>)
		 * for a boolean property indicating that an installable unit is a category.
		 * 
		 */
		public static final String PROP_TYPE_CATEGORY = "org.eclipse.equinox.p2.type.category"; //$NON-NLS-1$

		/**
		 * A property key (value <code>"org.eclipse.equinox.p2.type.product"</code>) for
		 * a boolean property indicating that an installable unit is the root IU of a
		 * product.
		 * 
		 * @since 2.2
		 */
		public static final String PROP_TYPE_PRODUCT = "org.eclipse.equinox.p2.type.product"; //$NON-NLS-1$

		public InstallableUnitDescription() {
			super();
		}

		/**
		 * Add the specified capabilities to the installable unit.
		 * 
		 * @param additional the capabilities to add.
		 */
		public void addProvidedCapabilities(Collection<IProvidedCapability> additional) {
			if (additional == null || additional.size() == 0)
				return;
			Collection<IProvidedCapability> current = unit().getProvidedCapabilities();
			ArrayList<IProvidedCapability> all = new ArrayList<>(additional.size() + current.size());
			all.addAll(current);
			all.addAll(additional);
			unit().setCapabilities(all.toArray(new IProvidedCapability[all.size()]));
		}

		/** @deprecated Use addRequirements(additional) instead */
		@Deprecated
		public void addRequiredCapabilities(Collection<IRequirement> additional) {
			addRequirements(additional);
		}

		/**
		 * Add the specified requirements to the installable unit.
		 * 
		 * @param additional the requirements to add
		 */
		public void addRequirements(Collection<IRequirement> additional) {
			if (additional == null || additional.size() == 0)
				return;
			List<IRequirement> current = unit().getRequirements();
			ArrayList<IRequirement> all = new ArrayList<>(additional.size() + current.size());
			all.addAll(current);
			all.addAll(additional);
			unit().setRequiredCapabilities(all.toArray(new IRequirement[all.size()]));
		}

		public void addTouchpointData(ITouchpointData data) {
			Assert.isNotNull(data);
			unit().addTouchpointData(data);
		}

		/**
		 * Returns the id of the installable unit.
		 */
		public String getId() {
			return unit().getId();
		}

		/**
		 * Return a collection of all the capabilities specified on this installable
		 * unit.
		 */
		public Collection<IProvidedCapability> getProvidedCapabilities() {
			return unit().getProvidedCapabilities();
		}

		/** @deprecated Use getRequirements() instead */
		@Deprecated
		public List<IRequirement> getRequiredCapabilities() {
			return getRequirements();
		}

		/**
		 * Return a collection of the requirements specified on this installable unit.
		 */
		public List<IRequirement> getRequirements() {
			return unit().getRequirements();
		}

		/** @deprecated Use getMetaRequirements() instead */
		@Deprecated
		public Collection<IRequirement> getMetaRequiredCapabilities() {
			return getMetaRequirements();
		}

		/**
		 * Return a collection of the meta requirements specified on this installable
		 * unit.
		 */
		public Collection<IRequirement> getMetaRequirements() {
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

		/**
		 * Return the versiono on this installable unit description.
		 */
		public Version getVersion() {
			return unit().getVersion();
		}

		/**
		 * Returns the UpdateDescriptor for this IU
		 * 
		 * @since 2.1
		 */
		public IUpdateDescriptor getUpdateDescriptor() {
			return unit().getUpdateDescriptor();
		}

		/**
		 * Set the artifact keys for the installable unit. Previous values will be
		 * overwritten.
		 * 
		 * @param value the artifacts to the used.
		 */
		public void setArtifacts(IArtifactKey[] value) {
			unit().setArtifacts(value);
		}

		/**
		 * Set the capabilities for the installable unit. Previous values will be
		 * overwritten.
		 * 
		 * @param exportedCapabilities the capabilities to be used.
		 */
		public void setCapabilities(IProvidedCapability[] exportedCapabilities) {
			unit().setCapabilities(exportedCapabilities);
		}

		/**
		 * Set the copyright for the installable unit. Previous values will be
		 * overwritten.
		 * 
		 * @param copyright the copyright to be used.
		 */
		public void setCopyright(ICopyright copyright) {
			unit().setCopyright(copyright);
		}

		public void setFilter(IMatchExpression<IInstallableUnit> filter) {
			unit().setFilter(filter);
		}

		public void setFilter(String filter) {
			unit().setFilter(filter);
		}

		/**
		 * Set the id of the installable unit.
		 */
		public void setId(String id) {
			unit().setId(id);
		}

		/**
		 * Set the licenses for the installable unit. Previous values will be
		 * overwritten.
		 */
		public void setLicenses(ILicense[] licenses) {
			unit().setLicenses(licenses);
		}

		/**
		 * Set a property with a specified value for this installable unit.
		 * 
		 * @param key   key with which the specified value is to be associated
		 * @param value value to be associated with the specified key
		 */
		public void setProperty(String key, String value) {
			unit().setProperty(key, value);
		}

		/** @deprecated Use setRequirements(requirements) instead */
		@Deprecated
		public void setRequiredCapabilities(IRequirement[] requirements) {
			setRequirements(requirements);
		}

		/**
		 * Set the requirements for the installable unit. Previous values will be
		 * overwritten.
		 * 
		 * @param requirements the requirements to be used.
		 */
		public void setRequirements(IRequirement[] requirements) {
			unit().setRequiredCapabilities(requirements);
		}

		/** @deprecated Use setMetaRequirements(requirements) instead */
		@Deprecated
		public void setMetaRequiredCapabilities(IRequirement[] metaRequirements) {
			setMetaRequirements(metaRequirements);
		}

		/**
		 * Set the meta requirements for the installable unit. Previous values will be
		 * overwritten.
		 * 
		 * @param metaRequirements the meta requirements to be used.
		 */
		public void setMetaRequirements(IRequirement[] metaRequirements) {
			unit().setMetaRequiredCapabilities(metaRequirements);
		}

		/**
		 * Change the singleton status of the installable unit.
		 */
		public void setSingleton(boolean singleton) {
			unit().setSingleton(singleton);
		}

		/**
		 * Set the touchpoint type for the installable unit.
		 */
		public void setTouchpointType(ITouchpointType type) {
			unit().setTouchpointType(type);
		}

		/**
		 * Set the update descriptor for the installable unit.
		 */
		public void setUpdateDescriptor(IUpdateDescriptor updateInfo) {
			unit().setUpdateDescriptor(updateInfo);
		}

		/**
		 * Set the version of this installable unit.
		 * 
		 * @param newVersion version to be set on the installable unit.
		 */
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

	/**
	 * A description containing information about an installable unit fragment. Once
	 * created, installable units are immutable. This description class allows a
	 * client to build up the state for an installable unit fragment incrementally,
	 * and then finally produce the resulting immutable unit.
	 */
	public static class InstallableUnitFragmentDescription extends InstallableUnitDescription {
		public InstallableUnitFragmentDescription() {
			super();
			setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		}

		/**
		 * Specify the requirements identifying the host to which the installable unit
		 * fragment should be attached to.
		 */
		public void setHost(IRequirement... hostRequirement) {
			((InstallableUnitFragment) unit()).setHost(Arrays.asList(hostRequirement));
		}

		@Override
		InstallableUnit unit() {
			if (unit == null)
				unit = new InstallableUnitFragment();
			return unit;
		}
	}

	/**
	 * A description containing information about an installable unit patch. Once
	 * created, installable units are immutable. This description class allows a
	 * client to build up the state for an installable unit patch incrementally, and
	 * then finally produce the resulting immutable unit.
	 */
	public static class InstallableUnitPatchDescription extends InstallableUnitDescription {

		public InstallableUnitPatchDescription() {
			super();
			setProperty(InstallableUnitDescription.PROP_TYPE_PATCH, Boolean.TRUE.toString());
		}

		/**
		 * Set the applicability scope for the installable unit patch.
		 */
		public void setApplicabilityScope(IRequirement[][] applyTo) {
			if (applyTo == null)
				throw new IllegalArgumentException("A patch scope can not be null"); //$NON-NLS-1$
			((InstallableUnitPatch) unit()).setApplicabilityScope(applyTo);
		}

		/**
		 * Set the lifecycle change for the installable unit patch.
		 */
		public void setLifeCycle(IRequirement lifeCycle) {
			((InstallableUnitPatch) unit()).setLifeCycle(lifeCycle);
		}

		/**
		 * Set the requirement change for the installable unit patch.
		 */
		public void setRequirementChanges(IRequirementChange[] changes) {
			((InstallableUnitPatch) unit()).setRequirementsChange(changes);
		}

		@Override
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
	private static final ITouchpointData EMPTY_TOUCHPOINT_DATA = new TouchpointData(Collections.emptyMap());

	private static ITouchpointType[] typeCache = new ITouchpointType[5];

	private static int typeCacheOffset;

	/**
	 * Returns an {@link IInstallableUnit} based on the given description. Once the
	 * installable unit has been created, the information is discarded from the
	 * description object.
	 * 
	 * @param description The description of the unit to create
	 * @return The created installable unit
	 */
	public static IInstallableUnit createInstallableUnit(InstallableUnitDescription description) {
		Assert.isNotNull(description);
		return description.unitCreate();
	}

	/**
	 * Returns an {@link IInstallableUnitFragment} based on the given description.
	 * Once the fragment has been created, the information is discarded from the
	 * description object.
	 * 
	 * @param description The description of the unit to create
	 * @return The created installable unit fragment
	 */
	public static IInstallableUnitFragment createInstallableUnitFragment(
			InstallableUnitFragmentDescription description) {
		Assert.isNotNull(description);
		return (IInstallableUnitFragment) description.unitCreate();
	}

	/**
	 * Returns an {@link IInstallableUnitPatch} based on the given description. Once
	 * the patch installable unit has been created, the information is discarded
	 * from the description object.
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
	 * @param name      The capability name
	 * @param version   The capability version
	 */
	public static IProvidedCapability createProvidedCapability(String namespace, String name, Version version) {
		return new ProvidedCapability(namespace, name, version);
	}

	/**
	 * Returns a {@link IProvidedCapability} with the given values.
	 * 
	 * @param namespace  The capability namespace
	 * @param properties The description of the capability
	 * @since 2.4
	 */
	public static IProvidedCapability createProvidedCapability(String namespace, Map<String, Object> properties) {
		return new ProvidedCapability(namespace, properties);
	}

	/**
	 * Create and return a new requirement ({@link IRequirement}) with the specified
	 * values.
	 * 
	 * @param namespace the namespace for the requirement. Must not be
	 *                  <code>null</code>.
	 * @param name      the name for the requirement. Must not be <code>null</code>.
	 * @param range     the version range. A value of <code>null</code> is
	 *                  equivalent to {@link VersionRange#emptyRange} and matches
	 *                  all versions.
	 * @param filter    The filter used to evaluate whether this capability is
	 *                  applicable in the current environment, or <code>null</code>
	 *                  to indicate this capability is always applicable
	 * @param optional  <code>true</code> if this requirement is optional, and
	 *                  <code>false</code> otherwise.
	 * @param multiple  <code>true</code> if this requirement can be satisfied by
	 *                  multiple provided capabilities, or <code>false</code> if it
	 *                  requires exactly one match
	 * @param greedy    <code>true</code> if the requirement should be considered
	 *                  greedy and <code>false</code> otherwise
	 * @return the requirement
	 */
	public static IRequirement createRequirement(String namespace, String name, VersionRange range, String filter,
			boolean optional, boolean multiple, boolean greedy) {
		return new RequiredCapability(namespace, name, range, InstallableUnit.parseFilter(filter), optional ? 0 : 1,
				multiple ? Integer.MAX_VALUE : 1, greedy, null);
	}

	/**
	 * Returns a {@link IRequirement} with the given values.
	 * 
	 * @param namespace The capability namespace
	 * @param name      The required capability name
	 * @param range     The range of versions that are required, or
	 *                  <code>null</code> to indicate that any version will do.
	 * @param filter    The filter used to evaluate whether this capability is
	 *                  applicable in the current environment, or <code>null</code>
	 *                  to indicate this capability is always applicable
	 * @param optional  <code>true</code> if this required capability is optional,
	 *                  and <code>false</code> otherwise.
	 * @param multiple  <code>true</code> if this capability can be satisfied by
	 *                  multiple provided capabilities, or <code>false</code> if it
	 *                  requires exactly one match
	 * @return the requirement
	 */
	public static IRequirement createRequirement(String namespace, String name, VersionRange range,
			IMatchExpression<IInstallableUnit> filter, boolean optional, boolean multiple) {
		return new RequiredCapability(namespace, name, range, filter, optional ? 0 : 1,
				multiple ? Integer.MAX_VALUE : 1, true, null);
	}

	/**
	 * Create and return a new requirement ({@link IRequirement}) with the specified
	 * values.
	 * 
	 * @param namespace the namespace for the requirement. Must not be
	 *                  <code>null</code>.
	 * @param name      the name for the requirement. Must not be <code>null</code>.
	 * @param range     the version range. A value of <code>null</code> is
	 *                  equivalent to {@link VersionRange#emptyRange} and matches
	 *                  all versions.
	 * @param filter    The filter used to evaluate whether this capability is
	 *                  applicable in the current environment, or <code>null</code>
	 *                  to indicate this capability is always applicable
	 * @param minCard   minimum cardinality
	 * @param maxCard   maximum cardinality
	 * @param greedy    <code>true</code> if the requirement should be considered
	 *                  greedy and <code>false</code> otherwise
	 * @return the requirement
	 */
	public static IRequirement createRequirement(String namespace, String name, VersionRange range,
			IMatchExpression<IInstallableUnit> filter, int minCard, int maxCard, boolean greedy) {
		return new RequiredCapability(namespace, name, range, filter, minCard, maxCard, greedy, null);
	}

	/**
	 * Create and return a new requirement ({@link IRequirement}) with the specified
	 * values.
	 * 
	 * @param namespace   the namespace for the requirement. Must not be
	 *                    <code>null</code>.
	 * @param name        the name for the requirement. Must not be
	 *                    <code>null</code>.
	 * @param range       the version range. A value of <code>null</code> is
	 *                    equivalent to {@link VersionRange#emptyRange} and matches
	 *                    all versions.
	 * @param filter      The filter used to evaluate whether this capability is
	 *                    applicable in the current environment, or
	 *                    <code>null</code> to indicate this capability is always
	 *                    applicable
	 * @param minCard     minimum cardinality
	 * @param maxCard     maximum cardinality
	 * @param greedy      <code>true</code> if the requirement should be considered
	 *                    greedy and <code>false</code> otherwise
	 * @param description a <code>String</code> description of the requirement, or
	 *                    <code>null</code>
	 * @return the requirement
	 */
	public static IRequirement createRequirement(String namespace, String name, VersionRange range,
			IMatchExpression<IInstallableUnit> filter, int minCard, int maxCard, boolean greedy, String description) {
		return new RequiredCapability(namespace, name, range, filter, minCard, maxCard, greedy, description);
	}

	/**
	 * 
	 * @param namespace   the namespace for the requirement. Must not be
	 *                    <code>null</code>.
	 * @param propsFilter filter applied on
	 *                    {@link IProvidedCapability#getProperties()} of every
	 *                    {@link IInstallableUnit#getProvidedCapabilities()}
	 * @param envFilter   matcher over {@link IInstallableUnit#getProperties()}
	 * @param minCard     minimum cardinality
	 * @param maxCard     maximum cardinality
	 * @param greedy      <code>true</code> if the requirement should be considered
	 *                    greedy and <code>false</code> otherwise
	 * @return the requirement
	 * @since 2.4
	 */
	public static IRequirement createRequirement(String namespace, String propsFilter,
			IMatchExpression<IInstallableUnit> envFilter, int minCard, int maxCard, boolean greedy) {
		IFilterExpression attrFilterExpr = ExpressionUtil.parseLDAP(propsFilter);
		return new RequiredPropertiesMatch(namespace, attrFilterExpr, envFilter, minCard, maxCard, greedy, null);
	}

	/**
	 * 
	 * @param namespace   the namespace for the requirement. Must not be
	 *                    <code>null</code>.
	 * @param propsFilter filter applied on
	 *                    {@link IProvidedCapability#getProperties()} of every
	 *                    {@link IInstallableUnit#getProvidedCapabilities()}
	 * @param envFilter   matcher over {@link IInstallableUnit#getProperties()}
	 * @param minCard     minimum cardinality
	 * @param maxCard     maximum cardinality
	 * @param greedy      <code>true</code> if the requirement should be considered
	 *                    greedy and <code>false</code> otherwise
	 * @return the requirement
	 * @since 2.5
	 */
	public static IRequirement createRequirement(String namespace, String propsFilter,
			IMatchExpression<IInstallableUnit> envFilter, int minCard, int maxCard, boolean greedy,
			String description) {
		IFilterExpression attrFilterExpr = ExpressionUtil.parseLDAP(propsFilter);
		return new RequiredPropertiesMatch(namespace, attrFilterExpr, envFilter, minCard, maxCard, greedy, description);
	}

	/**
	 * 
	 * @param namespace   the namespace for the requirement. Must not be
	 *                    <code>null</code>.
	 * @param propsFilter filter applied on
	 *                    {@link IProvidedCapability#getProperties()} of every
	 *                    {@link IInstallableUnit#getProvidedCapabilities()}
	 * @param envFilter   matcher over {@link IInstallableUnit#getProperties()}
	 * @param minCard     minimum cardinality
	 * @param maxCard     maximum cardinality
	 * @param greedy      <code>true</code> if the requirement should be considered
	 *                    greedy and <code>false</code> otherwise
	 * @param description a <code>String</code> description of the requirement, or
	 *                    <code>null</code>
	 * @return the requirement
	 * @since 2.4
	 */
	public static IRequirement createRequirement(String namespace, IFilterExpression propsFilter,
			IMatchExpression<IInstallableUnit> envFilter, int minCard, int maxCard, boolean greedy,
			String description) {
		return new RequiredPropertiesMatch(namespace, propsFilter, envFilter, minCard, maxCard, greedy, description);
	}

	/**
	 * Create and return a new requirement ({@link IRequirement}) with the specified
	 * values.
	 * 
	 * @param requirement the match expression
	 * @param envFilter   The filter used to evaluate whether this capability is
	 *                    applicable in the current environment, or
	 *                    <code>null</code> to indicate this capability is always
	 *                    applicable
	 * @param minCard     minimum cardinality
	 * @param maxCard     maximum cardinality
	 * @param greedy      <code>true</code> if the requirement should be considered
	 *                    greedy and <code>false</code> otherwise
	 * @return the requirement
	 */
	public static IRequirement createRequirement(IMatchExpression<IInstallableUnit> requirement,
			IMatchExpression<IInstallableUnit> envFilter, int minCard, int maxCard, boolean greedy) {
		return createRequirementInternal(requirement, envFilter, minCard, maxCard, greedy, null);
	}

	/**
	 * Create and return a new requirement ({@link IRequirement}) with the specified
	 * values.
	 * 
	 * @param requirement the match expression
	 * @param envFilter   the filter, or <code>null</code>
	 * @param minCard     minimum cardinality
	 * @param maxCard     maximum cardinality
	 * @param greedy      <code>true</code> if the requirement should be considered
	 *                    greedy and <code>false</code> otherwise
	 * @param description a <code>String</code> description of the requirement, or
	 *                    <code>null</code>
	 * @return the requirement
	 */
	public static IRequirement createRequirement(IMatchExpression<IInstallableUnit> requirement,
			IMatchExpression<IInstallableUnit> envFilter, int minCard, int maxCard, boolean greedy,
			String description) {
		return createRequirementInternal(requirement, envFilter, minCard, maxCard, greedy, description);
	}

	/**
	 * Returns a new requirement change.
	 * 
	 * @param applyOn  The source of the requirement change - the kind of
	 *                 requirement to apply the change to
	 * @param newValue The result of the requirement change - the requirement to
	 *                 replace the source requirement with
	 * @return a requirement change
	 */
	public static IRequirementChange createRequirementChange(IRequirement applyOn, IRequirement newValue) {
		if ((applyOn == null || applyOn instanceof IRequiredCapability)
				&& (newValue == null || newValue instanceof IRequiredCapability))
			return new RequirementChange((IRequiredCapability) applyOn, (IRequiredCapability) newValue);
		throw new IllegalArgumentException();
	}

	/**
	 * Returns a new {@link ICopyright}.
	 * 
	 * @param location the location of a document containing the copyright notice,
	 *                 or <code>null</code>
	 * @param body     the copyright body, cannot be <code>null</code>
	 * @throws IllegalArgumentException when the <code>body</code> is
	 *                                  <code>null</code>
	 */
	public static ICopyright createCopyright(URI location, String body) {
		return new Copyright(location, body);
	}

	/**
	 * Return a new {@link ILicense} The body should contain either the full text of
	 * the license or an summary for a license fully specified in the given
	 * location.
	 * 
	 * @param location the location of a document containing the full license, or
	 *                 <code>null</code>
	 * @param body     the license body, cannot be <code>null</code>
	 * @throws IllegalArgumentException when the <code>body</code> is
	 *                                  <code>null</code>
	 */
	public static ILicense createLicense(URI location, String body) {
		return new License(location, body, null);
	}

	/**
	 * Returns an {@link IInstallableUnit} that represents the given unit bound to
	 * the given fragments.
	 * 
	 * @see IInstallableUnit#isResolved()
	 * @param unit      The unit to be bound
	 * @param fragments The fragments to be bound
	 * @return A resolved installable unit
	 */
	public static IInstallableUnit createResolvedInstallableUnit(IInstallableUnit unit,
			IInstallableUnitFragment[] fragments) {
		if (unit.isResolved())
			return unit;
		Assert.isNotNull(unit);
		Assert.isNotNull(fragments);
		return new ResolvedInstallableUnit(unit, Arrays.asList(fragments));

	}

	/**
	 * Returns an instance of {@link ITouchpointData} with the given instructions.
	 * 
	 * @param instructions The instructions for the touchpoint data.
	 * @return The created touchpoint data
	 */
	public static ITouchpointData createTouchpointData(Map<String, ? extends Object> instructions) {
		Assert.isNotNull(instructions);
		// copy the map to protect against subsequent change by caller
		if (instructions.isEmpty())
			return EMPTY_TOUCHPOINT_DATA;

		Map<String, ITouchpointInstruction> result = new LinkedHashMap<>(instructions.size());

		for (Map.Entry<String, ? extends Object> entry : instructions.entrySet()) {
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
	 * 
	 * @param initial              - the initial ITouchpointData
	 * @param incomingInstructions - Map of ITouchpointInstructions to merge into
	 *                             the initial touchpoint data
	 * @return the merged ITouchpointData
	 */
	public static ITouchpointData mergeTouchpointData(ITouchpointData initial,
			Map<String, ITouchpointInstruction> incomingInstructions) {
		if (incomingInstructions == null || incomingInstructions.size() == 0)
			return initial;

		Map<String, ITouchpointInstruction> resultInstructions = new HashMap<>(initial.getInstructions());
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
	 * @param id      The touchpoint id
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

	/**
	 * 
	 * @param descriptors The IUs that the descriptor provides updates for.
	 * @param severity    The update severity (either
	 *                    {@link IUpdateDescriptor#NORMAL} or
	 *                    {@link IUpdateDescriptor#HIGH})
	 * @param description A description of the update
	 * @param location    a {@link URI} specifying the location or <code>null</code>
	 * @return A new update descriptor
	 */
	public static IUpdateDescriptor createUpdateDescriptor(Collection<IMatchExpression<IInstallableUnit>> descriptors,
			int severity, String description, URI location) {
		return new UpdateDescriptor(descriptors, severity, description, location);
	}

	/**
	 * Creates a new update descriptor object.
	 * 
	 * @param id          The id of the installable unit that the descriptor
	 *                    provides updates for
	 * @param range       The version range that the descriptor provides updates for
	 * @param severity    The update severity (either
	 *                    {@link IUpdateDescriptor#NORMAL} or
	 *                    {@link IUpdateDescriptor#HIGH})
	 * @param description A description of the update
	 * @return A new update descriptor
	 */
	public static IUpdateDescriptor createUpdateDescriptor(String id, VersionRange range, int severity,
			String description) {
		return createUpdateDescriptor(id, range, severity, description, null);
	}

	/**
	 * Create and return a new update descriptor {@link IUpdateDescriptor} with the
	 * specified values.
	 * 
	 * @param id          the identifiter for the update. Must not be
	 *                    <code>null</code>.
	 * @param range       the version range. A <code>null</code> range is equivalent
	 *                    to {@link VersionRange#emptyRange} and matches all
	 *                    versions.
	 * @param severity    the severity
	 * @param description a <code>String</code> description or <code>null</code>
	 * @param location    a {@link URI} specifying the location or <code>null</code>
	 * @return the update descriptor
	 */
	public static IUpdateDescriptor createUpdateDescriptor(String id, VersionRange range, int severity,
			String description, URI location) {
		Collection<IMatchExpression<IInstallableUnit>> descriptors = new ArrayList<>(1);
		descriptors.add(RequiredCapability.createMatchExpressionFromRange(IInstallableUnit.NAMESPACE_IU_ID, id, range));
		return createUpdateDescriptor(descriptors, severity, description, location);
	}

	private static IRequirement createRequirementInternal(IMatchExpression<IInstallableUnit> requirement,
			IMatchExpression<IInstallableUnit> envFilter, int minCard, int maxCard, boolean greedy,
			String description) {
		// IRequiredCapability is simply a requirement with a match expression derived
		// from a (namespace, name, version) tripet.
		// However the xml format also requires that maxCard > 1 or it is serialized in
		// the generic format.
		// When parsing back from xml try to convert to an IRequiredCapability to retain
		// the representation prior to serialization
		if (RequiredCapability.isVersionRangeRequirement(requirement)) {
			String namespace = RequiredCapability.extractNamespace(requirement);
			String name = RequiredCapability.extractName(requirement);
			VersionRange range = RequiredCapability.extractRange(requirement);
			return new RequiredCapability(namespace, name, range, envFilter, minCard, maxCard, greedy, description);
		}

		if (RequiredPropertiesMatch.isPropertiesMatchRequirement(requirement)) {
			String namespace = RequiredPropertiesMatch.extractNamespace(requirement);
			IFilterExpression attrMatch = RequiredPropertiesMatch.extractPropertiesMatch(requirement);
			return new RequiredPropertiesMatch(namespace, attrMatch, envFilter, minCard, maxCard, greedy, description);
		}

		return new Requirement(requirement, envFilter, minCard, maxCard, greedy, description);
	}

	private static ITouchpointType getCachedTouchpointType(String id, Version version) {
		for (ITouchpointType typeCache1 : typeCache) {
			if (typeCache1 != null && typeCache1.getId().equals(id) && typeCache1.getVersion().equals(version)) {
				return typeCache1;
			}
		}
		return null;
	}

	private static void putCachedTouchpointType(ITouchpointType result) {
		// simple rotating buffer
		typeCache[typeCacheOffset] = result;
		typeCacheOffset = (typeCacheOffset + 1) % typeCache.length;
	}
}
