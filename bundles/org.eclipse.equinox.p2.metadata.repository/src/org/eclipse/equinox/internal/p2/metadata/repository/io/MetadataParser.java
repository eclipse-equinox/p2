/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *		EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository.io;

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;

import java.net.URI;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.persistence.XMLParser;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.*;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.RepositoryReference;
import org.osgi.framework.BundleContext;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

public abstract class MetadataParser extends XMLParser implements XMLConstants {

	public MetadataParser(BundleContext context, String bundleId) {
		super(context, bundleId);
	}

	protected class RepositoryReferencesHandler extends AbstractHandler {
		private HashSet references;

		public RepositoryReferencesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, REPOSITORY_REFERENCES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			references = (size != null ? new HashSet(Integer.parseInt(size)) : new HashSet(4));
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(REPOSITORY_REFERENCE_ELEMENT)) {
				new RepositoryReferenceHandler(this, attributes, references);
			} else {
				invalidElement(name, attributes);
			}
		}

		public RepositoryReference[] getReferences() {
			return (RepositoryReference[]) references.toArray(new RepositoryReference[references.size()]);
		}
	}

	protected class RepositoryReferenceHandler extends AbstractHandler {

		private final String[] required = new String[] {TYPE_ATTRIBUTE, OPTIONS_ATTRIBUTE};

		public RepositoryReferenceHandler(AbstractHandler parentHandler, Attributes attributes, Set references) {
			super(parentHandler, REPOSITORY_REFERENCE_ELEMENT);
			String[] values = parseRequiredAttributes(attributes, required);
			String name = parseOptionalAttribute(attributes, NAME_ATTRIBUTE);
			int type = checkInteger(elementHandled, TYPE_ATTRIBUTE, values[0]);
			int options = checkInteger(elementHandled, OPTIONS_ATTRIBUTE, values[1]);
			URI location = parseURIAttribute(attributes, true);
			if (location != null)
				references.add(new RepositoryReference(location, name, type, options));
		}

		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}
	}

	protected class InstallableUnitsHandler extends AbstractHandler {
		private ArrayList units;

		public InstallableUnitsHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, INSTALLABLE_UNITS_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			units = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public IInstallableUnit[] getUnits() {
			int size = units.size();
			IInstallableUnit[] result = new IInstallableUnit[size];
			int i = 0;
			for (Iterator it = units.iterator(); it.hasNext(); i++) {
				InstallableUnitDescription desc = (InstallableUnitDescription) it.next();
				result[i] = MetadataFactory.createInstallableUnit(desc);
			}
			return result;
		}

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
		private RequiredCapabilitiesHandler requiredCapabilitiesHandler = null;
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

		private List units;

		public InstallableUnitHandler(AbstractHandler parentHandler, Attributes attributes, List units) {
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
			} else if (REQUIRED_CAPABILITIES_ELEMENT.equals(name)) {
				if (requiredCapabilitiesHandler == null) {
					requiredCapabilitiesHandler = new RequiredCapabilitiesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (HOST_REQUIRED_CAPABILITIES_ELEMENT.equals(name)) {
				if (hostRequiredCapabilitiesHandler == null) {
					hostRequiredCapabilitiesHandler = new HostRequiredCapabilitiesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (META_REQUIRED_CAPABILITIES_ELEMENT.equals(name)) {
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

		protected void finished() {
			if (isValidXML()) {
				if (requirementChangesHandler != null) {
					currentUnit = new MetadataFactory.InstallableUnitPatchDescription();
					((InstallableUnitPatchDescription) currentUnit).setRequirementChanges((IRequirementChange[]) requirementChangesHandler.getRequirementChanges().toArray(new IRequirementChange[requirementChangesHandler.getRequirementChanges().size()]));
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
				for (Enumeration e = properties.keys(); e.hasMoreElements();) {
					String key = (String) e.nextElement();
					String value = properties.getProperty(key);
					//Backward compatibility
					if (key.equals("equinox.p2.update.from")) {
						updateFrom = value;
						continue;
					}
					if (key.equals("equinox.p2.update.range")) {
						updateRange = new VersionRange(value);
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
					ILicense license = licensesHandler.getLicense();
					currentUnit.setLicense(license);
				}

				if (copyrightHandler != null) {
					ICopyright copyright = copyrightHandler.getCopyright();
					currentUnit.setCopyright(copyright);
				}

				IProvidedCapability[] providedCapabilities = (providedCapabilitiesHandler == null ? new IProvidedCapability[0] : providedCapabilitiesHandler.getProvidedCapabilities());
				currentUnit.setCapabilities(providedCapabilities);
				IRequiredCapability[] requiredCapabilities = (requiredCapabilitiesHandler == null ? new IRequiredCapability[0] : requiredCapabilitiesHandler.getRequiredCapabilities());
				currentUnit.setRequiredCapabilities(requiredCapabilities);
				IRequiredCapability[] metaRequiredCapabilities = (metaRequiredCapabilitiesHandler == null ? new IRequiredCapability[0] : metaRequiredCapabilitiesHandler.getMetaRequiredCapabilities());
				currentUnit.setMetaRequiredCapabilities(metaRequiredCapabilities);
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

	protected class ApplicabilityScopesHandler extends AbstractHandler {
		private List scopes;

		public ApplicabilityScopesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, APPLICABILITY_SCOPE);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			scopes = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public void startElement(String name, Attributes attributes) {
			if (APPLY_ON.equals(name)) {
				new ApplicabilityScopeHandler(this, attributes, scopes);
			} else {
				duplicateElement(this, name, attributes);
			}
		}

		public IRequiredCapability[][] getScope() {
			return (IRequiredCapability[][]) scopes.toArray(new IRequiredCapability[scopes.size()][]);
		}
	}

	protected class ApplicabilityScopeHandler extends AbstractHandler {
		private RequiredCapabilitiesHandler children;
		private List scopes;

		public ApplicabilityScopeHandler(AbstractHandler parentHandler, Attributes attributes, List scopes) {
			super(parentHandler, APPLY_ON);
			this.scopes = scopes;
		}

		public void startElement(String name, Attributes attributes) {
			if (REQUIRED_CAPABILITIES_ELEMENT.equals(name)) {
				children = new RequiredCapabilitiesHandler(this, attributes);
			} else {
				duplicateElement(this, name, attributes);
			}
		}

		protected void finished() {
			if (children != null) {
				scopes.add(children.getRequiredCapabilities());
			}
		}
	}

	protected class RequirementsChangeHandler extends AbstractHandler {
		private List requirementChanges;

		public RequirementsChangeHandler(InstallableUnitHandler parentHandler, Attributes attributes) {
			super(parentHandler, REQUIREMENT_CHANGES);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			requirementChanges = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(REQUIREMENT_CHANGE)) {
				new RequirementChangeHandler(this, attributes, requirementChanges);
			} else {
				invalidElement(name, attributes);
			}
		}

		public List getRequirementChanges() {
			return requirementChanges;
		}
	}

	protected class RequirementChangeHandler extends AbstractHandler {
		private List from;
		private List to;
		private List requirementChanges;

		public RequirementChangeHandler(AbstractHandler parentHandler, Attributes attributes, List requirementChanges) {
			super(parentHandler, REQUIREMENT_CHANGE);
			from = new ArrayList(1);
			to = new ArrayList(1);
			this.requirementChanges = requirementChanges;
		}

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

		protected void finished() {
			requirementChanges.add(MetadataFactory.createRequirementChange(from.size() == 0 ? null : (IRequiredCapability) from.get(0), to.size() == 0 ? null : (IRequiredCapability) to.get(0)));
		}
	}

	protected class RequirementChangeEltHandler extends AbstractHandler {
		private List requirement;

		public RequirementChangeEltHandler(AbstractHandler parentHandler, String parentId, Attributes attributes, List from) {
			super(parentHandler, parentId);
			requirement = from;
		}

		public void startElement(String name, Attributes attributes) {
			if (REQUIRED_CAPABILITY_ELEMENT.equals(name))
				new RequiredCapabilityHandler(this, attributes, requirement);
			else {
				invalidElement(name, attributes);
			}
		}

	}

	protected class LifeCycleHandler extends AbstractHandler {
		private List lifeCycleRequirement;

		public LifeCycleHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, LIFECYCLE);
			lifeCycleRequirement = new ArrayList(1);
		}

		public IRequiredCapability getLifeCycleRequirement() {
			if (lifeCycleRequirement.size() == 0)
				return null;
			return (IRequiredCapability) lifeCycleRequirement.get(0);
		}

		public void startElement(String name, Attributes attributes) {
			if (REQUIRED_CAPABILITY_ELEMENT.equals(name)) {
				new RequiredCapabilityHandler(this, attributes, lifeCycleRequirement);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class ProvidedCapabilitiesHandler extends AbstractHandler {
		private List providedCapabilities;

		public ProvidedCapabilitiesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, PROVIDED_CAPABILITIES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			providedCapabilities = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public IProvidedCapability[] getProvidedCapabilities() {
			return (IProvidedCapability[]) providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]);
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(PROVIDED_CAPABILITY_ELEMENT)) {
				new ProvidedCapabilityHandler(this, attributes, providedCapabilities);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class ProvidedCapabilityHandler extends AbstractHandler {

		public ProvidedCapabilityHandler(AbstractHandler parentHandler, Attributes attributes, List capabilities) {
			super(parentHandler, PROVIDED_CAPABILITY_ELEMENT);
			String[] values = parseRequiredAttributes(attributes, REQUIRED_PROVIDED_CAPABILITY_ATTRIBUTES);
			Version version = checkVersion(PROVIDED_CAPABILITY_ELEMENT, VERSION_ATTRIBUTE, values[2]);
			capabilities.add(MetadataFactory.createProvidedCapability(values[0], values[1], version));
		}

		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}
	}

	protected class HostRequiredCapabilitiesHandler extends AbstractHandler {
		private List requiredCapabilities;

		public HostRequiredCapabilitiesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, HOST_REQUIRED_CAPABILITIES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			requiredCapabilities = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public IRequiredCapability[] getHostRequiredCapabilities() {
			return (IRequiredCapability[]) requiredCapabilities.toArray(new IRequiredCapability[requiredCapabilities.size()]);
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(REQUIRED_CAPABILITY_ELEMENT)) {
				new RequiredCapabilityHandler(this, attributes, requiredCapabilities);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class MetaRequiredCapabilitiesHandler extends AbstractHandler {
		private List requiredCapabilities;

		public MetaRequiredCapabilitiesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, META_REQUIRED_CAPABILITIES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			requiredCapabilities = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public IRequiredCapability[] getMetaRequiredCapabilities() {
			return (IRequiredCapability[]) requiredCapabilities.toArray(new IRequiredCapability[requiredCapabilities.size()]);
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(REQUIRED_CAPABILITY_ELEMENT)) {
				new RequiredCapabilityHandler(this, attributes, requiredCapabilities);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class RequiredCapabilitiesHandler extends AbstractHandler {
		private List requiredCapabilities;

		public RequiredCapabilitiesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, REQUIRED_CAPABILITIES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			requiredCapabilities = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public IRequiredCapability[] getRequiredCapabilities() {
			return (IRequiredCapability[]) requiredCapabilities.toArray(new IRequiredCapability[requiredCapabilities.size()]);
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(REQUIRED_CAPABILITY_ELEMENT)) {
				new RequiredCapabilityHandler(this, attributes, requiredCapabilities);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class RequiredCapabilityHandler extends AbstractHandler {

		private IRequiredCapability currentCapability = null;

		private TextHandler filterHandler = null;
		private CapabilitySelectorsHandler selectorsHandler = null;

		public RequiredCapabilityHandler(AbstractHandler parentHandler, Attributes attributes, List capabilities) {
			super(parentHandler, REQUIRED_CAPABILITY_ELEMENT);
			String[] values = parseAttributes(attributes, REQIURED_CAPABILITY_ATTRIBUTES, OPTIONAL_CAPABILITY_ATTRIBUTES);
			VersionRange range = checkVersionRange(REQUIRED_CAPABILITY_ELEMENT, VERSION_RANGE_ATTRIBUTE, values[2]);
			boolean isOptional = checkBoolean(REQUIRED_CAPABILITY_ELEMENT, CAPABILITY_OPTIONAL_ATTRIBUTE, values[3], false).booleanValue();
			boolean isMultiple = checkBoolean(REQUIRED_CAPABILITY_ELEMENT, CAPABILITY_MULTIPLE_ATTRIBUTE, values[4], false).booleanValue();
			boolean isGreedy = checkBoolean(REQUIRED_CAPABILITY_ELEMENT, CAPABILITY_GREED_ATTRIBUTE, values[5], true).booleanValue();
			currentCapability = MetadataFactory.createRequiredCapability(values[0], values[1], range, null, isOptional, isMultiple, isGreedy);
			capabilities.add(currentCapability);
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(CAPABILITY_FILTER_ELEMENT)) {
				filterHandler = new TextHandler(this, CAPABILITY_FILTER_ELEMENT, attributes);
			} else if (name.equals(CAPABILITY_SELECTORS_ELEMENT)) {
				selectorsHandler = new CapabilitySelectorsHandler(this, attributes);
			} else {
				invalidElement(name, attributes);
			}
		}

		protected void finished() {
			if (isValidXML()) {
				if (currentCapability != null) {
					if (filterHandler != null) {
						currentCapability.setFilter(filterHandler.getText());
					}
					if (selectorsHandler != null) {
						currentCapability.setSelectors(selectorsHandler.getSelectors());
					}
				}
			}
		}
	}

	protected class ArtifactsHandler extends AbstractHandler {

		private List artifacts;

		public ArtifactsHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, ARTIFACT_KEYS_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			artifacts = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public IArtifactKey[] getArtifactKeys() {
			return (IArtifactKey[]) artifacts.toArray(new IArtifactKey[artifacts.size()]);
		}

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

		public ArtifactHandler(AbstractHandler parentHandler, Attributes attributes, List artifacts) {
			super(parentHandler, ARTIFACT_KEY_ELEMENT);
			String[] values = parseRequiredAttributes(attributes, required);
			Version version = checkVersion(ARTIFACT_KEY_ELEMENT, VERSION_ATTRIBUTE, values[2]);
			artifacts.add(new ArtifactKey(values[0], values[1], version));
		}

		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}
	}

	protected class CapabilitySelectorsHandler extends AbstractHandler {

		private List selectors;

		public CapabilitySelectorsHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, CAPABILITY_SELECTORS_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			selectors = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public String[] getSelectors() {
			return (String[]) selectors.toArray(new String[selectors.size()]);
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(CAPABILITY_SELECTOR_ELEMENT)) {
				new TextHandler(this, CAPABILITY_SELECTOR_ELEMENT, attributes, selectors);
			} else {
				invalidElement(name, attributes);
			}
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

		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}
	}

	protected class TouchpointDataHandler extends AbstractHandler {

		ITouchpointData touchpointData = null;

		List data = null;

		public TouchpointDataHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, TOUCHPOINT_DATA_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			data = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public ITouchpointData[] getTouchpointData() {
			ITouchpointData[] result = new ITouchpointData[data.size()];
			for (int i = 0; i < result.length; i++)
				result[i] = ((TouchpointInstructionsHandler) data.get(i)).getTouchpointData();
			return result;
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT)) {
				data.add(new TouchpointInstructionsHandler(this, attributes, data));
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class TouchpointInstructionsHandler extends AbstractHandler {

		Map instructions = null;

		public TouchpointInstructionsHandler(AbstractHandler parentHandler, Attributes attributes, List data) {
			super(parentHandler, TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			instructions = (size != null ? new LinkedHashMap(new Integer(size).intValue()) : new LinkedHashMap(4));
		}

		public ITouchpointData getTouchpointData() {
			return MetadataFactory.createTouchpointData(instructions);
		}

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

		Map instructions = null;
		String key = null;
		String qualifier = null;

		public TouchpointInstructionHandler(AbstractHandler parentHandler, Attributes attributes, Map instructions) {
			super(parentHandler, TOUCHPOINT_DATA_INSTRUCTION_ELEMENT);
			String[] values = parseAttributes(attributes, required, optional);
			key = values[0];
			qualifier = values[1];
			this.instructions = instructions;
		}

		protected void finished() {
			if (isValidXML()) {
				if (key != null) {
					instructions.put(key, MetadataFactory.createTouchpointInstruction(getText(), qualifier));
				}
			}
		}
	}

	protected class UpdateDescriptorHandler extends TextHandler {
		private final String[] required = new String[] {ID_ATTRIBUTE, VERSION_RANGE_ATTRIBUTE};
		private final String[] optional = new String[] {UPDATE_DESCRIPTOR_SEVERITY, DESCRIPTION_ATTRIBUTE};

		private IUpdateDescriptor descriptor;

		public UpdateDescriptorHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, INSTALLABLE_UNIT_ELEMENT);
			String[] values = parseAttributes(attributes, required, optional);
			VersionRange range = checkVersionRange(REQUIRED_CAPABILITY_ELEMENT, VERSION_RANGE_ATTRIBUTE, values[1]);
			int severity = new Integer(values[2]).intValue();
			descriptor = MetadataFactory.createUpdateDescriptor(values[0], range, severity, values[3]);
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
		private List licenses;

		public LicensesHandler(ContentHandler parentHandler, Attributes attributes) {
			super(parentHandler, LICENSES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			licenses = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(2));
		}

		public ILicense getLicense() {
			if (licenses.size() == 0)
				return null;
			return (ILicense) licenses.get(0);
		}

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

		private final List licenses;

		public LicenseHandler(AbstractHandler parentHandler, Attributes attributes, List licenses) {
			super(parentHandler, LICENSE_ELEMENT);
			location = parseURIAttribute(attributes, false);
			this.licenses = licenses;
		}

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

		protected void finished() {
			if (isValidXML()) {
				copyright = MetadataFactory.createCopyright(location, getText());
			}
		}

		public ICopyright getCopyright() {
			return copyright;
		}
	}
}
