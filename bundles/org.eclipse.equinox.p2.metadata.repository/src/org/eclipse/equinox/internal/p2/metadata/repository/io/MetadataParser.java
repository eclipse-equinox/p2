/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     Genuitec, LLC - added license support
 *		EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository.io;

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.persistence.XMLParser;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.*;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.osgi.framework.BundleContext;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

public abstract class MetadataParser extends XMLParser implements XMLConstants {
	static final ILicense[] NO_LICENSES = new ILicense[0];

	public MetadataParser(BundleContext context, String bundleId) {
		super(context, bundleId);
	}

	protected abstract class AbstractMetadataHandler extends AbstractHandler {

		public AbstractMetadataHandler(ContentHandler parentHandler, String elementHandled) {
			super(parentHandler, elementHandled);
		}

		int getOptionalSize(Attributes attributes, int dflt) {
			String sizeStr = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			return sizeStr != null ? Integer.parseInt(sizeStr) : dflt;
		}
	}

	protected class RepositoryReferencesHandler extends AbstractMetadataHandler {
		private HashSet<IRepositoryReference> references;

		public RepositoryReferencesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, REPOSITORY_REFERENCES_ELEMENT);
			references = new HashSet<>(getOptionalSize(attributes, 4));
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(REPOSITORY_REFERENCE_ELEMENT)) {
				new RepositoryReferenceHandler(this, attributes, references);
			} else {
				invalidElement(name, attributes);
			}
		}

		public IRepositoryReference[] getReferences() {
			return references.toArray(new IRepositoryReference[references.size()]);
		}
	}

	protected class RepositoryReferenceHandler extends AbstractHandler {

		private final String[] required = new String[] {TYPE_ATTRIBUTE, OPTIONS_ATTRIBUTE};

		public RepositoryReferenceHandler(AbstractHandler parentHandler, Attributes attributes, Set<IRepositoryReference> references) {
			super(parentHandler, REPOSITORY_REFERENCE_ELEMENT);
			String[] values = parseRequiredAttributes(attributes, required);
			String name = parseOptionalAttribute(attributes, NAME_ATTRIBUTE);
			int type = checkInteger(elementHandled, TYPE_ATTRIBUTE, values[0]);
			int options = checkInteger(elementHandled, OPTIONS_ATTRIBUTE, values[1]);
			URI location = parseURIAttribute(attributes, true);
			if (location != null)
				references.add(new RepositoryReference(location, name, type, options));
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}
	}

	protected class InstallableUnitsHandler extends AbstractMetadataHandler {
		private ArrayList<InstallableUnitDescription> units;

		public InstallableUnitsHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, INSTALLABLE_UNITS_ELEMENT);
			units = new ArrayList<>(getOptionalSize(attributes, 4));
		}

		public IInstallableUnit[] getUnits() {
			int size = units.size();
			IInstallableUnit[] result = new IInstallableUnit[size];
			int i = 0;
			for (InstallableUnitDescription desc : units)
				result[i++] = MetadataFactory.createInstallableUnit(desc);
			return result;
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(INSTALLABLE_UNIT_ELEMENT)) {
				new InstallableUnitHandler(this, attributes, units);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class InstallableUnitHandler extends AbstractHandler {

		InstallableUnitDescription currentUnit = null;

		private PropertiesHandler propertiesHandler = null;
		private ProvidedCapabilitiesHandler providedCapabilitiesHandler = null;
		private RequirementsHandler requiredCapabilitiesHandler = null;
		private HostRequiredCapabilitiesHandler hostRequiredCapabilitiesHandler = null;
		private MetaRequiredCapabilitiesHandler metaRequiredCapabilitiesHandler = null;
		private TextHandler filterHandler = null;
		private ArtifactsHandler artifactsHandler = null;
		private TouchpointTypeHandler touchpointTypeHandler = null;
		private TouchpointDataHandler touchpointDataHandler = null;
		private UpdateDescriptorHandler updateDescriptorHandler = null;
		private LicensesHandler licensesHandler = null;
		private CopyrightHandler copyrightHandler = null;
		private RequirementsChangeHandler requirementChangesHandler = null;
		private ApplicabilityScopesHandler applicabilityScopeHandler = null;
		private LifeCycleHandler lifeCycleHandler;

		private String id;
		private Version version;
		private boolean singleton;

		private List<InstallableUnitDescription> units;

		public InstallableUnitHandler(AbstractHandler parentHandler, Attributes attributes, List<InstallableUnitDescription> units) {
			super(parentHandler, INSTALLABLE_UNIT_ELEMENT);
			String[] values = parseAttributes(attributes, REQUIRED_IU_ATTRIBUTES, OPTIONAL_IU_ATTRIBUTES);
			this.units = units;
			//skip entire IU if the id is missing
			if (values[0] == null)
				return;

			id = values[0];
			version = checkVersion(INSTALLABLE_UNIT_ELEMENT, VERSION_ATTRIBUTE, values[1]);
			singleton = checkBoolean(INSTALLABLE_UNIT_ELEMENT, SINGLETON_ATTRIBUTE, values[2], true).booleanValue();
		}

		public IInstallableUnit getInstallableUnit() {
			return MetadataFactory.createInstallableUnit(currentUnit);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			checkCancel();
			if (PROPERTIES_ELEMENT.equals(name)) {
				if (propertiesHandler == null) {
					propertiesHandler = new PropertiesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (PROVIDED_CAPABILITIES_ELEMENT.equals(name)) {
				if (providedCapabilitiesHandler == null) {
					providedCapabilitiesHandler = new ProvidedCapabilitiesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (REQUIREMENTS_ELEMENT.equals(name)) {
				if (requiredCapabilitiesHandler == null) {
					requiredCapabilitiesHandler = new RequirementsHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (HOST_REQUIREMENTS_ELEMENT.equals(name)) {
				if (hostRequiredCapabilitiesHandler == null) {
					hostRequiredCapabilitiesHandler = new HostRequiredCapabilitiesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (META_REQUIREMENTS_ELEMENT.equals(name)) {
				if (metaRequiredCapabilitiesHandler == null) {
					metaRequiredCapabilitiesHandler = new MetaRequiredCapabilitiesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (IU_FILTER_ELEMENT.equals(name)) {
				if (filterHandler == null) {
					filterHandler = new TextHandler(this, IU_FILTER_ELEMENT, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (ARTIFACT_KEYS_ELEMENT.equals(name)) {
				if (artifactsHandler == null) {
					artifactsHandler = new ArtifactsHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (TOUCHPOINT_TYPE_ELEMENT.equals(name)) {
				if (touchpointTypeHandler == null) {
					touchpointTypeHandler = new TouchpointTypeHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (TOUCHPOINT_DATA_ELEMENT.equals(name)) {
				if (touchpointDataHandler == null) {
					touchpointDataHandler = new TouchpointDataHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (UPDATE_DESCRIPTOR_ELEMENT.equals(name)) {
				if (updateDescriptorHandler == null)
					updateDescriptorHandler = new UpdateDescriptorHandler(this, attributes);
				else {
					duplicateElement(this, name, attributes);
				}
			} else if (LICENSES_ELEMENT.equals(name)) {
				if (licensesHandler == null) {
					licensesHandler = new LicensesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (REQUIREMENT_CHANGES.equals(name)) {
				if (requirementChangesHandler == null) {
					requirementChangesHandler = new RequirementsChangeHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (APPLICABILITY_SCOPE.equals(name)) {
				if (applicabilityScopeHandler == null) {
					applicabilityScopeHandler = new ApplicabilityScopesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (LIFECYCLE.equals(name)) {
				if (lifeCycleHandler == null) {
					lifeCycleHandler = new LifeCycleHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (COPYRIGHT_ELEMENT.equals(name)) {
				if (copyrightHandler == null) {
					copyrightHandler = new CopyrightHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else {
				invalidElement(name, attributes);
			}
		}

		@Override
		protected void finished() {
			if (isValidXML()) {
				if (requirementChangesHandler != null) {
					currentUnit = new MetadataFactory.InstallableUnitPatchDescription();
					((InstallableUnitPatchDescription) currentUnit).setRequirementChanges(requirementChangesHandler.getRequirementChanges().toArray(new IRequirementChange[requirementChangesHandler.getRequirementChanges().size()]));
					if (applicabilityScopeHandler != null)
						((InstallableUnitPatchDescription) currentUnit).setApplicabilityScope(applicabilityScopeHandler.getScope());
					if (lifeCycleHandler != null)
						((InstallableUnitPatchDescription) currentUnit).setLifeCycle(lifeCycleHandler.getLifeCycleRequirement());
				} else if (hostRequiredCapabilitiesHandler == null || hostRequiredCapabilitiesHandler.getHostRequiredCapabilities().length == 0) {
					currentUnit = new InstallableUnitDescription();
				} else {
					currentUnit = new MetadataFactory.InstallableUnitFragmentDescription();
					((InstallableUnitFragmentDescription) currentUnit).setHost(hostRequiredCapabilitiesHandler.getHostRequiredCapabilities());
				}
				currentUnit.setId(id);
				currentUnit.setVersion(version);
				currentUnit.setSingleton(singleton);
				OrderedProperties properties = (propertiesHandler == null ? new OrderedProperties(0) : propertiesHandler.getProperties());
				String updateFrom = null;
				VersionRange updateRange = null;
				for (Entry<String, String> e : properties.entrySet()) {
					String key = e.getKey();
					String value = e.getValue();
					//Backward compatibility
					if (key.equals("equinox.p2.update.from")) { //$NON-NLS-1$
						updateFrom = value;
						continue;
					}
					if (key.equals("equinox.p2.update.range")) { //$NON-NLS-1$
						updateRange = VersionRange.create(value);
						continue;
					}
					//End of backward compatibility
					currentUnit.setProperty(key, value);
				}
				//Backward compatibility
				if (updateFrom != null && updateRange != null)
					currentUnit.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(updateFrom, updateRange, IUpdateDescriptor.NORMAL, null));
				//End of backward compatibility

				if (licensesHandler != null) {
					currentUnit.setLicenses(licensesHandler.getLicenses());
				}

				if (copyrightHandler != null) {
					ICopyright copyright = copyrightHandler.getCopyright();
					currentUnit.setCopyright(copyright);
				}

				IProvidedCapability[] providedCapabilities = (providedCapabilitiesHandler == null ? new IProvidedCapability[0] : providedCapabilitiesHandler.getProvidedCapabilities());
				currentUnit.setCapabilities(providedCapabilities);
				IRequirement[] requiredCapabilities = (requiredCapabilitiesHandler == null ? new IRequirement[0] : requiredCapabilitiesHandler.getRequirements());
				currentUnit.setRequirements(requiredCapabilities);
				IRequirement[] metaRequiredCapabilities = (metaRequiredCapabilitiesHandler == null ? new IRequirement[0] : metaRequiredCapabilitiesHandler.getMetaRequiredCapabilities());
				currentUnit.setMetaRequirements(metaRequiredCapabilities);
				if (filterHandler != null) {
					currentUnit.setFilter(filterHandler.getText());
				}
				IArtifactKey[] artifacts = (artifactsHandler == null ? new IArtifactKey[0] : artifactsHandler.getArtifactKeys());
				currentUnit.setArtifacts(artifacts);
				if (touchpointTypeHandler != null) {
					currentUnit.setTouchpointType(touchpointTypeHandler.getTouchpointType());
				} else {
					// TODO: create an error
				}
				ITouchpointData[] touchpointData = (touchpointDataHandler == null ? new ITouchpointData[0] : touchpointDataHandler.getTouchpointData());
				for (int i = 0; i < touchpointData.length; i++)
					currentUnit.addTouchpointData(touchpointData[i]);
				if (updateDescriptorHandler != null)
					currentUnit.setUpdateDescriptor(updateDescriptorHandler.getUpdateDescriptor());
				units.add(currentUnit);
			}
		}
	}

	protected class ApplicabilityScopesHandler extends AbstractMetadataHandler {
		private List<IRequirement[]> scopes;

		public ApplicabilityScopesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, APPLICABILITY_SCOPE);
			scopes = new ArrayList<>(getOptionalSize(attributes, 4));
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (APPLY_ON.equals(name)) {
				new ApplicabilityScopeHandler(this, attributes, scopes);
			} else {
				duplicateElement(this, name, attributes);
			}
		}

		public IRequirement[][] getScope() {
			return scopes.toArray(new IRequirement[scopes.size()][]);
		}
	}

	protected class ApplicabilityScopeHandler extends AbstractHandler {
		private RequirementsHandler children;
		private List<IRequirement[]> scopes;

		public ApplicabilityScopeHandler(AbstractHandler parentHandler, Attributes attributes, List<IRequirement[]> scopes) {
			super(parentHandler, APPLY_ON);
			this.scopes = scopes;
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (REQUIREMENTS_ELEMENT.equals(name)) {
				children = new RequirementsHandler(this, attributes);
			} else {
				duplicateElement(this, name, attributes);
			}
		}

		@Override
		protected void finished() {
			if (children != null) {
				scopes.add(children.getRequirements());
			}
		}
	}

	protected class RequirementsChangeHandler extends AbstractMetadataHandler {
		private List<IRequirementChange> requirementChanges;

		public RequirementsChangeHandler(InstallableUnitHandler parentHandler, Attributes attributes) {
			super(parentHandler, REQUIREMENT_CHANGES);
			requirementChanges = new ArrayList<>(getOptionalSize(attributes, 4));
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(REQUIREMENT_CHANGE)) {
				new RequirementChangeHandler(this, attributes, requirementChanges);
			} else {
				invalidElement(name, attributes);
			}
		}

		public List<IRequirementChange> getRequirementChanges() {
			return requirementChanges;
		}
	}

	protected class RequirementChangeHandler extends AbstractHandler {
		private List<IRequirement> from;
		private List<IRequirement> to;
		private List<IRequirementChange> requirementChanges;

		public RequirementChangeHandler(AbstractHandler parentHandler, Attributes attributes, List<IRequirementChange> requirementChanges) {
			super(parentHandler, REQUIREMENT_CHANGE);
			from = new ArrayList<>(1);
			to = new ArrayList<>(1);
			this.requirementChanges = requirementChanges;
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(REQUIREMENT_FROM)) {
				new RequirementChangeEltHandler(this, REQUIREMENT_FROM, attributes, from);
				return;
			}

			if (name.equals(REQUIREMENT_TO)) {
				new RequirementChangeEltHandler(this, REQUIREMENT_TO, attributes, to);
				return;
			}
			invalidElement(name, attributes);
		}

		@Override
		protected void finished() {
			requirementChanges.add(MetadataFactory.createRequirementChange(from.size() == 0 ? null : (IRequirement) from.get(0), to.size() == 0 ? null : (IRequirement) to.get(0)));
		}
	}

	protected class RequirementChangeEltHandler extends AbstractHandler {
		private List<IRequirement> requirement;

		public RequirementChangeEltHandler(AbstractHandler parentHandler, String parentId, Attributes attributes, List<IRequirement> from) {
			super(parentHandler, parentId);
			requirement = from;
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (REQUIREMENT_ELEMENT.equals(name))
				new RequirementHandler(this, attributes, requirement);
			else {
				invalidElement(name, attributes);
			}
		}

	}

	protected class LifeCycleHandler extends AbstractHandler {
		private List<IRequirement> lifeCycleRequirement;

		public LifeCycleHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, LIFECYCLE);
			lifeCycleRequirement = new ArrayList<>(1);
		}

		public IRequirement getLifeCycleRequirement() {
			if (lifeCycleRequirement.size() == 0)
				return null;
			return lifeCycleRequirement.get(0);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (REQUIREMENT_ELEMENT.equals(name)) {
				new RequirementHandler(this, attributes, lifeCycleRequirement);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class ProvidedCapabilitiesHandler extends AbstractMetadataHandler {
		private List<IProvidedCapability> providedCapabilities;

		public ProvidedCapabilitiesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, PROVIDED_CAPABILITIES_ELEMENT);
			providedCapabilities = new ArrayList<>(getOptionalSize(attributes, 4));
		}

		public IProvidedCapability[] getProvidedCapabilities() {
			return providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(PROVIDED_CAPABILITY_ELEMENT)) {
				new ProvidedCapabilityHandler(this, attributes, providedCapabilities);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class ProvidedCapabilityHandler extends AbstractHandler {
		private String namespace;
		private String name;
		private Version version;
		private ProvidedCapabilityPropertiesHandler propertiesHandler;

		private List<IProvidedCapability> capabilities;

		public ProvidedCapabilityHandler(AbstractHandler parentHandler, Attributes attributes, List<IProvidedCapability> capabilities) {
			super(parentHandler, PROVIDED_CAPABILITY_ELEMENT);

			this.capabilities = capabilities;

			String[] values = parseRequiredAttributes(attributes, REQUIRED_PROVIDED_CAPABILITY_ATTRIBUTES);
			this.namespace = values[0];
			this.name = values[1];
			this.version = checkVersion(PROVIDED_CAPABILITY_ELEMENT, VERSION_ATTRIBUTE, values[2]);
		}

		@Override
		public void startElement(String elem, Attributes attributes) {
			switch (elem) {
				case PROPERTIES_ELEMENT :
					this.propertiesHandler = new ProvidedCapabilityPropertiesHandler(this, attributes);
					break;
				default :
					invalidElement(elem, attributes);
					break;
			}
		}

		@Override
		protected void finished() {
			Map<String, Object> properties = (propertiesHandler != null)
					? propertiesHandler.getProperties()
					: new HashMap<>();

			properties.put(namespace, name);
			properties.put(IProvidedCapability.PROPERTY_VERSION, version);
			IProvidedCapability cap = MetadataFactory.createProvidedCapability(namespace, properties);
			capabilities.add(cap);
		}
	}

	protected class ProvidedCapabilityPropertiesHandler extends AbstractMetadataHandler {
		private Map<String, Object> properties;

		public ProvidedCapabilityPropertiesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, PROPERTIES_ELEMENT);
			this.properties = new HashMap<>(getOptionalSize(attributes, 2));
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		@Override
		public void startElement(String elem, Attributes attributes) {
			switch (elem) {
				case PROPERTY_ELEMENT :
					new ProvidedCapabilityPropertyHandler(this, attributes, properties);
					break;
				default :
					invalidElement(elem, attributes);
					break;
			}
		}
	}

	protected class ProvidedCapabilityPropertyHandler extends AbstractMetadataHandler {
		public ProvidedCapabilityPropertyHandler(AbstractHandler parentHandler, Attributes attributes, Map<String, Object> properties) {
			super(parentHandler, PROPERTY_ELEMENT);

			String[] values = parseAttributes(attributes, PROPERTY_ATTRIBUTES, PROPERTY_OPTIONAL_ATTRIBUTES);

			String name = values[0];
			String value = values[1];
			String type = values[2] == null ? PROPERTY_TYPE_STRING : values[2];

			if (type.startsWith(PROPERTY_TYPE_LIST)) {
				properties.put(name, parseList(type, value));
			} else {
				properties.put(name, parseScalar(type, value));
			}
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}

		private List<Object> parseList(String type, String value) {
			final String elType;
			if (type.length() > PROPERTY_TYPE_LIST.length()) {
				// Strip the leading "List<" and trailing ">"
				elType = type.substring(PROPERTY_TYPE_LIST.length() + 1, type.length() - 1);
			} else {
				elType = PROPERTY_TYPE_STRING;
			}

			return Arrays.stream(value.split("\\s*,\\s*")) //$NON-NLS-1$
					.map(val -> parseScalar(elType, val))
					.collect(toList());
		}

		private Object parseScalar(String type, String value) {
			if (PROPERTY_TYPE_STRING.equals(type)) {
				return value;
			}
			if (PROPERTY_TYPE_INTEGER.equals(type)) {
				return Integer.parseInt(value);
			}
			if (PROPERTY_TYPE_LONG.equals(type)) {
				return Long.parseLong(value);
			}
			if (PROPERTY_TYPE_FLOAT.equals(type)) {
				return Float.parseFloat(value);
			}
			if (PROPERTY_TYPE_DOUBLE.equals(type)) {
				return Double.parseDouble(value);
			}
			if (PROPERTY_TYPE_BYTE.equals(type)) {
				return Byte.parseByte(value);
			}
			if (PROPERTY_TYPE_SHORT.equals(type)) {
				return Short.parseShort(value);
			}
			if (PROPERTY_TYPE_CHARACTER.equals(type)) {
				return value.charAt(0);
			}
			if (PROPERTY_TYPE_BOOLEAN.equals(type)) {
				return Boolean.parseBoolean(value);
			}
			if (PROPERTY_TYPE_VERSION.equals(type)) {
				return Version.create(value);
			}

			// String is the default
			return value;
		}
	}

	protected class HostRequiredCapabilitiesHandler extends AbstractMetadataHandler {
		private List<IRequirement> requiredCapabilities;

		public HostRequiredCapabilitiesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, HOST_REQUIREMENTS_ELEMENT);
			requiredCapabilities = new ArrayList<>(getOptionalSize(attributes, 4));
		}

		public IRequirement[] getHostRequiredCapabilities() {
			return requiredCapabilities.toArray(new IRequirement[requiredCapabilities.size()]);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(REQUIREMENT_ELEMENT)) {
				new RequirementHandler(this, attributes, requiredCapabilities);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class MetaRequiredCapabilitiesHandler extends AbstractMetadataHandler {
		private List<IRequirement> requiredCapabilities;

		public MetaRequiredCapabilitiesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, META_REQUIREMENTS_ELEMENT);
			requiredCapabilities = new ArrayList<>(getOptionalSize(attributes, 4));
		}

		public IRequirement[] getMetaRequiredCapabilities() {
			return requiredCapabilities.toArray(new IRequirement[requiredCapabilities.size()]);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(REQUIREMENT_ELEMENT)) {
				new RequirementHandler(this, attributes, requiredCapabilities);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class RequirementsHandler extends AbstractMetadataHandler {
		private List<IRequirement> requirements;

		public RequirementsHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, REQUIREMENTS_ELEMENT);
			requirements = new ArrayList<>(getOptionalSize(attributes, 4));
		}

		public IRequirement[] getRequirements() {
			return requirements.toArray(new IRequirement[requirements.size()]);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			switch (name) {
				case REQUIREMENT_ELEMENT :
					new RequirementHandler(this, attributes, requirements);
					break;
				case REQUIREMENT_PROPERTIES_ELEMENT :
					new RequirementPropertiesHandler(this, attributes, requirements);
					break;
				default :
					invalidElement(name, attributes);
					break;
			}
		}
	}

	protected class RequirementHandler extends AbstractHandler {
		private List<IRequirement> capabilities;

		// Expression based requirement
		private String match;
		private String matchParams;

		// Simple requirement
		private String namespace;
		private String name;
		private VersionRange range;

		private int min;
		private int max;
		private boolean greedy;

		private TextHandler filterHandler = null;
		private TextHandler descriptionHandler = null;

		public RequirementHandler(AbstractHandler parentHandler, Attributes attributes, List<IRequirement> capabilities) {
			super(parentHandler, REQUIREMENT_ELEMENT);
			this.capabilities = capabilities;

			// Version range requirement
			if (attributes.getIndex(NAMESPACE_ATTRIBUTE) >= 0) {
				String[] values = parseAttributes(attributes, REQIURED_CAPABILITY_ATTRIBUTES, REQUIRED_CAPABILITY_OPTIONAL_ATTRIBUTES);
				namespace = values[0];
				name = values[1];
				range = checkVersionRange(REQUIREMENT_ELEMENT, VERSION_RANGE_ATTRIBUTE, values[2]);
				boolean isOptional = checkBoolean(REQUIREMENT_ELEMENT, REQUIRED_CAPABILITY_OPTIONAL_ATTRIBUTE, values[3], false).booleanValue();
				min = isOptional ? 0 : 1;
				boolean isMultiple = checkBoolean(REQUIREMENT_ELEMENT, REQUIRED_CAPABILITY_MULTIPLE_ATTRIBUTE, values[4], false).booleanValue();
				max = isMultiple ? Integer.MAX_VALUE : 1;
				greedy = checkBoolean(REQUIREMENT_ELEMENT, REQUIREMENT_GREED_ATTRIBUTE, values[5], true).booleanValue();
			}
			// IU match expression requirement
			else {
				String[] values = parseAttributes(attributes, REQUIRED_IU_MATCH_ATTRIBUTES, REQUIRED_IU_MATCH_OPTIONAL_ATTRIBUTES);
				match = values[0];
				matchParams = values[1];
				min = values[2] == null ? 1 : checkInteger(REQUIREMENT_ELEMENT, MIN_ATTRIBUTE, values[2]);
				max = values[3] == null ? 1 : checkInteger(REQUIREMENT_ELEMENT, MAX_ATTRIBUTE, values[3]);
				greedy = checkBoolean(REQUIREMENT_ELEMENT, REQUIREMENT_GREED_ATTRIBUTE, values[4], true).booleanValue();
			}
		}

		@Override
		public void startElement(String elem, Attributes attributes) {
			if (elem.equals(REQUIREMENT_FILTER_ELEMENT)) {
				filterHandler = new TextHandler(this, REQUIREMENT_FILTER_ELEMENT, attributes);
			} else if (elem.equals(REQUIREMENT_DESCRIPTION_ELEMENT)) {
				descriptionHandler = new TextHandler(this, REQUIREMENT_DESCRIPTION_ELEMENT, attributes);
			} else {
				invalidElement(elem, attributes);
			}
		}

		@Override
		protected void finished() {
			if (!isValidXML())
				return;
			IMatchExpression<IInstallableUnit> filter = null;
			if (filterHandler != null) {
				try {
					filter = InstallableUnit.parseFilter(filterHandler.getText());
				} catch (ExpressionParseException e) {
					if (removeWhiteSpace(filterHandler.getText()).equals("(&(|)(|)(|))")) {//$NON-NLS-1$
						// We could log this I guess
					} else {
						throw e;
					}
				}
			}
			String description = descriptionHandler == null ? null : descriptionHandler.getText();
			IRequirement requirement;
			if (match != null) {
				IMatchExpression<IInstallableUnit> matchExpr = createMatchExpression(match, matchParams);
				requirement = MetadataFactory.createRequirement(matchExpr, filter, min, max, greedy, description);
			} else {
				requirement = MetadataFactory.createRequirement(namespace, name, range, filter, min, max, greedy, description);
			}
			capabilities.add(requirement);
		}

		private String removeWhiteSpace(String s) {
			if (s == null)
				return ""; //$NON-NLS-1$
			StringBuffer builder = new StringBuffer();
			for (int i = 0; i < s.length(); i++) {
				if (s.charAt(i) != ' ')
					builder.append(s.charAt(i));
			}
			return builder.toString();
		}
	}

	protected class RequirementPropertiesHandler extends AbstractHandler {
		private List<IRequirement> requirements;

		private String namespace;
		private String match;
		private int min;
		private int max;
		private boolean greedy;

		private TextHandler filterHandler;
		private TextHandler descriptionHandler;

		public RequirementPropertiesHandler(AbstractHandler parentHandler, Attributes attributes, List<IRequirement> requirements) {
			super(parentHandler, REQUIREMENT_PROPERTIES_ELEMENT);
			this.requirements = requirements;

			String[] values = parseAttributes(attributes, REQIURED_PROPERTIES_MATCH_ATTRIBUTES, REQIURED_PROPERTIES_MATCH_OPTIONAL_ATTRIBUTES);
			namespace = values[0];
			match = values[1];
			min = (values[2] == null) ? 1 : checkInteger(REQUIREMENT_PROPERTIES_ELEMENT, MIN_ATTRIBUTE, values[2]);
			max = (values[3] == null) ? 1 : checkInteger(REQUIREMENT_PROPERTIES_ELEMENT, MAX_ATTRIBUTE, values[3]);
			greedy = checkBoolean(REQUIREMENT_PROPERTIES_ELEMENT, REQUIREMENT_GREED_ATTRIBUTE, values[4], true).booleanValue();
		}

		@Override
		public void startElement(String elem, Attributes attributes) {
			switch (elem) {
				case REQUIREMENT_FILTER_ELEMENT :
					filterHandler = new TextHandler(this, REQUIREMENT_FILTER_ELEMENT, attributes);
					break;
				case REQUIREMENT_DESCRIPTION_ELEMENT :
					descriptionHandler = new TextHandler(this, REQUIREMENT_DESCRIPTION_ELEMENT, attributes);
					break;
				default :
					invalidElement(elem, attributes);
					break;
			}
		}

		@Override
		protected void finished() {
			if (!isValidXML()) {
				return;
			}

			IMatchExpression<IInstallableUnit> filter = null;
			if (filterHandler != null) {
				try {
					filter = InstallableUnit.parseFilter(filterHandler.getText());
				} catch (ExpressionParseException e) {
					if (removeWhiteSpace(filterHandler.getText()).equals("(&(|)(|)(|))")) {//$NON-NLS-1$
						// We could log this I guess
					} else {
						throw e;
					}
				}
			}

			String description = (descriptionHandler != null) ? descriptionHandler.getText() : null;

			IFilterExpression attrMatch = ExpressionUtil.parseLDAP(match);
			IRequirement requirement = MetadataFactory.createRequirement(namespace, attrMatch, filter, min, max, greedy, description);
			requirements.add(requirement);
		}

		private String removeWhiteSpace(String s) {
			if (s == null)
				return ""; //$NON-NLS-1$
			StringBuffer builder = new StringBuffer();
			for (int i = 0; i < s.length(); i++) {
				if (s.charAt(i) != ' ')
					builder.append(s.charAt(i));
			}
			return builder.toString();
		}
	}

	protected class ArtifactsHandler extends AbstractHandler {

		private List<IArtifactKey> artifacts;

		public ArtifactsHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, ARTIFACT_KEYS_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			artifacts = (size != null ? new ArrayList<>(Integer.parseInt(size)) : new ArrayList<>(4));
		}

		public IArtifactKey[] getArtifactKeys() {
			return artifacts.toArray(new IArtifactKey[artifacts.size()]);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(ARTIFACT_KEY_ELEMENT)) {
				new ArtifactHandler(this, attributes, artifacts);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class ArtifactHandler extends AbstractHandler {

		private final String[] required = new String[] {CLASSIFIER_ATTRIBUTE, ID_ATTRIBUTE, VERSION_ATTRIBUTE};

		public ArtifactHandler(AbstractHandler parentHandler, Attributes attributes, List<IArtifactKey> artifacts) {
			super(parentHandler, ARTIFACT_KEY_ELEMENT);
			String[] values = parseRequiredAttributes(attributes, required);
			Version version = checkVersion(ARTIFACT_KEY_ELEMENT, VERSION_ATTRIBUTE, values[2]);
			artifacts.add(new ArtifactKey(values[0], values[1], version));
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}
	}

	protected class TouchpointTypeHandler extends AbstractHandler {

		private final String[] required = new String[] {ID_ATTRIBUTE, VERSION_ATTRIBUTE};

		ITouchpointType touchpointType = null;

		public TouchpointTypeHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, TOUCHPOINT_TYPE_ELEMENT);
			String[] values = parseRequiredAttributes(attributes, required);
			Version version = checkVersion(TOUCHPOINT_TYPE_ELEMENT, VERSION_ATTRIBUTE, values[1]);
			touchpointType = MetadataFactory.createTouchpointType(values[0], version);
		}

		public ITouchpointType getTouchpointType() {
			return touchpointType;
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}
	}

	protected class TouchpointDataHandler extends AbstractHandler {

		ITouchpointData touchpointData = null;

		List<TouchpointInstructionsHandler> data = null;

		public TouchpointDataHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, TOUCHPOINT_DATA_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			data = (size != null ? new ArrayList<>(Integer.parseInt(size)) : new ArrayList<>(4));
		}

		public ITouchpointData[] getTouchpointData() {
			ITouchpointData[] result = new ITouchpointData[data.size()];
			for (int i = 0; i < result.length; i++)
				result[i] = data.get(i).getTouchpointData();
			return result;
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT)) {
				data.add(new TouchpointInstructionsHandler(this, attributes, data));
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class TouchpointInstructionsHandler extends AbstractHandler {

		Map<String, ITouchpointInstruction> instructions = null;

		public TouchpointInstructionsHandler(AbstractHandler parentHandler, Attributes attributes, List<TouchpointInstructionsHandler> data) {
			super(parentHandler, TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			instructions = (size != null ? new LinkedHashMap<>(Integer.parseInt(size)) : new LinkedHashMap<>(4));
		}

		public ITouchpointData getTouchpointData() {
			return MetadataFactory.createTouchpointData(instructions);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(TOUCHPOINT_DATA_INSTRUCTION_ELEMENT)) {
				new TouchpointInstructionHandler(this, attributes, instructions);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class TouchpointInstructionHandler extends TextHandler {

		private final String[] required = new String[] {TOUCHPOINT_DATA_INSTRUCTION_KEY_ATTRIBUTE};
		private final String[] optional = new String[] {TOUCHPOINT_DATA_INSTRUCTION_IMPORT_ATTRIBUTE};

		Map<String, ITouchpointInstruction> instructions = null;
		String key = null;
		String qualifier = null;

		public TouchpointInstructionHandler(AbstractHandler parentHandler, Attributes attributes, Map<String, ITouchpointInstruction> instructions) {
			super(parentHandler, TOUCHPOINT_DATA_INSTRUCTION_ELEMENT);
			String[] values = parseAttributes(attributes, required, optional);
			key = values[0];
			qualifier = values[1];
			this.instructions = instructions;
		}

		@Override
		protected void finished() {
			if (isValidXML()) {
				if (key != null) {
					instructions.put(key, MetadataFactory.createTouchpointInstruction(getText(), qualifier));
				}
			}
		}
	}

	protected class UpdateDescriptorHandler extends TextHandler {
		private final String[] requiredSimple = new String[] {ID_ATTRIBUTE, VERSION_RANGE_ATTRIBUTE};
		private final String[] optionalSimple = new String[] {UPDATE_DESCRIPTOR_SEVERITY, DESCRIPTION_ATTRIBUTE};

		private final String[] requiredComplex = new String[] {MATCH_ATTRIBUTE};
		private final String[] optionalComplex = new String[] {UPDATE_DESCRIPTOR_SEVERITY, DESCRIPTION_ATTRIBUTE, MATCH_PARAMETERS_ATTRIBUTE};

		private IUpdateDescriptor descriptor;

		public UpdateDescriptorHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, INSTALLABLE_UNIT_ELEMENT);
			boolean simple = attributes.getIndex(ID_ATTRIBUTE) >= 0;
			String[] values;
			int severityIdx;
			String description;
			if (simple) {
				values = parseAttributes(attributes, requiredSimple, optionalSimple);
				severityIdx = 2;
				description = values[3];
			} else {
				values = parseAttributes(attributes, requiredComplex, optionalComplex);
				severityIdx = 1;
				description = values[2];
			}

			int severity;
			try {
				severity = Integer.parseInt(values[severityIdx]);
			} catch (NumberFormatException e) {
				invalidAttributeValue(UPDATE_DESCRIPTOR_ELEMENT, UPDATE_DESCRIPTOR_SEVERITY, values[severityIdx]);
				severity = IUpdateDescriptor.NORMAL;
			}
			URI location = parseURIAttribute(attributes, false);

			if (simple) {
				VersionRange range = checkVersionRange(REQUIREMENT_ELEMENT, VERSION_RANGE_ATTRIBUTE, values[1]);
				descriptor = MetadataFactory.createUpdateDescriptor(values[0], range, severity, description, location);
			} else {
				IMatchExpression<IInstallableUnit> r = createMatchExpression(values[0], values[3]);
				descriptor = MetadataFactory.createUpdateDescriptor(Collections.singleton(r), severity, description, location);
			}
		}

		public IUpdateDescriptor getUpdateDescriptor() {
			return descriptor;
		}
	}

	/**
	 * 	Handler for a list of licenses.
	 */
	protected class LicensesHandler extends AbstractHandler {

		// Note this handler is set up to handle multiple license elements, but for now
		// the API for IInstallableUnit only reflects one.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=216911
		private List<ILicense> licenses;

		public LicensesHandler(ContentHandler parentHandler, Attributes attributes) {
			super(parentHandler, LICENSES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			licenses = (size != null ? new ArrayList<>(Integer.parseInt(size)) : new ArrayList<>(2));
		}

		public ILicense[] getLicenses() {
			if (licenses.size() == 0)
				return NO_LICENSES;
			return licenses.toArray(new ILicense[licenses.size()]);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(LICENSE_ELEMENT)) {
				new LicenseHandler(this, attributes, licenses);
			} else {
				invalidElement(name, attributes);
			}
		}

	}

	/**
	 * 	Handler for a license in an list of licenses.
	 */
	protected class LicenseHandler extends TextHandler {

		URI location = null;

		private final List<ILicense> licenses;

		public LicenseHandler(AbstractHandler parentHandler, Attributes attributes, List<ILicense> licenses) {
			super(parentHandler, LICENSE_ELEMENT);
			location = parseURIAttribute(attributes, false);
			this.licenses = licenses;
		}

		@Override
		protected void finished() {
			if (isValidXML()) {
				licenses.add(MetadataFactory.createLicense(location, getText()));
			}
		}
	}

	/**
	 * 	Handler for a copyright.
	 */
	protected class CopyrightHandler extends TextHandler {

		URI location = null;
		private ICopyright copyright;

		public CopyrightHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, COPYRIGHT_ELEMENT);
			location = parseURIAttribute(attributes, false);
		}

		@Override
		protected void finished() {
			if (isValidXML()) {
				copyright = MetadataFactory.createCopyright(location, getText());
			}
		}

		public ICopyright getCopyright() {
			return copyright;
		}
	}

	static IMatchExpression<IInstallableUnit> createMatchExpression(String match, String matchParams) {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		IExpression expr = ExpressionUtil.parse(match);
		Object[] params;
		if (matchParams == null)
			params = new Object[0];
		else {
			IExpression[] arrayExpr = ExpressionUtil.getOperands(ExpressionUtil.parse(matchParams));
			params = new Object[arrayExpr.length];
			for (int idx = 0; idx < arrayExpr.length; ++idx)
				params[idx] = arrayExpr[idx].evaluate(null);
		}
		return factory.matchExpression(expr, params);
	}
}
