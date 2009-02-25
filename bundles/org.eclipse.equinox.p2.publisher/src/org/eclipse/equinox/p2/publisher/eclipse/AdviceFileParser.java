/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;

public class AdviceFileParser {

	private Properties adviceProperties = new Properties();
	private List adviceProvides = new ArrayList();
	private List adviceRequires = new ArrayList();
	private Map adviceInstructions = new HashMap();
	private List adviceOtherIUs = new ArrayList();

	private final Map advice;
	private Iterator keysIterator;
	private String current;
	//	private String hostId; not currently used
	private Version hostVersion;

	public AdviceFileParser(String id, Version version, Map advice) {
		// this.hostId = id; not currently used
		this.hostVersion = version;
		this.advice = advice;
	}

	public void parse() {
		List keys = new ArrayList(advice.keySet());
		Collections.sort(keys);

		keysIterator = keys.iterator();
		next();

		while (current != null) {
			if (current.startsWith("properties."))
				parseProperties("properties.", adviceProperties);
			else if (current.startsWith("provides."))
				parseProvides("provides.", adviceProvides);
			else if (current.startsWith("requires."))
				parseRequires("requires.", adviceRequires);
			else if (current.startsWith("instructions."))
				parseInstructions("instructions.", adviceInstructions);
			else if (current.startsWith("units."))
				parseUnits("units.", adviceOtherIUs);
			else
				throw new IllegalStateException("bad token: " + current);
		}
	}

	private void next() {
		current = (String) (keysIterator.hasNext() ? keysIterator.next() : null);
	}

	private String currentValue() {
		return ((String) advice.get(current)).trim();
	}

	private void parseProperties(String prefix, Map properties) {
		while (current != null && current.startsWith(prefix)) {
			String propertyName = current.substring(prefix.length());
			if (propertyName.indexOf('.') != -1)
				throw new IllegalStateException();
			properties.put(propertyName, currentValue());
			next();
		}
	}

