/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository.io;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.repository.Activator;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.*;

public class MetadataWriter extends XMLWriter implements XMLConstants {

	public MetadataWriter(OutputStream output, ProcessingInstruction[] piElements) throws UnsupportedEncodingException {
		super(output, piElements);
		// TODO: add a processing instruction for the metadata version
	}

	/**
	 * Writes a list of {@link IInstallableUnit}.
	 * @param units An Iterator of {@link IInstallableUnit}.
	 * @param size The number of units to write
	 */
	public void writeInstallableUnits(Iterator<IInstallableUnit> units, int size) {
		if (!units.hasNext())
			return;
		start(INSTALLABLE_UNITS_ELEMENT);

		// The size is a bummer. Is it really needed? It forces the use of a collect
		attribute(COLLECTION_SIZE_ATTRIBUTE, size);
		while (units.hasNext())
			writeInstallableUnit(units.next());
		end(INSTALLABLE_UNITS_ELEMENT);
	}

	protected void writeInstallableUnit(IInstallableUnit resolvedIU) {
		IInstallableUnit iu = resolvedIU.unresolved();
		start(INSTALLABLE_UNIT_ELEMENT);
		attribute(ID_ATTRIBUTE, iu.getId());
		attribute(VERSION_ATTRIBUTE, iu.getVersion());
		attribute(SINGLETON_ATTRIBUTE, iu.isSingleton(), true);
		//		attribute(FRAGMENT_ATTRIBUTE, iu.isFragment(), false);

		boolean simpleRequirements = hasOnlySimpleRequirements(iu);
		if (!simpleRequirements)
			attribute(GENERATION_ATTRIBUTE, 2);

		if (iu instanceof IInstallableUnitFragment) {
			IInstallableUnitFragment fragment = (IInstallableUnitFragment) iu;
			writeHostRequirements(fragment.getHost());
		}

		if (iu instanceof IInstallableUnitPatch) {
			IInstallableUnitPatch patch = (IInstallableUnitPatch) iu;
			writeApplicabilityScope(patch.getApplicabilityScope());
			writeRequirementsChange(patch.getRequirementsChange());
			writeLifeCycle(patch.getLifeCycle());
		}

		writeUpdateDescriptor(resolvedIU, resolvedIU.getUpdateDescriptor());
		writeProperties(iu.getProperties());
		writeMetaRequirements(iu.getMetaRequirements());
		writeProvidedCapabilities(iu.getProvidedCapabilities());
		if (simpleRequirements && iu instanceof IInstallableUnitFragment) {
			Collection<IRequirement> mergedRequirementsAndFragmentHostForPre36Compatibility = new LinkedHashSet<IRequirement>(iu.getRequirements());
			mergedRequirementsAndFragmentHostForPre36Compatibility.addAll(((IInstallableUnitFragment) iu).getHost());
			writeRequirements(mergedRequirementsAndFragmentHostForPre36Compatibility);
		} else {
			writeRequirements(iu.getRequirements());
		}
		writeTrimmedCdata(IU_FILTER_ELEMENT, iu.getFilter() == null ? null : iu.getFilter().getParameters()[0].toString());

		writeArtifactKeys(iu.getArtifacts());
		writeTouchpointType(iu.getTouchpointType());
		writeTouchpointData(iu.getTouchpointData());
		writeLicenses(iu.getLicenses());
		writeCopyright(iu.getCopyright());

		end(INSTALLABLE_UNIT_ELEMENT);
	}

	private boolean hasOnlySimpleRequirements(IInstallableUnit iu) {
		for (IRequirement r : iu.getRequirements())
			if (r.getMax() == 0 || !RequiredCapability.isSimpleRequirement(r.getMatches()))
				return false;

		if (iu.getUpdateDescriptor() != null) {
			for (IMatchExpression<IInstallableUnit> m : iu.getUpdateDescriptor().getIUsBeingUpdated()) {
				if (!RequiredCapability.isSimpleRequirement(m))
					return false;
			}
		}

		for (IRequirement r : iu.getMetaRequirements())
			if (r.getMax() == 0 || !RequiredCapability.isSimpleRequirement(r.getMatches()))
				return false;

		if (iu instanceof IInstallableUnitFragment) {
			for (IRequirement r : ((IInstallableUnitFragment) iu).getHost())
				if (!RequiredCapability.isSimpleRequirement(r.getMatches()))
					return false;
		}

		if (iu instanceof IInstallableUnitPatch) {
			IInstallableUnitPatch iuPatch = (IInstallableUnitPatch) iu;
			for (IRequirement[] rArr : iuPatch.getApplicabilityScope())
				for (IRequirement r : rArr)
					if (!RequiredCapability.isSimpleRequirement(r.getMatches()))
						return false;

			IRequirement lifeCycle = iuPatch.getLifeCycle();
			if (lifeCycle != null && !RequiredCapability.isSimpleRequirement(lifeCycle.getMatches()))
				return false;
		}
		return true;
	}

