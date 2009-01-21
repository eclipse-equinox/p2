/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository.io;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.Activator;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public abstract class MetadataWriter extends XMLWriter implements XMLConstants {

	public MetadataWriter(OutputStream output, ProcessingInstruction[] piElements) throws UnsupportedEncodingException {
		super(output, piElements);
		// TODO: add a processing instruction for the metadata version
	}

	/**
	 * Writes a list of {@link IInstallableUnit}.
	 * @param units An Iterator of {@link IInstallableUnit}.
	 * @param size The number of units to write
	 */
	protected void writeInstallableUnits(Iterator units, int size) {
		if (size == 0)
			return;
		start(INSTALLABLE_UNITS_ELEMENT);
		attribute(COLLECTION_SIZE_ATTRIBUTE, size);
		while (units.hasNext())
			writeInstallableUnit((IInstallableUnit) units.next());
		end(INSTALLABLE_UNITS_ELEMENT);
	}

	protected void writeInstallableUnit(IInstallableUnit resolvedIU) {
		IInstallableUnit iu = resolvedIU.unresolved();
		start(INSTALLABLE_UNIT_ELEMENT);
		attribute(ID_ATTRIBUTE, iu.getId());
		attribute(VERSION_ATTRIBUTE, iu.getVersion());
		attribute(SINGLETON_ATTRIBUTE, iu.isSingleton(), true);
		//		attribute(FRAGMENT_ATTRIBUTE, iu.isFragment(), false);

		if (iu.isFragment() && iu instanceof IInstallableUnitFragment) {
			IInstallableUnitFragment fragment = (IInstallableUnitFragment) iu;
			writeHostRequiredCapabilities(fragment.getHost());
		}

		if (iu instanceof IInstallableUnitPatch) {
			IInstallableUnitPatch patch = (IInstallableUnitPatch) iu;
			writeApplicabilityScope(patch.getApplicabilityScope());
			writeRequirementsChange(patch.getRequirementsChange());
			writeLifeCycle(patch.getLifeCycle());
		}

		writeUpdateDescriptor(resolvedIU, resolvedIU.getUpdateDescriptor());
		writeProperties(iu.getProperties());
		writeProvidedCapabilities(iu.getProvidedCapabilities());
		writeRequiredCapabilities(iu.getRequiredCapabilities());
		writeTrimmedCdata(IU_FILTER_ELEMENT, iu.getFilter());

		writeArtifactKeys(iu.getArtifacts());
		writeTouchpointType(iu.getTouchpointType());
		writeTouchpointData(iu.getTouchpointData());
		writeLicenses(iu.getLicense());
		writeCopyright(iu.getCopyright());

		end(INSTALLABLE_UNIT_ELEMENT);
	}

	protected void writeLifeCycle(IRequiredCapability capability) {
		if (capability == null)
			return;
		start(LIFECYCLE);
		writeRequiredCapability(capability);
		end(LIFECYCLE);
	}

	protected void writeHostRequiredCapabilities(IRequiredCapability[] capabilities) {
		if (capabilities != null && capabilities.length > 0) {
			start(HOST_REQUIRED_CAPABILITIES_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, capabilities.length);
			for (int i = 0; i < capabilities.length; i++) {
				writeRequiredCapability(capabilities[i]);
			}
			end(HOST_REQUIRED_CAPABILITIES_ELEMENT);
		}
	}

	protected void writeProvidedCapabilities(IProvidedCapability[] capabilities) {
		if (capabilities != null && capabilities.length > 0) {
			start(PROVIDED_CAPABILITIES_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, capabilities.length);
			for (int i = 0; i < capabilities.length; i++) {
				start(PROVIDED_CAPABILITY_ELEMENT);
				attribute(NAMESPACE_ATTRIBUTE, capabilities[i].getNamespace());
				attribute(NAME_ATTRIBUTE, capabilities[i].getName());
				attribute(VERSION_ATTRIBUTE, capabilities[i].getVersion());
				end(PROVIDED_CAPABILITY_ELEMENT);
			}
			end(PROVIDED_CAPABILITIES_ELEMENT);
		}
	}

	protected void writeRequiredCapabilities(IRequiredCapability[] capabilities) {
		if (capabilities != null && capabilities.length > 0) {
			start(REQUIRED_CAPABILITIES_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, capabilities.length);
			for (int i = 0; i < capabilities.length; i++) {
				writeRequiredCapability(capabilities[i]);
			}
			end(REQUIRED_CAPABILITIES_ELEMENT);
		}
	}

	protected void writeUpdateDescriptor(IInstallableUnit iu, IUpdateDescriptor descriptor) {
		if (descriptor == null)
			return;

		start(UPDATE_DESCRIPTOR_ELEMENT);
		attribute(ID_ATTRIBUTE, descriptor.getId());
		attribute(VERSION_RANGE_ATTRIBUTE, descriptor.getRange());
		attribute(UPDATE_DESCRIPTOR_SEVERITY, descriptor.getSeverity());
		attribute(DESCRIPTION_ATTRIBUTE, descriptor.getDescription());
		end(UPDATE_DESCRIPTOR_ELEMENT);
	}

	protected void writeApplicabilityScope(IRequiredCapability[][] capabilities) {
		start(APPLICABILITY_SCOPE);
		for (int i = 0; i < capabilities.length; i++) {
			start(APPLY_ON);
			writeRequiredCapabilities(capabilities[i]);
			end(APPLY_ON);
		}
		end(APPLICABILITY_SCOPE);
	}

	protected void writeRequirementsChange(IRequirementChange[] changes) {
		start(REQUIREMENT_CHANGES);
		for (int i = 0; i < changes.length; i++) {
			writeRequirementChange(changes[i]);
		}
		end(REQUIREMENT_CHANGES);
	}

	protected void writeRequirementChange(IRequirementChange change) {
		start(REQUIREMENT_CHANGE);
		if (change.applyOn() != null) {
			start(REQUIREMENT_FROM);
			writeRequiredCapability(change.applyOn());
			end(REQUIREMENT_FROM);
		}
		if (change.newValue() != null) {
			start(REQUIREMENT_TO);
			writeRequiredCapability(change.newValue());
			end(REQUIREMENT_TO);
		}
		end(REQUIREMENT_CHANGE);
	}

	protected void writeRequiredCapability(IRequiredCapability capability) {
		start(REQUIRED_CAPABILITY_ELEMENT);
		attribute(NAMESPACE_ATTRIBUTE, capability.getNamespace());
		attribute(NAME_ATTRIBUTE, capability.getName());
		attribute(VERSION_RANGE_ATTRIBUTE, capability.getRange());
		attribute(CAPABILITY_OPTIONAL_ATTRIBUTE, capability.isOptional(), false);
		attribute(CAPABILITY_MULTIPLE_ATTRIBUTE, capability.isMultiple(), false);
		attribute(CAPABILITY_GREED_ATTRIBUTE, capability.isGreedy(), true);
		writeTrimmedCdata(CAPABILITY_FILTER_ELEMENT, capability.getFilter());

		String[] selectors = capability.getSelectors();
		if (selectors.length > 0) {
			start(CAPABILITY_SELECTORS_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, selectors.length);
			for (int j = 0; j < selectors.length; j++) {
				writeTrimmedCdata(CAPABILITY_SELECTOR_ELEMENT, selectors[j]);
			}
			end(CAPABILITY_SELECTORS_ELEMENT);
		}

		end(REQUIRED_CAPABILITY_ELEMENT);
	}

	protected void writeArtifactKeys(IArtifactKey[] artifactKeys) {
		if (artifactKeys != null && artifactKeys.length > 0) {
			start(ARTIFACT_KEYS_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, artifactKeys.length);
			for (int i = 0; i < artifactKeys.length; i++) {
				start(ARTIFACT_KEY_ELEMENT);
				attribute(ARTIFACT_KEY_CLASSIFIER_ATTRIBUTE, artifactKeys[i].getClassifier());
				attribute(ID_ATTRIBUTE, artifactKeys[i].getId());
				attribute(VERSION_ATTRIBUTE, artifactKeys[i].getVersion());
				end(ARTIFACT_KEY_ELEMENT);
			}
			end(ARTIFACT_KEYS_ELEMENT);
		}
	}

	protected void writeTouchpointType(ITouchpointType touchpointType) {
		start(TOUCHPOINT_TYPE_ELEMENT);
		attribute(ID_ATTRIBUTE, touchpointType.getId());
		attribute(VERSION_ATTRIBUTE, touchpointType.getVersion());
		end(TOUCHPOINT_TYPE_ELEMENT);
	}

	protected void writeTouchpointData(ITouchpointData[] touchpointData) {
		if (touchpointData != null && touchpointData.length > 0) {
			start(TOUCHPOINT_DATA_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, touchpointData.length);
			for (int i = 0; i < touchpointData.length; i++) {
				ITouchpointData nextData = touchpointData[i];
				Map instructions = nextData.getInstructions();
				if (instructions.size() > 0) {
					start(TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT);
					attribute(COLLECTION_SIZE_ATTRIBUTE, instructions.size());
					for (Iterator iter = instructions.entrySet().iterator(); iter.hasNext();) {
						Map.Entry entry = (Map.Entry) iter.next();
						start(TOUCHPOINT_DATA_INSTRUCTION_ELEMENT);
						attribute(TOUCHPOINT_DATA_INSTRUCTION_KEY_ATTRIBUTE, entry.getKey());
						ITouchpointInstruction instruction = (ITouchpointInstruction) entry.getValue();
						if (instruction.getImportAttribute() != null)
							attribute(TOUCHPOINT_DATA_INSTRUCTION_IMPORT_ATTRIBUTE, instruction.getImportAttribute());
						cdata(instruction.getBody(), true);
						end(TOUCHPOINT_DATA_INSTRUCTION_ELEMENT);
					}
					end(TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT);
				}
			}
			end(TOUCHPOINT_DATA_ELEMENT);
		}
	}

	private void writeTrimmedCdata(String element, String filter) {
		String trimmed;
		if (filter != null && (trimmed = filter.trim()).length() > 0) {
			start(element);
			cdata(trimmed);
			end(element);
		}
	}

	private void writeLicenses(ILicense license) {
		if (license != null) {
			// In the future there may be more than one license, so we write this 
			// as a collection of one.
			// See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=216911
			start(LICENSES_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, 1);
			start(LICENSE_ELEMENT);
			if (license.getLocation() != null)
				attribute(URI_ATTRIBUTE, license.getLocation().toString());
			cdata(license.getBody(), true);
			end(LICENSE_ELEMENT);
			end(LICENSES_ELEMENT);
		}
	}

	private void writeCopyright(ICopyright copyright) {
		if (copyright != null) {
			start(COPYRIGHT_ELEMENT);
			try {
				if (copyright.getLocation() != null)
					attribute(URI_ATTRIBUTE, copyright.getLocation().toString());
			} catch (IllegalStateException ise) {
				LogHelper.log(new Status(IStatus.INFO, Activator.ID, "Error writing the copyright URL: " + copyright.getLocation())); //$NON-NLS-1$
			}
			cdata(copyright.getBody(), true);
			end(COPYRIGHT_ELEMENT);
		}
	}
}
