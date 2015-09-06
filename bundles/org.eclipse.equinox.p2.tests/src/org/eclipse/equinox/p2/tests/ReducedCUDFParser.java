/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 * Sonatype Inc. - trim down for annotation work
 ******************************************************************************/

package org.eclipse.equinox.p2.tests;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;

public class ReducedCUDFParser {

	private static final boolean DEBUG = false; //TO SET TO FALSE FOR COMPETITION
	private static final boolean TIMING = true; //TO SET TO FALSE FOR COMPETITION
	private InstallableUnitDescription currentIU = null;
	//	private ProfileChangeRequest currentRequest = null;
	private List allIUs = new ArrayList();
	//	private QueryableArray query = null;
	private List currentKeepRequests = new ArrayList();

	class Tuple {
		String name;
		String version;
		String operator;
		Set extraData;

		Tuple(String line) {
			String[] tuple = new String[3];
			int i = 0;
			for (StringTokenizer iter = new StringTokenizer(line, " \t"); iter.hasMoreTokens(); i++)
				tuple[i] = iter.nextToken().trim();
			name = tuple[0];
			operator = tuple[1];
			version = tuple[2];
		}
	}

	//
	//	public ProfileChangeRequest parse(File file) {
	//		return parse(file, false, null);
	//	}
	//
	//	public ProfileChangeRequest parse(File file, boolean includeRecommends, String sumProperty) {
	//		try {
	//			return parse(new FileInputStream(file), includeRecommends, sumProperty);
	//		} catch (FileNotFoundException e) {
	//			e.printStackTrace();
	//			return null;
	//		}
	//	}
	//
	//	public ProfileChangeRequest parse(InputStream stream) {
	//		return parse(stream, false, null);
	//	}
	//
	//	public ProfileChangeRequest parse(InputStream stream, String sumProperty) {
	//		return parse(stream, false, sumProperty);
	//	}