	protected void writeLifeCycle(IRequirement capability) {
		if (capability == null)
			return;
		start(LIFECYCLE);
		writeRequirement(capability);
		end(LIFECYCLE);
	}

	protected void writeHostRequirements(Collection<IRequirement> hostRequirements) {
		if (hostRequirements != null && hostRequirements.size() > 0) {
			start(HOST_REQUIREMENTS_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, hostRequirements.size());
			for (IRequirement req : hostRequirements) {
				writeRequirement(req);
			}
			end(HOST_REQUIREMENTS_ELEMENT);
		}
	}

	protected void writeProvidedCapabilities(Collection<IProvidedCapability> capabilities) {
		if (capabilities != null && capabilities.size() > 0) {
			start(PROVIDED_CAPABILITIES_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, capabilities.size());
			for (IProvidedCapability capability : capabilities) {
				start(PROVIDED_CAPABILITY_ELEMENT);
				attribute(NAMESPACE_ATTRIBUTE, capability.getNamespace());
				attribute(NAME_ATTRIBUTE, capability.getName());
				attribute(VERSION_ATTRIBUTE, capability.getVersion());
				end(PROVIDED_CAPABILITY_ELEMENT);
			}
			end(PROVIDED_CAPABILITIES_ELEMENT);
		}
	}

	protected void writeMetaRequirements(Collection<IRequirement> metaRequirements) {
		if (metaRequirements != null && metaRequirements.size() > 0) {
			start(META_REQUIREMENTS_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, metaRequirements.size());
			for (IRequirement req : metaRequirements) {
				writeRequirement(req);
			}
			end(META_REQUIREMENTS_ELEMENT);
		}
	}

	protected void writeRequirements(Collection<IRequirement> requirements) {
		if (requirements != null && requirements.size() > 0) {
			start(REQUIREMENTS_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, requirements.size());
			for (IRequirement req : requirements) {
				writeRequirement(req);
			}
			end(REQUIREMENTS_ELEMENT);
		}
	}

	protected void writeUpdateDescriptor(IInstallableUnit iu, IUpdateDescriptor descriptor) {
		if (descriptor == null)
			return;

		if (descriptor.getIUsBeingUpdated().size() > 1)
			throw new IllegalStateException();
		IMatchExpression<IInstallableUnit> singleUD = descriptor.getIUsBeingUpdated().iterator().next();
		start(UPDATE_DESCRIPTOR_ELEMENT);
		if (RequiredCapability.isSimpleRequirement(singleUD)) {
			attribute(ID_ATTRIBUTE, RequiredCapability.extractName(singleUD));
			attribute(VERSION_RANGE_ATTRIBUTE, RequiredCapability.extractRange(singleUD));
		} else {
			writeMatchExpression(singleUD);
		}
		attribute(UPDATE_DESCRIPTOR_SEVERITY, descriptor.getSeverity());
		attribute(DESCRIPTION_ATTRIBUTE, descriptor.getDescription());
		end(UPDATE_DESCRIPTOR_ELEMENT);
	}

	protected void writeApplicabilityScope(IRequirement[][] capabilities) {
		start(APPLICABILITY_SCOPE);
		for (int i = 0; i < capabilities.length; i++) {
			start(APPLY_ON);
			writeRequirements(Arrays.asList(capabilities[i]));
			end(APPLY_ON);
		}
		end(APPLICABILITY_SCOPE);
	}

	protected void writeRequirementsChange(List<IRequirementChange> changes) {
		start(REQUIREMENT_CHANGES);
		for (int i = 0; i < changes.size(); i++) {
			writeRequirementChange(changes.get(i));
		}
		end(REQUIREMENT_CHANGES);
	}

	protected void writeRequirementChange(IRequirementChange change) {
		start(REQUIREMENT_CHANGE);
		if (change.applyOn() != null) {
			start(REQUIREMENT_FROM);
			writeRequirement(change.applyOn());
			end(REQUIREMENT_FROM);
		}
		if (change.newValue() != null) {
			start(REQUIREMENT_TO);
			writeRequirement(change.newValue());
			end(REQUIREMENT_TO);
		}
		end(REQUIREMENT_CHANGE);
	}

