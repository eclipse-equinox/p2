/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository.io;

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.persistence.XMLParser;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.xml.sax.Attributes;

public abstract class MetadataParser extends XMLParser implements XMLConstants {

	public MetadataParser(BundleContext context, String bundleId) {
		super(context, bundleId);
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

		private final String[] required = new String[] {ID_ATTRIBUTE, VERSION_ATTRIBUTE};
		private final String[] optional = new String[] {SINGLETON_ATTRIBUTE, FRAGMENT_ATTRIBUTE, FRAGMENT_HOST_ID_ATTRIBUTE, FRAGMENT_HOST_RANGE_ATTRIBUTE};

		InstallableUnitDescription currentUnit = null;

		private PropertiesHandler propertiesHandler = null;
		private ProvidedCapabilitiesHandler providedCapabilitiesHandler = null;
		private RequiredCapabilitiesHandler requiredCapabilitiesHandler = null;
		private TextHandler filterHandler = null;
		private TextHandler applicabilityHandler = null;
		private ArtifactsHandler artifactsHandler = null;
		private TouchpointTypeHandler touchpointTypeHandler = null;
		private TouchpointDataHandler touchpointDataHandler = null;

		public InstallableUnitHandler(AbstractHandler parentHandler, Attributes attributes, List units) {
			super(parentHandler, INSTALLABLE_UNIT_ELEMENT);
			String[] values = parseAttributes(attributes, required, optional);
			//skip entire IU if the id is missing
			if (values[0] == null)
				return;
			Version version = checkVersion(INSTALLABLE_UNIT_ELEMENT, VERSION_ATTRIBUTE, values[1]);
			boolean singleton = checkBoolean(INSTALLABLE_UNIT_ELEMENT, SINGLETON_ATTRIBUTE, values[2], true).booleanValue();
			boolean isFragment = checkBoolean(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_ATTRIBUTE, values[3], false).booleanValue();
			if (isFragment) {
				String hostId = values[4];
				// TODO: tooling default fragment does not have a host id
				if (hostId != null)
					checkRequiredAttribute(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_HOST_ID_ATTRIBUTE, hostId);
				checkRequiredAttribute(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_HOST_RANGE_ATTRIBUTE, values[5]);
				VersionRange hostRange = checkVersionRange(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_HOST_RANGE_ATTRIBUTE, values[5]);
				currentUnit = new InstallableUnitFragmentDescription();
				currentUnit.setId(values[0]);
				currentUnit.setVersion(version);
				currentUnit.setSingleton(singleton);
				((InstallableUnitFragmentDescription) currentUnit).setHost(values[4], hostRange);
			} else {
				if (values[4] != null) {
					unexpectedAttribute(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_HOST_ID_ATTRIBUTE, values[4]);
				} else if (values[5] != null) {
					unexpectedAttribute(INSTALLABLE_UNIT_ELEMENT, FRAGMENT_HOST_RANGE_ATTRIBUTE, values[4]);
				}
				currentUnit = new InstallableUnitDescription();
				currentUnit.setId(values[0]);
				currentUnit.setVersion(version);
				currentUnit.setSingleton(singleton);
			}
			units.add(currentUnit);
		}

		public IInstallableUnit getInstallableUnit() {
			return MetadataFactory.createInstallableUnit(currentUnit);
		}

		public void startElement(String name, Attributes attributes) {
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
			} else if (IU_FILTER_ELEMENT.equals(name)) {
				if (filterHandler == null) {
					filterHandler = new TextHandler(this, IU_FILTER_ELEMENT, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (APPLICABILITY_FILTER_ELEMENT.equals(name)) {
				if (applicabilityHandler == null) {
					applicabilityHandler = new TextHandler(this, APPLICABILITY_FILTER_ELEMENT, attributes);
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
			} else {
				invalidElement(name, attributes);
			}
		}

		protected void finished() {
			if (isValidXML() && currentUnit != null) {
				OrderedProperties properties = (propertiesHandler == null ? new OrderedProperties(0) //
						: propertiesHandler.getProperties());
				for (Enumeration e = properties.keys(); e.hasMoreElements();) {
					String key = (String) e.nextElement();
					String value = properties.getProperty(key);
					currentUnit.setProperty(key, value);
				}
				ProvidedCapability[] providedCapabilities = (providedCapabilitiesHandler == null ? new ProvidedCapability[0] //
						: providedCapabilitiesHandler.getProvidedCapabilities());
				currentUnit.setCapabilities(providedCapabilities);
				RequiredCapability[] requiredCapabilities = (requiredCapabilitiesHandler == null ? new RequiredCapability[0] //
						: requiredCapabilitiesHandler.getRequiredCapabilities());
				currentUnit.setRequiredCapabilities(requiredCapabilities);
				if (filterHandler != null) {
					currentUnit.setFilter(filterHandler.getText());
				}
				if (applicabilityHandler != null) {
					currentUnit.setApplicabilityFilter(applicabilityHandler.getText());
				}
				IArtifactKey[] artifacts = (artifactsHandler == null ? new IArtifactKey[0] //
						: artifactsHandler.getArtifactKeys());
				currentUnit.setArtifacts(artifacts);
				if (touchpointTypeHandler != null) {
					currentUnit.setTouchpointType(touchpointTypeHandler.getTouchpointType());
				} else {
					// TODO: create an error
				}
				TouchpointData[] touchpointData = (touchpointDataHandler == null ? new TouchpointData[0] //
						: touchpointDataHandler.getTouchpointData());
				for (int i = 0; i < touchpointData.length; i++)
					currentUnit.addTouchpointData(touchpointData[i]);
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

		public ProvidedCapability[] getProvidedCapabilities() {
			return (ProvidedCapability[]) providedCapabilities.toArray(new ProvidedCapability[providedCapabilities.size()]);
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

		private final String[] required = new String[] {NAMESPACE_ATTRIBUTE, NAME_ATTRIBUTE, VERSION_ATTRIBUTE};

		public ProvidedCapabilityHandler(AbstractHandler parentHandler, Attributes attributes, List capabilities) {
			super(parentHandler, PROVIDED_CAPABILITY_ELEMENT);
			String[] values = parseRequiredAttributes(attributes, required);
			Version version = checkVersion(PROVIDED_CAPABILITY_ELEMENT, VERSION_ATTRIBUTE, values[2]);
			capabilities.add(new ProvidedCapability(values[0], values[1], version));
		}

		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}
	}

	protected class RequiredCapabilitiesHandler extends AbstractHandler {

		private List requiredCapabilities;

		public RequiredCapabilitiesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, REQUIRED_CAPABILITIES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			requiredCapabilities = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public RequiredCapability[] getRequiredCapabilities() {
			return (RequiredCapability[]) requiredCapabilities.toArray(new RequiredCapability[requiredCapabilities.size()]);
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

		private final String[] required = new String[] {NAMESPACE_ATTRIBUTE, NAME_ATTRIBUTE, VERSION_RANGE_ATTRIBUTE};
		private final String[] optional = new String[] {CAPABILITY_OPTIONAL_ATTRIBUTE, CAPABILITY_MULTIPLE_ATTRIBUTE};

		private RequiredCapability currentCapability = null;

		private TextHandler filterHandler = null;
		private CapabilitySelectorsHandler selectorsHandler = null;

		public RequiredCapabilityHandler(AbstractHandler parentHandler, Attributes attributes, List capabilities) {
			super(parentHandler, REQUIRED_CAPABILITY_ELEMENT);
			String[] values = parseAttributes(attributes, required, optional);
			VersionRange range = checkVersionRange(REQUIRED_CAPABILITY_ELEMENT, VERSION_RANGE_ATTRIBUTE, values[2]);
			boolean isOptional = checkBoolean(REQUIRED_CAPABILITY_ELEMENT, CAPABILITY_OPTIONAL_ATTRIBUTE, values[3], false).booleanValue();
			boolean isMultiple = checkBoolean(REQUIRED_CAPABILITY_ELEMENT, CAPABILITY_MULTIPLE_ATTRIBUTE, values[4], false).booleanValue();
			currentCapability = new RequiredCapability(values[0], values[1], range, null, isOptional, isMultiple);
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

		private final String[] required = new String[] {NAMESPACE_ATTRIBUTE, CLASSIFIER_ATTRIBUTE, ID_ATTRIBUTE, VERSION_ATTRIBUTE};

		public ArtifactHandler(AbstractHandler parentHandler, Attributes attributes, List artifacts) {
			super(parentHandler, ARTIFACT_KEY_ELEMENT);
			String[] values = parseRequiredAttributes(attributes, required);
			Version version = checkVersion(ARTIFACT_KEY_ELEMENT, VERSION_ATTRIBUTE, values[3]);
			artifacts.add(new ArtifactKey(values[0], values[1], values[2], version));
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

		TouchpointType touchpointType = null;

		public TouchpointTypeHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, TOUCHPOINT_TYPE_ELEMENT);
			String[] values = parseRequiredAttributes(attributes, required);
			Version version = checkVersion(TOUCHPOINT_TYPE_ELEMENT, VERSION_ATTRIBUTE, values[1]);
			touchpointType = new TouchpointType(values[0], version);
		}

		public TouchpointType getTouchpointType() {
			return touchpointType;
		}

		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}
	}

	protected class TouchpointDataHandler extends AbstractHandler {

		TouchpointData touchpointData = null;

		List data = null;

		public TouchpointDataHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, TOUCHPOINT_DATA_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			data = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public TouchpointData[] getTouchpointData() {
			return (TouchpointData[]) data.toArray(new TouchpointData[data.size()]);
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT)) {
				new TouchpointInstructionsHandler(this, attributes, data);
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
			data.add(new TouchpointData(instructions));
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

		Map instructions = null;
		String key = null;

		public TouchpointInstructionHandler(AbstractHandler parentHandler, Attributes attributes, Map instructions) {
			super(parentHandler, TOUCHPOINT_DATA_INSTRUCTION_ELEMENT);
			key = parseRequiredAttributes(attributes, required)[0];
			this.instructions = instructions;
		}

		protected void finished() {
			if (isValidXML()) {
				if (key != null) {
					instructions.put(key, getText());
				}
			}
		}
	}
}