	public void parse(InputStream stream, boolean includeRecommends, String sumProperty) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			String next = reader.readLine();
			while (true) {

				// look-ahead to check for line continuation
				String line = next;
				for (next = reader.readLine(); next != null && next.length() > 1 && next.charAt(0) == ' '; next = reader.readLine()) {
					line = line + next.substring(1);
				}

				// terminating condition of the loop... reached the end of the file
				if (line == null) {
					validateAndAddIU();
					break;
				}

				// end of stanza
				if (line.trim().length() == 0) {
					validateAndAddIU();
					continue;
				}

				// preamble stanza
				if (line.startsWith("#") || line.startsWith("preamble: ") || line.startsWith("property: ") || line.startsWith("univ-checksum: ")) {
					// ignore
				}

				//				// request stanza
				//				else if (line.startsWith("request: ")) {
				//					handleRequest(line);
				//				} else if (line.startsWith("install: ")) {
				//					handleInstall(line);
				//				} else if (line.startsWith("upgrade: ")) {
				//					handleUpgrade(line);
				//				} else if (line.startsWith("remove: ")) {
				//					handleRemove(line);
				//				}

				// package stanza
				else if (line.startsWith("package: ")) {
					handlePackage(line);
				} else if (line.startsWith("version: ")) {
					handleVersion(line);
					//				} else if (line.startsWith("installed: ")) {
					//					handleInstalled(line);
				} else if (line.startsWith("depends: ")) {
					handleDepends(line);
					//				} else if (line.startsWith("conflicts: ")) {
					//					handleConflicts(line);
				} else if (line.startsWith("provides: ")) {
					handleProvides(line);
				} else if (line.startsWith("singleton:")) {
					handleSingleton(line);
				}
				//				} else if (line.startsWith("expected: ")) {
				//					handleExpected(line);
				//				} else if (line.startsWith("recommends: ") && includeRecommends) {
				//					handleRecommends(line);
				//				} else if (line.startsWith("keep: ")) {
				//					handleKeep(line);
				//				} else if (sumProperty != null && line.startsWith(sumProperty + ":")) {
				//					handleSumProperty(line, sumProperty);
				//				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
		}
		if (TIMING)
			//			Log.println("Time to parse:" + (System.currentTimeMillis() - start));
			if (DEBUG)
				for (Iterator iter = allIUs.iterator(); iter.hasNext();)
					debug((InstallableUnit) iter.next());
		//		if (FORCE_QUERY) {
		//			if (query == null)
		//				initializeQueryableArray();
		//			if (currentRequest == null)
		//				currentRequest = new ProfileChangeRequest(query);
		//		}
		//		debug(currentRequest);
		//		return currentRequest;
	}

	//	private void handleSumProperty(String line, String sumProperty) {
	//		String value = line.substring(sumProperty.length() + 1).trim();
	//		try {
	//			currentIU.setSumProperty(Long.valueOf(value));
	//		} catch (NumberFormatException ex) {
	//			throw new IllegalArgumentException("The value \"" + value + "\" of property \"" + sumProperty + "\" cannot be summed up");
	//		}
	//	}

	//	private void handleKeep(String line) {
	//		line = line.substring("keep: ".length());
	//		if (line.contains("version")) {
	//			currentKeepRequests.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, currentIU.getId(), new VersionRange(currentIU.getVersion(), true, currentIU.getVersion(), true), null, false, false, true));
	//			return;
	//		}
	//		if (line.contains("package")) {
	//			currentKeepRequests.add(new RequiredCapability(currentIU.getId(), VersionRange.emptyRange, false));
	//			return;
	//		}
	//		if (line.contains("none"))
	//			return;
	//		if (line.contains("feature")) {
	//			IProvidedCapability[] caps = currentIU.getProvidedCapabilities();
	//			for (int i = 0; i < caps.length; i++) {
	//				if (!caps[i].getName().equals(currentIU.getId()))
	//					currentKeepRequests.add(new RequiredCapability(caps[i].getName(), caps[i].getVersion(), false));
	//			}
	//		}
	//
	//	}

	//	private void handleExpected(String line) {
	//		currentRequest.setExpected(Integer.decode(line.substring("expected: ".length()).trim()).intValue());
	//	}

	private void handleSingleton(String line) {
		currentIU.setSingleton(line.contains("true") ? true : false);
	}

	/*
	 * Ensure that the current IU that we have been building is validate and if so, then
	 * add it to our collected list of all converted IUs from the file.
	 */
	private void validateAndAddIU() {
		if (currentIU == null)
			return;
		// For a package stanza, the id and version are the only mandatory elements
		if (currentIU.getId() == null)
			throw new IllegalStateException("Malformed \'package\' stanza. No package element found.");
		if (currentIU.getVersion() == null)
			throw new IllegalStateException("Malformed \'package\' stanza. Package " + currentIU.getId() + " does not have a version.");
		if (currentIU.getProvidedCapabilities().size() == 0) {
			currentIU.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, currentIU.getId(), currentIU.getVersion()), MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, currentIU.getId(), currentIU.getVersion())});
		}
		currentIU.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(currentIU.getId(), new VersionRange(Version.emptyVersion, true, currentIU.getVersion(), false), IUpdateDescriptor.NORMAL, null));
		//		if (currentIU.isInstalled()) {
		//			keepRequests.addAll(currentKeepRequests);
		//		}
		currentIU.setProperty(InstallableUnitDescription.PROP_TYPE_GROUP, "true");
		allIUs.add(MetadataFactory.createInstallableUnit(currentIU));
		// reset to be ready for the next stanza
		currentIU = null;
		currentKeepRequests.clear();
	}

	//	private void handleInstalled(String line) {
	//		String value = line.substring("installed: ".length());
	//		if (value.length() != 0) {
	//			if (DEBUG)
	//				if (!Boolean.parseBoolean(value)) {
	//					System.err.println("Unexpected value for installed.");
	//					return;
	//				}
	//			currentIU.setInstalled(true);
	//			preInstalled.add(new RequiredCapability(currentIU.getId(), new VersionRange(currentIU.getVersion()), true));
	//		}
	//	}

	//	private void handleInstall(String line) {
	//		line = line.substring("install: ".length());
	//		List installRequest = createRequires(line, true, false, true);
	//		for (Iterator iterator = installRequest.iterator(); iterator.hasNext();) {
	//			currentRequest.addInstallableUnit((IRequiredCapability) iterator.next());
	//		}
	//		return;
	//	}

	//	private void handleRequest(String line) {
	//		initializeQueryableArray();
	//		currentRequest = new ProfileChangeRequest(query);
	//		currentRequest.setPreInstalledIUs(preInstalled);
	//		currentRequest.setContrainstFromKeep(keepRequests);
	//	}
	//
	//	private void handleRemove(String line) {
	//		line = line.substring("remove: ".length());
	//		List removeRequest = createRequires(line, true, false, true);
	//		for (Iterator iterator = removeRequest.iterator(); iterator.hasNext();) {
	//			currentRequest.removeInstallableUnit((IRequiredCapability) iterator.next());
	//		}
	//		return;
	//	}

	//	private void initializeQueryableArray() {
	//		query = new QueryableArray((InstallableUnit[]) allIUs.toArray(new InstallableUnit[allIUs.size()]));
	//	}

	//	private void handleUpgrade(String line) {
	//		line = line.substring("upgrade: ".length());
	//		List updateRequest = createRequires(line, true, false, true);
	//		for (Iterator iterator = updateRequest.iterator(); iterator.hasNext();) {
	//			IRequiredCapability requirement = (IRequiredCapability) iterator.next();
	//			currentRequest.upgradeInstallableUnit(requirement);
	//
	//			//Add a requirement forcing uniqueness of the upgraded package in the resulting solution
	//			currentRequest.upgradeInstallableUnit(new RequiredCapability(requirement.getName(), VersionRange.emptyRange, 1));
	//
	//			//Add a requirement forcing the solution to be greater or equal to the highest installed version
	//			requirement = getHighestInstalledVersion(requirement);
	//			if (requirement != null)
	//				currentRequest.upgradeInstallableUnit(requirement);
	//		}
	//		return;
	//	}
	//
	//	private IRequiredCapability getHighestInstalledVersion(IRequiredCapability req) {
	//		Version highestVersion = null;
	//		Collector c = query.query(new CapabilityQuery(req), new Collector(), null);
	//		for (Iterator iterator = c.iterator(); iterator.hasNext();) {
	//			InstallableUnit candidate = (InstallableUnit) iterator.next();
	//			if (!candidate.isInstalled())
	//				continue;
	//			if (candidate.getId().equals(req.getName())) {
	//				if (highestVersion == null || candidate.getVersion().getMajor() > highestVersion.getMajor())
	//					highestVersion = candidate.getVersion();
	//			} else {
	//				//Requesting the upgrade of a virtual package
	//				IProvidedCapability[] prov = candidate.getProvidedCapabilities();
	//				for (int i = 0; i < prov.length; i++) {
	//					if (prov[i].getVersion().equals(VersionRange.emptyRange))
	//						continue;
	//					if (prov[i].getName().equals(req.getName()) && (highestVersion == null || prov[i].getVersion().getMinimum().getMajor() > highestVersion.getMajor()))
	//						highestVersion = prov[i].getVersion().getMinimum();
	//				}
	//			}
	//		}
	//		if (highestVersion == null)
	//			return null;
	//		return new RequiredCapability(req.getName(), new VersionRange(highestVersion, true, Version.maxVersion, true));
	//	}

	/*
	 * Convert the version string to a version object and set it on the IU
	 */
	private void handleVersion(String line) {
		currentIU.setVersion(Version.create(cudfPosintToInt(line.substring("version: ".length()))));
	}

	private String cudfPosintToInt(String posint) {
		if (posint.startsWith("+")) {
			return posint.substring(1).trim();
		}
		return posint.trim();
	}

	private void handleDepends(String line) {
		mergeRequirements(createRequires(line.substring("depends: ".length()), true, false, true));
	}

	//	private void handleRecommends(String line) {
	//		mergeRequirements(createRequires(line.substring("recommends: ".length()), true, true, true));
	//	}

	/*
	 * Conflicts are like depends except NOT'd.
	 */
	//TODO Remove conflict for now
	//	private void handleConflicts(String line) {
	//		List reqs = createRequires(line.substring("conflicts: ".length()), false, false, false);
	//		List conflicts = new ArrayList();
	//		for (Iterator iter = reqs.iterator(); iter.hasNext();) {
	//			IRequiredCapability req = (IRequiredCapability) iter.next();
	//			if (currentIU.getId().equals(req.getName()) && req.getRange().equals(VersionRange.emptyRange)) {
	//				currentIU.setSingleton(true);
	//			} else {
	//				conflicts.add(new NotRequirement(req));
	//			}
	//		}
	//		mergeRequirements(conflicts);
	//	}

	/*
	 * Set the given list of requirements on teh current IU. Merge if necessary.
	 */
	private void mergeRequirements(List requirements) {
		if (currentIU.getRequiredCapabilities() != null) {
			List<IRequirement> current = currentIU.getRequiredCapabilities();
			for (IRequirement iRequirement : current) {
				requirements.add(iRequirement);
			}
		}
		currentIU.setRequirements((IRequiredCapability[]) requirements.toArray(new IRequiredCapability[requirements.size()]));
	}

	/*
	 * Returns a map where the key is the package name and the value is a Tuple.
	 * If there is more than one entry for a particular package, the extra entries are included
	 * in the extraData field of the Tuple. 
	 */
	private List createPackageList(String line) {
		StringTokenizer tokenizer = new StringTokenizer(line, ",");
		List result = new ArrayList(tokenizer.countTokens());
		while (tokenizer.hasMoreElements()) {
			result.add(new Tuple(tokenizer.nextToken()));
		}
		return result;
	}

	private List createRequires(String line, boolean expandNotEquals, boolean optional, boolean dependency) {
		ArrayList ands = new ArrayList();
		StringTokenizer s = new StringTokenizer(line, ",");
		String subtoken;
		while (s.hasMoreElements()) {
			StringTokenizer subTokenizer = new StringTokenizer(s.nextToken(), "|");
			if (subTokenizer.countTokens() == 1) { //This token does not contain a |.
				subtoken = subTokenizer.nextToken().trim();
				// FIXME should be handled differently in depends and conflicts.
				if ("true!".equals(subtoken)) {
					if (dependency)
						continue;
					throw new RuntimeException("Cannot have true! in a conflict!!!!!");
				}
				if ("false!".equals(subtoken)) {
					if (!dependency)
						continue;
					throw new RuntimeException("Cannot have false! in a dependency!!!!!");
				}
				Object o = createRequire(subtoken, expandNotEquals, optional);
				if (o instanceof IRequiredCapability)
					ands.add(o);
				else
					ands.addAll((Collection) o);
				continue;
			}

			IRequiredCapability[] ors = new RequiredCapability[subTokenizer.countTokens()];
			int i = 0;
			while (subTokenizer.hasMoreElements()) {
				ors[i++] = (IRequiredCapability) createRequire(subTokenizer.nextToken().trim(), expandNotEquals, optional);
			}
			//TODO Remove OR'ing from requirements for now
			//			ands.add(new ORRequirement(ors, optional));
		}
		return ands;
	}

	private Object createRequire(String nextToken, boolean expandNotEquals, boolean optional) {
		//>, >=, =, <, <=, !=
		StringTokenizer expressionTokens = new StringTokenizer(nextToken.trim(), ">=!<", true);
		int tokenCount = expressionTokens.countTokens();

		if (tokenCount == 1) // a
			return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, expressionTokens.nextToken().trim(), VersionRange.emptyRange, null, optional, false, true);

		if (tokenCount == 3) // a > 2, a < 2, a = 2
			return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, expressionTokens.nextToken().trim(), createRange3(expressionTokens.nextToken(), expressionTokens.nextToken()), null, optional, false, true);

		if (tokenCount == 4) { //a >= 2, a <=2, a != 2
			String id = expressionTokens.nextToken().trim();
			String signFirstChar = expressionTokens.nextToken();
			expressionTokens.nextToken();//skip second char of the sign
			String version = expressionTokens.nextToken().trim();
			if (!("!".equals(signFirstChar))) // a >= 2 a <= 2
				return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, createRange4(signFirstChar, version), null, optional, false, true);

			//			//a != 2 TODO To uncomment
			//			if (expandNotEquals) {
			//				return new ORRequirement(new IRequiredCapability[] {new RequiredCapability(id, createRange3("<", version), optional), new RequiredCapability(id, createRange3(">", version), optional)}, optional);
			//			}
			ArrayList res = new ArrayList(2);
			res.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, createRange3("<", version), null, optional, false, true));
			res.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, createRange3(">", version), null, optional, false, true));
			return res;
		}
		return null;
	}

	private VersionRange createRange3(String sign, String versionAsString) {
		int version = Integer.decode(cudfPosintToInt(versionAsString)).intValue();
		sign = sign.trim();
		if (">".equals(sign))
			return new VersionRange(Version.createOSGi(version, 0, 0), false, Version.MAX_VERSION, false);
		if ("<".equals(sign))
			return new VersionRange(Version.emptyVersion, false, Version.createOSGi(version, 0, 0), false);
		if ("=".equals(sign))
			return new VersionRange(Version.createOSGi(version, 0, 0), true, Version.createOSGi(version, 0, 0), true);
		throw new IllegalArgumentException(sign);
	}

	private VersionRange createRange4(String sign, String versionAsString) {
		int version = Integer.decode(cudfPosintToInt(versionAsString)).intValue();
		if (">".equals(sign)) //THIS IS FOR >=
			return new VersionRange(Version.createOSGi(version, 0, 0), true, Version.MAX_VERSION, false);
		if ("<".equals(sign)) //THIS IS FOR <=
			return new VersionRange(Version.emptyVersion, false, Version.createOSGi(version, 0, 0), true);
		return null;
	}

	private IProvidedCapability createProvidedCapability(Tuple tuple) {
		//At this point the parser only deal with standard provided capabilities and not ranges like cudf does.
		assert tuple.extraData == null;
		assert tuple.operator == null;
		return MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, tuple.name, Version.create(tuple.version));
		//		Set extraData = tuple.extraData;
		// one constraint so simply return the capability
		//		if (extraData == null)
		//			return new ProvidedCapability(tuple.name, createVersionRange(tuple.operator, tuple.version));
		//		// 2 constraints (e.g. a>=1, a<4) so create a real range like a[1,4)
		//		if (extraData.size() == 1)
		//			return new ProvidedCapability(tuple.name, createVersionRange(tuple, (Tuple) extraData.iterator().next()));
		//		// TODO merge more than 2 requirements (a>2, a<4, a>3)
		//		return new ProvidedCapability(tuple.name, createVersionRange(tuple.operator, tuple.version));
	}

	/*
	 * Create and return a version range object which merges the 2 given versions and operators.
	 * e.g  a>=1 and a<4 becomes a[1,4)
	 */
	//	private VersionRange createVersionRange(Tuple t1, Tuple t2) {
	//		Version one = Version.parseVersion(t1.version);
	//		Version two = Version.parseVersion(t2.version);
	//		if (one.compareTo(two) < 0) {
	//			return new VersionRange(one, include(t1.operator), two, include(t2.operator));
	//		} else if (one.compareTo(two) == 0) {
	//			return new VersionRange(one, include(t1.operator), one, include(t1.operator));
	//		} else if (one.compareTo(two) > 0) {
	//			return new VersionRange(two, include(t2.operator), one, include(t1.operator));
	//		}
	//		// should never reach this. avoid compile error.
	//		return null;
	//	}

	/*
	 * Helper method for when we are creating version ranges and calculating "includeMin/Max".
	 */
	//	private boolean include(String operator) {
	//		return "=".equals(operator) || "<=".equals(operator) || ">=".equals(operator);
	//	}
	//
	//	/*
	//	 * Create and return a version range based on the given operator and number. Note that != is
	//	 * handled elsewhere.
	//	 */
	//	private VersionRange createVersionRange(String operator, String number) {
	//		if (operator == null || number == null)
	//			return VersionRange.emptyRange;
	//		if ("=".equals(operator))
	//			return new VersionRange('[' + number + ',' + number + ']');
	//		if ("<".equals(operator))
	//			return new VersionRange("[0," + number + ')');
	//		if (">".equals(operator))
	//			return new VersionRange('(' + number + ',' + Integer.MAX_VALUE + ']');
	//		if ("<=".equals(operator))
	//			return new VersionRange("[0," + number + ']');
	//		if (">=".equals(operator))
	//			return new VersionRange('[' + number + ',' + Integer.MAX_VALUE + ']');
	//		return VersionRange.emptyRange;
	//	}

	// package name matches: "^[a-zA-Z0-9+./@()%-]+$"
	private void handlePackage(String readLine) {
		currentIU = new MetadataFactory.InstallableUnitDescription();
		currentIU.setId(readLine.substring("package: ".length()).trim());
	}

	private void handleProvides(String line) {
		line = line.substring("provides: ".length());
		List pkgs = createPackageList(line);
		IProvidedCapability[] providedCapabilities = new ProvidedCapability[pkgs.size() + 2];
		int i = 0;
		for (Iterator iter = pkgs.iterator(); iter.hasNext();) {
			providedCapabilities[i++] = createProvidedCapability((Tuple) iter.next());
		}
		providedCapabilities[i++] = MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, currentIU.getId(), currentIU.getVersion());
		providedCapabilities[i++] = MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, currentIU.getId(), currentIU.getVersion());
		currentIU.setCapabilities(providedCapabilities);
	}

	//	// copied from ProfileSynchronizer
	//	private void debug(ProfileChangeRequest request) {
	//		if (!DEBUG || request == null)
	//			return;
	//		//		Log.println("\nProfile Change Request:");
	//		//		InstallableUnit[] toAdd = request.getAddedInstallableUnit();
	//		//		if (toAdd == null || toAdd.length == 0) {
	//		//			Log.println("No installable units to add.");
	//		//		} else {
	//		//			for (int i = 0; i < toAdd.length; i++)
	//		//				Log.println("Adding IU: " + toAdd[i].getId() + ' ' + toAdd[i].getVersion());
	//		//		}
	//		//		Map propsToAdd = request.getInstallableUnitProfilePropertiesToAdd();
	//		//		if (propsToAdd == null || propsToAdd.isEmpty()) {
	//		//			Log.println("No IU properties to add.");
	//		//		} else {
	//		//			for (Iterator iter = propsToAdd.keySet().iterator(); iter.hasNext();) {
	//		//				Object key = iter.next();
	//		//				Log.println("Adding IU property: " + key + "->" + propsToAdd.get(key));
	//		//			}
	//		//		}
	//		//
	//		//		InstallableUnit[] toRemove = request.getRemovedInstallableUnits();
	//		//		if (toRemove == null || toRemove.length == 0) {
	//		//			Log.println("No installable units to remove.");
	//		//		} else {
	//		//			for (int i = 0; i < toRemove.length; i++)
	//		//				Log.println("Removing IU: " + toRemove[i].getId() + ' ' + toRemove[i].getVersion());
	//		//		}
	//		//		Map propsToRemove = request.getInstallableUnitProfilePropertiesToRemove();
	//		//		if (propsToRemove == null || propsToRemove.isEmpty()) {
	//		//			Log.println("No IU properties to remove.");
	//		//		} else {
	//		//			for (Iterator iter = propsToRemove.keySet().iterator(); iter.hasNext();) {
	//		//				Object key = iter.next();
	//		//				Log.println("Removing IU property: " + key + "->" + propsToRemove.get(key));
	//		//			}
	//		//		}
	//	}

	// dump info to console
	private void debug(InstallableUnit unit) {
		//		if (!DEBUG)
		//			return;
		//		Log.println("\nInstallableUnit: " + unit.getId());
		//		Log.println("Version: " + unit.getVersion());
		//		if (unit.isInstalled())
		//			Log.println("Installed: true");
		//		IRequiredCapability[] reqs = unit.getRequiredCapabilities();
		//		for (int i = 0; i < reqs.length; i++) {
		//			Log.println("Requirement: " + reqs[i]);
		//		}
	}

	public IInstallableUnit getIU() {
		assert allIUs.size() == 1;
		return (IInstallableUnit) allIUs.get(0);
	}
}