	protected void writeRequirement(IRequirement requirement) {
		start(REQUIREMENT_ELEMENT);
		IMatchExpression<IInstallableUnit> match = requirement.getMatches();
		if (requirement.getMax() > 0 && RequiredCapability.isSimpleRequirement(match)) {
			attribute(NAMESPACE_ATTRIBUTE, RequiredCapability.extractNamespace(match));
			attribute(NAME_ATTRIBUTE, RequiredCapability.extractName(match));
			attribute(VERSION_RANGE_ATTRIBUTE, RequiredCapability.extractRange(match));
			attribute(CAPABILITY_OPTIONAL_ATTRIBUTE, requirement.getMin() == 0, false);
			attribute(CAPABILITY_MULTIPLE_ATTRIBUTE, requirement.getMax() > 1, false);
		} else {
			writeMatchExpression(match);
			if (requirement.getMin() != 1)
				attribute(MIN_ATTRIBUTE, requirement.getMin());
			if (requirement.getMax() != 1)
				attribute(MAX_ATTRIBUTE, requirement.getMax());
		}
		attribute(CAPABILITY_GREED_ATTRIBUTE, requirement.isGreedy(), true);
		if (requirement.getFilter() != null)
			writeTrimmedCdata(CAPABILITY_FILTER_ELEMENT, requirement.getFilter().getParameters()[0].toString());
		if (requirement.getDescription() != null)
			writeTrimmedCdata(REQUIREMENT_DESCRIPTION_ELEMENT, requirement.getDescription());
		end(REQUIREMENT_ELEMENT);
	}

	private void writeMatchExpression(IMatchExpression<IInstallableUnit> match) {
		attribute(MATCH_ATTRIBUTE, ExpressionUtil.getOperand(match));
		Object[] params = match.getParameters();
		if (params.length > 0) {
			IExpressionFactory factory = ExpressionUtil.getFactory();
			IExpression[] constantArray = new IExpression[params.length];
			for (int idx = 0; idx < params.length; ++idx)
				constantArray[idx] = factory.constant(params[idx]);
			attribute(MATCH_PARAMETERS_ATTRIBUTE, factory.array(constantArray));
		}
	}

	protected void writeArtifactKeys(Collection<IArtifactKey> artifactKeys) {
		if (artifactKeys != null && artifactKeys.size() > 0) {
			start(ARTIFACT_KEYS_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, artifactKeys.size());
			for (IArtifactKey artifactKey : artifactKeys) {
				start(ARTIFACT_KEY_ELEMENT);
				attribute(ARTIFACT_KEY_CLASSIFIER_ATTRIBUTE, artifactKey.getClassifier());
				attribute(ID_ATTRIBUTE, artifactKey.getId());
				attribute(VERSION_ATTRIBUTE, artifactKey.getVersion());
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

	protected void writeTouchpointData(Collection<ITouchpointData> touchpointData) {
		if (touchpointData != null && touchpointData.size() > 0) {
			start(TOUCHPOINT_DATA_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, touchpointData.size());
			for (ITouchpointData nextData : touchpointData) {
				Map<String, ITouchpointInstruction> instructions = nextData.getInstructions();
				if (instructions.size() > 0) {
					start(TOUCHPOINT_DATA_INSTRUCTIONS_ELEMENT);
					attribute(COLLECTION_SIZE_ATTRIBUTE, instructions.size());
					for (Map.Entry<String, ITouchpointInstruction> entry : instructions.entrySet()) {
						start(TOUCHPOINT_DATA_INSTRUCTION_ELEMENT);
						attribute(TOUCHPOINT_DATA_INSTRUCTION_KEY_ATTRIBUTE, entry.getKey());
						ITouchpointInstruction instruction = entry.getValue();
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

	private void writeLicenses(Collection<ILicense> licenses) {
		if (licenses != null && licenses.size() > 0) {
			// In the future there may be more than one license, so we write this 
			// as a collection of one.
			// See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=216911
			start(LICENSES_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, licenses.size());
			for (ILicense license : licenses) {
				if (license == null)
					continue;
				start(LICENSE_ELEMENT);
				if (license.getLocation() != null) {
					attribute(URI_ATTRIBUTE, license.getLocation().toString());

					try {
						// we write the URL attribute for backwards compatibility with 3.4.x
						// this attribute should be removed if we make a breaking format change.
						attribute(URL_ATTRIBUTE, URIUtil.toURL(license.getLocation()).toExternalForm());
					} catch (MalformedURLException e) {
						attribute(URL_ATTRIBUTE, license.getLocation().toString());
					}
				}
				cdata(license.getBody(), true);
				end(LICENSE_ELEMENT);
			}
			end(LICENSES_ELEMENT);
		}
	}

	private void writeCopyright(ICopyright copyright) {
		if (copyright != null) {
			start(COPYRIGHT_ELEMENT);
			try {
				if (copyright.getLocation() != null) {
					attribute(URI_ATTRIBUTE, copyright.getLocation().toString());
					try {
						// we write the URL attribute for backwards compatibility with 3.4.x
						// this attribute should be removed if we make a breaking format change.
						attribute(URL_ATTRIBUTE, URIUtil.toURL(copyright.getLocation()).toExternalForm());
					} catch (MalformedURLException e) {
						attribute(URL_ATTRIBUTE, copyright.getLocation().toString());
					}
				}
			} catch (IllegalStateException ise) {
				LogHelper.log(new Status(IStatus.INFO, Activator.ID, "Error writing the copyright URL: " + copyright.getLocation())); //$NON-NLS-1$
			}
			cdata(copyright.getBody(), true);
			end(COPYRIGHT_ELEMENT);
		}
	}
}
