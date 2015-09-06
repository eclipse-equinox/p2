/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class AdviceFileParser {

	private static final String ADVICE_VERSION = "advice.version"; //$NON-NLS-1$

	private static final String QUALIFIER_SUBSTITUTION = "$qualifier$"; //$NON-NLS-1$
	private static final String VERSION_SUBSTITUTION = "$version$"; //$NON-NLS-1$

	private static final String UPDATE_DESCRIPTION = "update.description"; //$NON-NLS-1$
	private static final String UPDATE_SEVERITY = "update.severity"; //$NON-NLS-1$
	private static final String UPDATE_RANGE = "update.range"; //$NON-NLS-1$
	private static final String UPDATE_ID = "update.id"; //$NON-NLS-1$
	private static final String UPDATE_MATCH_EXP = "update.matchExp"; //$NON-NLS-1$
	private static final String CLASSIFIER = "classifier"; //$NON-NLS-1$
	private static final String TOUCHPOINT_VERSION = "touchpoint.version"; //$NON-NLS-1$
	private static final String TOUCHPOINT_ID = "touchpoint.id"; //$NON-NLS-1$
	private static final String COPYRIGHT_LOCATION = "copyright.location"; //$NON-NLS-1$
	private static final String COPYRIGHT = "copyright"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$
	private static final String SINGLETON = "singleton"; //$NON-NLS-1$
	private static final String IMPORT = "import"; //$NON-NLS-1$
	private static final String RANGE = "range"; //$NON-NLS-1$
	private static final String MIN = "min"; //$NON-NLS-1$
	private static final String MAX = "max"; //$NON-NLS-1$
	private static final String FILTER = "filter"; //$NON-NLS-1$
	private static final String MULTIPLE = "multiple"; //$NON-NLS-1$
	private static final String OPTIONAL = "optional"; //$NON-NLS-1$
	private static final String GREEDY = "greedy"; //$NON-NLS-1$
	private static final String VERSION = "version"; //$NON-NLS-1$
	private static final String NAMESPACE = "namespace"; //$NON-NLS-1$
	private static final String NAME = "name"; //$NON-NLS-1$
	private static final String MATCH_EXP = "matchExp"; //$NON-NLS-1$
	private static final String LOCATION = "location"; //$NON-NLS-1$
	private static final String VALUE = "value"; //$NON-NLS-1$

	private static final String UNITS_PREFIX = "units."; //$NON-NLS-1$
	private static final String INSTRUCTIONS_PREFIX = "instructions."; //$NON-NLS-1$
	private static final String REQUIRES_PREFIX = "requires."; //$NON-NLS-1$
	private static final String META_REQUIREMENTS_PREFIX = "metaRequirements."; //$NON-NLS-1$
	private static final String PROVIDES_PREFIX = "provides."; //$NON-NLS-1$
	private static final String PROPERTIES_PREFIX = "properties."; //$NON-NLS-1$
	private static final String LICENSES_PREFIX = "licenses."; //$NON-NLS-1$
	private static final String ARTIFACTS_PREFIX = "artifacts."; //$NON-NLS-1$
	private static final String HOST_REQUIREMENTS_PREFIX = "hostRequirements."; //$NON-NLS-1$
	private static final String UPDATE_DESCRIPTOR_PREFIX = "update."; //$NON-NLS-1$

	public static final Version COMPATIBLE_VERSION = Version.createOSGi(1, 0, 0);
	public static final VersionRange VERSION_TOLERANCE = new VersionRange(COMPATIBLE_VERSION, true, Version.createOSGi(2, 0, 0), false);

	private Map<String, String> adviceProperties = new HashMap<String, String>();
	private List<IProvidedCapability> adviceProvides = new ArrayList<IProvidedCapability>();
	private List<IRequirement> adviceRequires = new ArrayList<IRequirement>();
	private List<IRequirement> adviceMetaRequires = new ArrayList<IRequirement>();
	private IUpdateDescriptor adviceUpdateDescriptor = null;
	private Map<String, ITouchpointInstruction> adviceInstructions = new HashMap<String, ITouchpointInstruction>();
	private List<InstallableUnitDescription> adviceOtherIUs = new ArrayList<InstallableUnitDescription>();

	private final Map<String, String> advice;
	private Iterator<String> keysIterator;
	private String current;
	private String hostId;
	private Version hostVersion;

	public AdviceFileParser(String id, Version version, Map<String, String> advice) {
		this.hostId = id;
		this.hostVersion = version;
		this.advice = advice;
	}

	public void parse() {
		String adviceVersion = advice.get(ADVICE_VERSION);
		if (adviceVersion != null)
			checkAdviceVersion(adviceVersion);

		List<String> keys = new ArrayList<String>(advice.keySet());
		Collections.sort(keys);

		keysIterator = keys.iterator();
		next();

		while (current != null) {
			if (current.startsWith(PROPERTIES_PREFIX))
				parseProperties(PROPERTIES_PREFIX, adviceProperties);
			else if (current.startsWith(UPDATE_DESCRIPTOR_PREFIX))
				this.adviceUpdateDescriptor = parseUpdateDescriptor(UPDATE_DESCRIPTOR_PREFIX, hostId);
			else if (current.startsWith(PROVIDES_PREFIX))
				parseProvides(PROVIDES_PREFIX, adviceProvides);
			else if (current.startsWith(REQUIRES_PREFIX))
				parseRequires(REQUIRES_PREFIX, adviceRequires);
			else if (current.startsWith(META_REQUIREMENTS_PREFIX))
				parseRequires(META_REQUIREMENTS_PREFIX, adviceMetaRequires);
			else if (current.startsWith(INSTRUCTIONS_PREFIX))
				parseInstructions(INSTRUCTIONS_PREFIX, adviceInstructions);
			else if (current.startsWith(UNITS_PREFIX))
				parseUnits(UNITS_PREFIX, adviceOtherIUs);
			else if (current.equals(ADVICE_VERSION)) {
				next();
			} else {
				// we ignore elements we do not understand
				next();
			}
		}
	}

	private void checkAdviceVersion(String adviceVersion) {
		Version version = Version.parseVersion(adviceVersion);
		if (!VERSION_TOLERANCE.isIncluded(version))
			throw new IllegalStateException("bad version: " + version + ". Expected range was " + VERSION_TOLERANCE); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void next() {
		current = keysIterator.hasNext() ? keysIterator.next() : null;
	}

	private String currentValue() {
		return advice.get(current).trim();
	}

	private void parseProperties(String prefix, Map<String, String> properties) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current); //$NON-NLS-1$

			parseProperty(current.substring(0, dotIndex + 1), properties);
		}
	}

	private void parseProperty(String prefix, Map<String, String> properties) {
		String propertyName = null;
		String propertyValue = null;
		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals(NAME)) {
				propertyName = currentValue();
			} else if (token.equals(VALUE)) {
				propertyValue = currentValue();
			} else {
				// we ignore elements we do not understand
			}
			next();
		}

		properties.put(propertyName, propertyValue);
	}

	private IUpdateDescriptor parseUpdateDescriptor(String prefix, String id) {
		String name = id;
		String description = null;
		String range = "[0.0.0,$version$)"; //$NON-NLS-1$ 
		String severity = "0"; //$NON-NLS-1$
		String match = null;

		while (current != null && current.startsWith(prefix)) {
			String token = current;
			if (token.equals(UPDATE_MATCH_EXP)) {
				match = currentValue();
			} else if (token.equals(UPDATE_ID)) {
				name = currentValue();
			} else if (token.equals(UPDATE_DESCRIPTION)) {
				description = currentValue();
			} else if (token.equals(UPDATE_RANGE)) {
				range = currentValue();
			} else if (token.equals(UPDATE_SEVERITY)) {
				severity = currentValue();
			} else {
				// ignore
			}
			next();
		}

		if (match != null) {
			//When update.match is specified, versionRange and id are ignored
			IExpression expr = ExpressionUtil.parse(substituteVersionAndQualifier(match));
			IMatchExpression<IInstallableUnit> matchExpression = ExpressionUtil.getFactory().matchExpression(expr);
			Collection<IMatchExpression<IInstallableUnit>> descriptors = new ArrayList<IMatchExpression<IInstallableUnit>>(1);
			descriptors.add(matchExpression);
			return MetadataFactory.createUpdateDescriptor(descriptors, Integer.valueOf(severity), description, (URI) null);
		}
		range = substituteVersionAndQualifier(range);
		VersionRange versionRange = new VersionRange(range);
		return MetadataFactory.createUpdateDescriptor(name, versionRange, Integer.valueOf(severity), description);
	}

	private void parseProvides(String prefix, List<IProvidedCapability> provides) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current); //$NON-NLS-1$

			parseProvided(current.substring(0, dotIndex + 1), provides);
		}
	}

	private void parseProvided(String prefix, List<IProvidedCapability> provides) {
		String namespace = null;
		String name = null;
		Version capabilityVersion = null;
		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals(NAME)) {
				name = currentValue();
			} else if (token.equals(NAMESPACE)) {
				namespace = currentValue();
			} else if (token.equals(VERSION)) {
				capabilityVersion = Version.parseVersion(substituteVersionAndQualifier(currentValue()));
			} else {
				// we ignore elements we do not understand
			}
			next();
		}

		IProvidedCapability capability = MetadataFactory.createProvidedCapability(namespace, name, capabilityVersion);
		provides.add(capability);
	}

	private void parseRequires(String prefix, List<IRequirement> requires) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current); //$NON-NLS-1$

			parseRequired(current.substring(0, dotIndex + 1), requires);
		}
	}

	private void parseRequired(String prefix, List<IRequirement> requires) {

		String namespace = null;
		String name = null;
		VersionRange range = null;
		String matchExp = null;
		String filter = null;
		boolean optional = false;
		boolean multiple = false;
		boolean greedy = true;

		int min = -1;
		int max = -1;

		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals(GREEDY)) {
				greedy = Boolean.parseBoolean(currentValue());
			} else if (token.equals(OPTIONAL)) {
				optional = Boolean.parseBoolean(currentValue());
			} else if (token.equals(MULTIPLE)) {
				multiple = Boolean.parseBoolean(currentValue());
			} else if (token.equals(FILTER)) {
				filter = currentValue();
			} else if (token.equals(NAME)) {
				name = currentValue();
			} else if (token.equals(NAMESPACE)) {
				namespace = currentValue();
			} else if (token.equals(RANGE)) {
				range = new VersionRange(substituteVersionAndQualifier(currentValue()));
			} else if (token.equals(MIN)) {
				min = Integer.valueOf(currentValue()).intValue();
			} else if (token.equals(MAX)) {
				max = Integer.valueOf(currentValue()).intValue();
			} else if (token.equals(MATCH_EXP)) {
				matchExp = currentValue();
			} else {
				// we ignore elements we do not understand
			}
			next();
		}
		IRequirement capability = null;
		if (matchExp == null) {
			if (min >= 0 && max >= 0) {
				capability = createRequirement(namespace, name, range, filter, min, max, greedy);
			} else {
				capability = createRequirement(namespace, name, range, filter, optional, multiple, greedy);
			}
		} else {
			//When a match expression is specified, namespace, name and versionRange are ignored
			if (optional && min == -1 && max == -1) {
				min = 0;
				max = 1;
			}
			capability = createRequirement(matchExp, filter, min, max, greedy, null);
		}
		if (capability != null) {
			requires.add(capability);
		}
	}

	protected IRequirement createRequirement(String requirement, String filter, int min, int max, boolean greedy, String description) {
		IExpression expr = ExpressionUtil.parse(substituteVersionAndQualifier(requirement));
		IMatchExpression<IInstallableUnit> requirementExp = ExpressionUtil.getFactory().matchExpression(expr);
		IMatchExpression<IInstallableUnit> filterExp = InstallableUnit.parseFilter(filter);
		return MetadataFactory.createRequirement(requirementExp, filterExp, min, max, greedy, description);
	}

	protected IRequirement createRequirement(String namespace, String name, VersionRange range, String filter, int min, int max, boolean greedy) {
		IMatchExpression<IInstallableUnit> filterExpression = InstallableUnit.parseFilter(filter);
		return MetadataFactory.createRequirement(namespace, name, range, filterExpression, min, max, greedy);
	}

	protected IRequirement createRequirement(String namespace, String name, VersionRange range, String filter, boolean optional, boolean multiple, boolean greedy) {
		return MetadataFactory.createRequirement(namespace, name, range, filter, optional, multiple, greedy);
	}

	private void parseInstructions(String prefix, Map<String, ITouchpointInstruction> instructions) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex != -1)
				throw new IllegalStateException("bad token: " + current); //$NON-NLS-1$

			parseInstruction(current, instructions);
		}
	}

	private void parseInstruction(String prefix, Map<String, ITouchpointInstruction> instructions) {
		String phase = current.substring(current.lastIndexOf('.') + 1);
		String body = currentValue();
		next();

		prefix += '.';
		String importAttribute = null;
		if (current != null && current.startsWith(prefix)) {
			if (current.substring(prefix.length()).equals(IMPORT)) {
				importAttribute = currentValue();
			} else {
				// we ignore elements we do not understand
			}
			next();
		}
		ITouchpointInstruction instruction = MetadataFactory.createTouchpointInstruction(body, importAttribute);
		instructions.put(phase, instruction);
	}

	private void parseUnits(String prefix, List<InstallableUnitDescription> ius) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current + " = " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$

			parseUnit(current.substring(0, dotIndex + 1), ius);
		}
	}

	private void parseUnit(String prefix, List<InstallableUnitDescription> units) {
		String unitId = null;
		Version unitVersion = null;
		boolean unitSingleton = false;
		String unitFilter = null;
		String unitCopyright = null;
		String unitCopyrightLocation = null;
		String unitTouchpointId = null;
		Version unitTouchpointVersion = null;

		String unitUpdateId = null;
		VersionRange unitUpdateRange = null;
		int unitUpdateSeverity = 0;
		String unitUpdateDescription = null;

		List<IArtifactKey> unitArtifacts = new ArrayList<IArtifactKey>();
		Map<String, String> unitProperties = new HashMap<String, String>();
		List<IRequirement> unitHostRequirements = new ArrayList<IRequirement>();
		List<IProvidedCapability> unitProvides = new ArrayList<IProvidedCapability>();
		List<IRequirement> unitRequires = new ArrayList<IRequirement>();
		List<IRequirement> unitMetaRequirements = new ArrayList<IRequirement>();
		List<ILicense> unitLicenses = new ArrayList<ILicense>();
		Map<String, ITouchpointInstruction> unitInstructions = new HashMap<String, ITouchpointInstruction>();
		//		updatedescriptor ??

		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals(ID)) {
				unitId = currentValue();
				next();
			} else if (token.equals(VERSION)) {
				unitVersion = Version.parseVersion(substituteVersionAndQualifier(currentValue()));
				next();
			} else if (token.equals(SINGLETON)) {
				unitSingleton = Boolean.parseBoolean(currentValue());
				next();
			} else if (token.equals(FILTER)) {
				unitFilter = currentValue();
				next();
			} else if (token.equals(COPYRIGHT)) {
				unitCopyright = currentValue();
				next();
			} else if (token.equals(COPYRIGHT_LOCATION)) {
				unitCopyrightLocation = currentValue();
				next();
			} else if (token.equals(TOUCHPOINT_ID)) {
				unitTouchpointId = currentValue();
				next();
			} else if (token.equals(TOUCHPOINT_VERSION)) {
				unitTouchpointVersion = Version.parseVersion(substituteVersionAndQualifier(currentValue()));
				next();
			} else if (token.equals(UPDATE_ID)) {
				unitUpdateId = currentValue();
				next();
			} else if (token.equals(UPDATE_RANGE)) {
				unitUpdateRange = new VersionRange(substituteVersionAndQualifier(currentValue()));
				next();
			} else if (token.equals(UPDATE_SEVERITY)) {
				unitUpdateSeverity = Integer.parseInt(currentValue());
				next();
			} else if (token.equals(UPDATE_DESCRIPTION)) {
				unitUpdateDescription = currentValue();
				next();
			} else if (token.startsWith(HOST_REQUIREMENTS_PREFIX))
				parseRequires(prefix + HOST_REQUIREMENTS_PREFIX, unitHostRequirements);
			else if (token.startsWith(ARTIFACTS_PREFIX))
				parseArtifacts(prefix + ARTIFACTS_PREFIX, unitArtifacts);
			else if (token.startsWith(LICENSES_PREFIX))
				parseLicenses(prefix + LICENSES_PREFIX, unitLicenses);
			else if (token.startsWith(PROPERTIES_PREFIX))
				parseProperties(prefix + PROPERTIES_PREFIX, unitProperties);
			else if (token.startsWith(PROVIDES_PREFIX))
				parseProvides(prefix + PROVIDES_PREFIX, unitProvides);
			else if (token.startsWith(REQUIRES_PREFIX))
				parseRequires(prefix + REQUIRES_PREFIX, unitRequires);
			else if (token.startsWith(META_REQUIREMENTS_PREFIX))
				parseRequires(prefix + META_REQUIREMENTS_PREFIX, unitMetaRequirements);
			else if (token.startsWith(INSTRUCTIONS_PREFIX))
				parseInstructions(prefix + INSTRUCTIONS_PREFIX, unitInstructions);
			else {
				// we ignore elements we do not understand
				next();
			}
		}

		InstallableUnitDescription description = unitHostRequirements.isEmpty() ? new InstallableUnitDescription() : new InstallableUnitFragmentDescription();
		description.setId(unitId);
		description.setVersion(unitVersion);
		description.setSingleton(unitSingleton);
		description.setFilter(unitFilter);
		if (unitCopyright != null || unitCopyrightLocation != null) {
			try {
				URI uri = unitCopyrightLocation != null ? new URI(unitCopyrightLocation) : null;
				description.setCopyright(MetadataFactory.createCopyright(uri, unitCopyright));
			} catch (URISyntaxException e) {
				throw new IllegalStateException("bad copyright URI at token: " + current + ", " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (unitTouchpointId != null)
			description.setTouchpointType(MetadataFactory.createTouchpointType(unitTouchpointId, unitTouchpointVersion));

		if (unitUpdateId != null)
			description.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(unitUpdateId, unitUpdateRange, unitUpdateSeverity, unitUpdateDescription));

		if (!unitLicenses.isEmpty())
			description.setLicenses(unitLicenses.toArray(new ILicense[unitLicenses.size()]));

		if (!unitArtifacts.isEmpty())
			description.setArtifacts(unitArtifacts.toArray(new IArtifactKey[unitArtifacts.size()]));

		if (!unitHostRequirements.isEmpty())
			((InstallableUnitFragmentDescription) description).setHost(unitHostRequirements.toArray(new IRequirement[unitHostRequirements.size()]));

		if (!unitProperties.isEmpty()) {
			for (Entry<String, String> entry : unitProperties.entrySet()) {
				description.setProperty(entry.getKey(), entry.getValue());
			}
		}

		if (!unitProvides.isEmpty())
			description.setCapabilities(unitProvides.toArray(new IProvidedCapability[unitProvides.size()]));

		if (!unitRequires.isEmpty())
			description.setRequirements(unitRequires.toArray(new IRequirement[unitRequires.size()]));

		if (!unitMetaRequirements.isEmpty())
			description.setMetaRequirements(unitMetaRequirements.toArray(new IRequirement[unitMetaRequirements.size()]));

		if (!unitInstructions.isEmpty())
			description.addTouchpointData(MetadataFactory.createTouchpointData(unitInstructions));

		adviceOtherIUs.add(description);
	}

	private void parseLicenses(String prefix, List<ILicense> licenses) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex != -1)
				throw new IllegalStateException("bad token: " + current + " = " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$

			parseLicense(current, licenses);
		}
	}

	private void parseLicense(String prefix, List<ILicense> licenses) {
		String body = currentValue();
		next();

		prefix += '.';
		String location = null;
		if (current != null && current.startsWith(prefix)) {
			if (current.substring(prefix.length()).equals(LOCATION)) {
				location = currentValue();
			} else {
				// we ignore elements we do not understand
			}
			next();
		}

		try {
			URI uri = location != null ? new URI(location) : null;
			ILicense license = MetadataFactory.createLicense(uri, body);
			licenses.add(license);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("bad license URI at token: " + current + ", " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void parseArtifacts(String prefix, List<IArtifactKey> artifacts) {
		while (current != null && current.startsWith(prefix)) {
			int dotIndex = current.indexOf('.', prefix.length());
			if (dotIndex == -1)
				throw new IllegalStateException("bad token: " + current + " = " + currentValue()); //$NON-NLS-1$ //$NON-NLS-2$

			parseArtifact(current.substring(0, dotIndex + 1), artifacts);
		}
	}

	private void parseArtifact(String prefix, List<IArtifactKey> artifacts) {
		String artifactClassifier = null;
		String artifactId = null;
		Version artifactVersion = null;
		while (current != null && current.startsWith(prefix)) {
			String token = current.substring(prefix.length());
			if (token.equals(CLASSIFIER)) {
				artifactClassifier = currentValue();
			} else if (token.equals(ID)) {
				artifactId = currentValue();
			} else if (token.equals(VERSION)) {
				artifactVersion = Version.parseVersion(substituteVersionAndQualifier(currentValue()));
			} else {
				// we ignore elements we do not understand
			}

			next();
		}
		IArtifactKey artifactKey = new ArtifactKey(artifactClassifier, artifactId, artifactVersion);
		artifacts.add(artifactKey);
	}

	private String substituteVersionAndQualifier(String version) {
		if (version.indexOf(VERSION_SUBSTITUTION) != -1) {
			version = replace(version, VERSION_SUBSTITUTION, hostVersion.toString());
		}

		if (version.indexOf(QUALIFIER_SUBSTITUTION) != -1) {
			try {
				String qualifier = PublisherHelper.toOSGiVersion(hostVersion).getQualifier();
				if (qualifier == null)
					qualifier = ""; //$NON-NLS-1$
				if (qualifier.length() == 0) {
					// Note: this works only for OSGi versions and version ranges
					// where the qualifier if present must be at the end of a version string
					version = replace(version, "." + QUALIFIER_SUBSTITUTION, ""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				version = replace(version, QUALIFIER_SUBSTITUTION, qualifier);
			} catch (UnsupportedOperationException e) {
				// Version cannot be converted to OSGi
			}
		}
		return version;
	}

	// originally from org.eclipse.core.internal.net.StringUtil
	public static String replace(String source, String from, String to) {
		if (from.length() == 0)
			return source;
		StringBuffer buffer = new StringBuffer();
		int current = 0;
		int pos = 0;
		while (pos != -1) {
			pos = source.indexOf(from, current);
			if (pos == -1) {
				buffer.append(source.substring(current));
			} else {
				buffer.append(source.substring(current, pos));
				buffer.append(to);
				current = pos + from.length();
			}
		}
		return buffer.toString();
	}

	public Map<String, String> getProperties() {
		if (adviceProperties.isEmpty())
			return null;
		return adviceProperties;
	}

	public IRequirement[] getRequiredCapabilities() {
		if (adviceRequires.isEmpty())
			return null;

		return adviceRequires.toArray(new IRequirement[adviceRequires.size()]);
	}

	public IProvidedCapability[] getProvidedCapabilities() {
		if (adviceProvides.isEmpty())
			return null;

		return adviceProvides.toArray(new IProvidedCapability[adviceProvides.size()]);
	}

	public IUpdateDescriptor getUpdateDescriptor() {
		return adviceUpdateDescriptor;
	}

	public Map<String, ITouchpointInstruction> getTouchpointInstructions() {
		if (adviceInstructions.isEmpty())
			return null;

		return adviceInstructions;
	}

	public InstallableUnitDescription[] getAdditionalInstallableUnitDescriptions() {
		if (adviceOtherIUs.isEmpty())
			return null;

		return adviceOtherIUs.toArray(new InstallableUnitDescription[adviceOtherIUs.size()]);
	}

	public IRequirement[] getMetaRequiredCapabilities() {
		if (adviceMetaRequires.isEmpty())
			return null;

		return adviceMetaRequires.toArray(new IRequirement[adviceMetaRequires.size()]);
	}
}