	private void parseProvides(String prefix, List provides) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current);

			parseProvided(current.substring(0, dotIndex + 1), provides);
		}
	}

	private void parseProvided(String prefix, List provides) {
		String namespace = null;
		String name = null;
		Version capabilityVersion = null;
		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals("name")) {
				name = currentValue();
			} else if (token.equals("namespace")) {
				namespace = currentValue();
			} else if (token.equals("version")) {
				capabilityVersion = new Version(substituteVersionAndQualifier(currentValue()));
			} else
				throw new IllegalStateException("bad token: " + current);
			next();
		}

		IProvidedCapability capability = MetadataFactory.createProvidedCapability(namespace, name, capabilityVersion);
		provides.add(capability);
	}

	private void parseRequires(String prefix, List requires) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current);

			parseRequired(current.substring(0, dotIndex + 1), requires);
		}
	}

	private void parseRequired(String prefix, List requires) {

		String namespace = null;
		String name = null;
		VersionRange range = null;
		String filter = null;
		boolean optional = false;
		boolean multiple = false;
		boolean greedy = false;

		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals("greedy")) {
				greedy = Boolean.valueOf(currentValue()).booleanValue();
			} else if (token.equals("optional")) {
				optional = Boolean.valueOf(currentValue()).booleanValue();
			} else if (token.equals("multiple")) {
				multiple = Boolean.valueOf(currentValue()).booleanValue();
			} else if (token.equals("filter")) {
				filter = currentValue();
			} else if (token.equals("name")) {
				name = currentValue();
			} else if (token.equals("namespace")) {
				namespace = currentValue();
			} else if (token.equals("range")) {
				range = new VersionRange(substituteVersionAndQualifier(currentValue()));
			} else
				throw new IllegalStateException("bad token: " + current);
			next();
		}
		IRequiredCapability capability = MetadataFactory.createRequiredCapability(namespace, name, range, filter, optional, multiple, greedy);
		requires.add(capability);
	}

	private void parseInstructions(String prefix, Map instructions) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex != -1)
				throw new IllegalStateException("bad token: " + current);

			parseInstruction(current, instructions);
		}
	}

	private void parseInstruction(String prefix, Map instructions) {
		String phase = current.substring(current.lastIndexOf('.') + 1);
		String body = currentValue();
		next();

		prefix += ".";
		String importAttribute = null;
		if (current != null && current.startsWith(prefix)) {
			if (current.substring(prefix.length()).equals("import")) {
				importAttribute = currentValue();
			} else
				throw new IllegalStateException("bad token: " + current);
			next();
		}
		ITouchpointInstruction instruction = MetadataFactory.createTouchpointInstruction(body, importAttribute);
		instructions.put(phase, instruction);
	}

	private void parseUnits(String prefix, List ius) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current);

			parseUnit(current.substring(0, dotIndex + 1), ius);
		}
	}

	private void parseUnit(String prefix, List units) {
		String unitId = null;
		Version unitVersion = null;
		boolean unitSingleton = false;
		String unitFilter = null;
		String unitCopyright = null;
		String unitCopyrightLocation = null;
		String unitTouchpointId = null;
		Version unitTouchpointVersion = null;

		List unitArtifacts = new ArrayList();
		Properties unitProperties = new Properties();
		List unitHostRequirements = new ArrayList();
		List unitProvides = new ArrayList();
		List unitRequires = new ArrayList();
		List unitLicenses = new ArrayList();
		Map unitInstructions = new HashMap();
		//		updatedescriptor ??

		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals("id")) {
				unitId = currentValue();
				next();
			} else if (token.equals("version")) {
				unitVersion = new Version(substituteVersionAndQualifier(currentValue()));
				next();
			} else if (token.equals("filter")) {
				unitFilter = currentValue();
				next();
			} else if (token.equals("copyright")) {
				unitCopyright = currentValue();
				next();
			} else if (token.equals("copyright.location")) {
				unitCopyrightLocation = currentValue();
				next();
			} else if (token.equals("touchpoint.id")) {
				unitTouchpointId = currentValue();
				next();
			} else if (token.equals("touchpoinit.version")) {
				unitTouchpointVersion = new Version(substituteVersionAndQualifier(currentValue()));
				next();
			} else if (token.startsWith("hostRequirements."))
				parseHostRequirements(prefix + "hostRequirements.", unitHostRequirements);
			else if (token.startsWith("artifacts."))
				parseArtifacts(prefix + "artifacts.", unitArtifacts);
			else if (token.startsWith("licenses."))
				parseLicenses(prefix + "licenses.", unitLicenses);
			else if (token.startsWith("properties."))
				parseProperties(prefix + "properties.", unitProperties);
			else if (token.startsWith("provides."))
				parseProvides(prefix + "provides.", unitProvides);
			else if (token.startsWith("requires."))
				parseRequires(prefix + "requires.", unitRequires);
			else if (token.startsWith("instructions."))
				parseInstructions(prefix + "instructions.", unitInstructions);
			else
				throw new IllegalStateException("bad token: " + current);
		}

		InstallableUnitDescription description = unitHostRequirements.isEmpty() ? new InstallableUnitDescription() : new InstallableUnitFragmentDescription();
		description.setId(unitId);
		description.setVersion(unitVersion);
		description.setSingleton(unitSingleton);
		description.setFilter(unitFilter);
		if (unitCopyright != null || unitCopyrightLocation != null) {
			try {
				description.setCopyright(MetadataFactory.createCopyright(new URI(unitCopyrightLocation), unitCopyright));
			} catch (URISyntaxException e) {
				throw new IllegalStateException();
			}
		}
		if (unitTouchpointId != null)
			description.setTouchpointType(MetadataFactory.createTouchpointType(unitTouchpointId, unitTouchpointVersion));
		if (!unitLicenses.isEmpty())
			description.setLicense((ILicense) unitLicenses.get(0));

		if (!unitArtifacts.isEmpty())
			description.setArtifacts((IArtifactKey[]) unitArtifacts.toArray(new IArtifactKey[unitArtifacts.size()]));

		if (!unitHostRequirements.isEmpty())
			((InstallableUnitFragmentDescription) description).setHost((IRequiredCapability[]) unitHostRequirements.toArray(new IRequiredCapability[unitHostRequirements.size()]));

		if (!unitProperties.isEmpty()) {
			for (Iterator iterator = unitProperties.entrySet().iterator(); iterator.hasNext();) {
				Entry entry = (Entry) iterator.next();
				description.setProperty((String) entry.getKey(), (String) entry.getValue());
			}
		}

		if (!unitProvides.isEmpty())
			description.setCapabilities((IProvidedCapability[]) unitProvides.toArray(new IProvidedCapability[unitProvides.size()]));

		if (!unitRequires.isEmpty())
			description.setRequiredCapabilities((IRequiredCapability[]) unitRequires.toArray(new IRequiredCapability[unitRequires.size()]));

		if (!unitInstructions.isEmpty())
			description.addTouchpointData(MetadataFactory.createTouchpointData(unitInstructions));

		adviceOtherIUs.add(description);
	}

	private void parseLicenses(String prefix, List licenses) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current);

			parseLicense(current.substring(0, dotIndex + 1), licenses);
		}
	}

	private void parseLicense(String prefix, List licenses) {
		String body = currentValue();
		next();

		prefix += ".";
		String location = null;
		if (current != null && current.startsWith(prefix)) {
			if (current.substring(prefix.length()).equals("import")) {
				location = currentValue();
			} else
				throw new IllegalStateException("bad token: " + current);
			next();
		}

		try {
			ILicense license = MetadataFactory.createLicense(new URI(location), body);
			licenses.add(license);
		} catch (URISyntaxException e) {
			throw new IllegalStateException();
		}
	}

	private void parseArtifacts(String prefix, List artifacts) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current);

			parseArtifact(current.substring(0, dotIndex + 1), artifacts);
		}
	}

	private void parseArtifact(String prefix, List artifacts) {
		String artifactClassifier = null;
		String artifactId = null;
		Version artifactVersion = null;
		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals("classifier")) {
				artifactClassifier = currentValue();
			} else if (token.equals("id")) {
				artifactId = currentValue();
			} else if (token.equals("version")) {
				artifactVersion = new Version(substituteVersionAndQualifier(currentValue()));
			} else
				throw new IllegalStateException("bad token: " + current);
			next();
		}
		IArtifactKey artifactKey = new ArtifactKey(artifactClassifier, artifactId, artifactVersion);
		artifacts.add(artifactKey);
	}

	private void parseHostRequirements(String prefix, List hostRequirements) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current);

			parseRequired(current.substring(0, dotIndex + 1), hostRequirements);
		}
	}

	private String substituteVersionAndQualifier(String version) {
		if (version.indexOf("$version$") != -1) {
			version = version.replaceAll("\\$version\\$", hostVersion.toString());
		}

		if (version.indexOf("$qualifier$") != -1) {
			String qualifier = hostVersion.getQualifier();
			if (qualifier == null)
				qualifier = "";
			if (qualifier.length() == 0) {
				// Note: this works only for OSGi versions and version ranges
				// where the qualifier if present must be at the end of a version string
				version = version.replaceAll(".\\$qualifier\\$", "");
			}

			version = version.replaceAll("\\$qualifier\\$", qualifier);
		}
		return version;
	}

	public Properties getProperties() {
		if (adviceProperties.isEmpty())
			return null;
		return adviceProperties;
	}

	public IRequiredCapability[] getRequiredCapabilities() {
		if (adviceRequires.isEmpty())
			return null;

		return (IRequiredCapability[]) adviceRequires.toArray(new IRequiredCapability[adviceRequires.size()]);
	}

	public IProvidedCapability[] getProvidedCapabilities() {
		if (adviceProvides.isEmpty())
			return null;

		return (IProvidedCapability[]) adviceProvides.toArray(new IProvidedCapability[adviceProvides.size()]);
	}

	public Map getTouchpointInstructions() {
		if (adviceInstructions.isEmpty())
			return null;

		return adviceInstructions;
	}

	public InstallableUnitDescription[] getOtherInstallableUnitDescriptions() {
		if (adviceOtherIUs.isEmpty())
			return null;

		return (InstallableUnitDescription[]) adviceOtherIUs.toArray(new InstallableUnitDescription[adviceOtherIUs.size()]);
	}
}