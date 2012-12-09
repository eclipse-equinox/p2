/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.util.*;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.AdviceFileParser;
import org.eclipse.equinox.p2.query.QueryUtil;

public class AdviceFileParserTest extends TestCase {
	public void testNoAdvice() {
		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, Collections.EMPTY_MAP);
		parser.parse();
		assertNull(parser.getAdditionalInstallableUnitDescriptions());
		assertNull(parser.getProperties());
		assertNull(parser.getProvidedCapabilities());
		assertNull(parser.getRequiredCapabilities());
		assertNull(parser.getTouchpointInstructions());
	}

	public void testAdviceVersion() {
		Map map = new HashMap();
		map.put("advice.version", "1.0");
		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();

		map.put("advice.version", "999");
		parser = new AdviceFileParser("id", Version.emptyVersion, map);
		try {
			parser.parse();
		} catch (IllegalStateException e) {
			return;
		}
		fail("expected version parse problem");
	}

	public void testUpdateDescriptorAdvice() {
		Map map = new HashMap();
		map.put("update.id", "testName");
		map.put("update.severity", "10");
		map.put("update.description", "Test Description");
		map.put("update.range", "(1.0.0,10.10.10)");

		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();

		IUpdateDescriptor updateDescriptor = parser.getUpdateDescriptor();
		String testName = RequiredCapability.extractName(updateDescriptor.getIUsBeingUpdated().iterator().next());
		VersionRange testVersionRange = RequiredCapability.extractRange(updateDescriptor.getIUsBeingUpdated().iterator().next());
		assertEquals("testName", testName);
		assertEquals(Version.parseVersion("1.0.0"), testVersionRange.getMinimum());
		assertEquals(Version.parseVersion("10.10.10"), testVersionRange.getMaximum());
		assertEquals(10, updateDescriptor.getSeverity());
		assertEquals("Test Description", updateDescriptor.getDescription());
	}

	public void testUpdateDescriptorAdviceDefaultBound() {
		Map map = new HashMap();
		map.put("update.id", "testName");
		map.put("update.severity", "10");
		map.put("update.description", "Test Description");
		map.put("update.range", "(1.0.0,$version$)");

		AdviceFileParser parser = new AdviceFileParser("id", Version.parseVersion("9.10.11"), map);
		parser.parse();

		IUpdateDescriptor updateDescriptor = parser.getUpdateDescriptor();
		String testName = RequiredCapability.extractName(updateDescriptor.getIUsBeingUpdated().iterator().next());
		VersionRange testVersionRange = RequiredCapability.extractRange(updateDescriptor.getIUsBeingUpdated().iterator().next());
		assertEquals("testName", testName);
		assertEquals(Version.parseVersion("1.0.0"), testVersionRange.getMinimum());
		assertEquals(Version.parseVersion("9.10.11"), testVersionRange.getMaximum());
		assertEquals(10, updateDescriptor.getSeverity());
		assertEquals("Test Description", updateDescriptor.getDescription());
	}

	public void testUpdateDescriptorWithMatch() {
		Map map = new HashMap();
		map.put("update.matchExp", "providedCapabilities.exists(pc | pc.namespace == 'org.eclipse.equinox.p2.iu' && (pc.name == 'B' || pc.name == 'C'))");
		map.put("update.severity", "10");
		map.put("update.description", "Test Description");

		AdviceFileParser parser = new AdviceFileParser("id", Version.parseVersion("9.10.11"), map);
		parser.parse();

		IUpdateDescriptor updateDescriptor = parser.getUpdateDescriptor();
		assertEquals("Test Description", updateDescriptor.getDescription());
		assertEquals(10, updateDescriptor.getSeverity());
		//Here we test that the extraction of the name fails since this is not of an appropriate format
		boolean exceptionRaised = false;
		try {
			RequiredCapability.extractName(updateDescriptor.getIUsBeingUpdated().iterator().next());
		} catch (IllegalArgumentException e) {
			exceptionRaised = true;
		}
		assertTrue(exceptionRaised);
	}

	public void testUpdateDescriptorAdviceDefaultBound2() {
		Map map = new HashMap();
		map.put("update.id", "testName");
		map.put("update.severity", "10");
		map.put("update.description", "Test Description");

		AdviceFileParser parser = new AdviceFileParser("id", Version.parseVersion("9.10.11"), map);
		parser.parse();

		IUpdateDescriptor updateDescriptor = parser.getUpdateDescriptor();
		String testName = RequiredCapability.extractName(updateDescriptor.getIUsBeingUpdated().iterator().next());
		VersionRange testVersionRange = RequiredCapability.extractRange(updateDescriptor.getIUsBeingUpdated().iterator().next());
		assertEquals("testName", testName);
		assertEquals(Version.parseVersion("0.0.0"), testVersionRange.getMinimum());
		assertEquals(Version.parseVersion("9.10.11"), testVersionRange.getMaximum());
		assertEquals(10, updateDescriptor.getSeverity());
		assertEquals("Test Description", updateDescriptor.getDescription());
	}

	public void testUpdateDescriptorAdviceDefaultID() {
		Map map = new HashMap();
		map.put("update.severity", "10");
		map.put("update.description", "Test Description");

		AdviceFileParser parser = new AdviceFileParser("id", Version.parseVersion("9.10.11"), map);
		parser.parse();

		IUpdateDescriptor updateDescriptor = parser.getUpdateDescriptor();
		String testName = RequiredCapability.extractName(updateDescriptor.getIUsBeingUpdated().iterator().next());
		VersionRange testVersionRange = RequiredCapability.extractRange(updateDescriptor.getIUsBeingUpdated().iterator().next());
		assertEquals("id", testName);
		assertEquals(Version.parseVersion("0.0.0"), testVersionRange.getMinimum());
		assertEquals(Version.parseVersion("9.10.11"), testVersionRange.getMaximum());
		assertEquals(10, updateDescriptor.getSeverity());
		assertEquals("Test Description", updateDescriptor.getDescription());
	}

	public void testUpdateDescriptorAdviceDefaults() {
		Map map = new HashMap();
		map.put("update.id", "id");

		AdviceFileParser parser = new AdviceFileParser("id", Version.parseVersion("9.10.11"), map);
		parser.parse();

		IUpdateDescriptor updateDescriptor = parser.getUpdateDescriptor();
		String testName = RequiredCapability.extractName(updateDescriptor.getIUsBeingUpdated().iterator().next());
		VersionRange testVersionRange = RequiredCapability.extractRange(updateDescriptor.getIUsBeingUpdated().iterator().next());
		assertEquals("id", testName);
		assertEquals(Version.parseVersion("0.0.0"), testVersionRange.getMinimum());
		assertEquals(Version.parseVersion("9.10.11"), testVersionRange.getMaximum());
		assertEquals(0, updateDescriptor.getSeverity());
		assertEquals(null, updateDescriptor.getDescription());
	}

	public void testPropertyAdvice() {
		Map map = new HashMap();
		map.put("properties.0.name", "testName1");
		map.put("properties.0.value", "testValue1");
		map.put("properties.1.name", "testName2");
		map.put("properties.1.value", "testValue2");

		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		assertEquals("testValue1", parser.getProperties().get("testName1"));
		assertEquals("testValue2", parser.getProperties().get("testName2"));
	}

	public void testProvidesAdvice() {
		Map map = new HashMap();
		map.put("provides.0.namespace", "testNamespace1");
		map.put("provides.0.name", "testName1");
		map.put("provides.0.version", "1.2.3.$qualifier$");

		AdviceFileParser parser = new AdviceFileParser("id", Version.create("1.0.0.v20090909"), map);
		parser.parse();
		IProvidedCapability[] capabilities = parser.getProvidedCapabilities();
		assertEquals(1, capabilities.length);
		assertEquals("testNamespace1", capabilities[0].getNamespace());
		assertEquals("testName1", capabilities[0].getName());
		assertEquals(Version.create("1.2.3.v20090909"), capabilities[0].getVersion());

		map.put("provides.1.namespace", "testNamespace2");
		map.put("provides.1.name", "testName2");
		map.put("provides.1.version", "$version$");

		parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		capabilities = parser.getProvidedCapabilities();
		assertEquals(2, capabilities.length);
		assertEquals("testNamespace1", capabilities[0].getNamespace());
		assertEquals("testName1", capabilities[0].getName());
		assertEquals(Version.create("1.2.3"), capabilities[0].getVersion());
		assertEquals("testNamespace2", capabilities[1].getNamespace());
		assertEquals("testName2", capabilities[1].getName());
		assertEquals(Version.emptyVersion, capabilities[1].getVersion());
	}

	public void testRequiresAdvice() {
		Map map = new HashMap();
		map.put("requires.0.namespace", "testNamespace1");
		map.put("requires.0.name", "testName1");
		map.put("requires.0.range", "[1.2.3.$qualifier$, 2)");
		map.put("requires.0.greedy", Boolean.TRUE.toString());
		map.put("requires.0.optional", Boolean.TRUE.toString());
		map.put("requires.0.multiple", Boolean.TRUE.toString());

		AdviceFileParser parser = new AdviceFileParser("id", Version.create("1.0.0.v20090909"), map);
		parser.parse();
		IRequirement[] reqs = parser.getRequiredCapabilities();
		assertEquals(1, reqs.length);
		assertEquals("testNamespace1", RequiredCapability.extractNamespace(reqs[0].getMatches()));
		assertEquals("testName1", RequiredCapability.extractName(reqs[0].getMatches()));
		assertEquals(new VersionRange("[1.2.3.v20090909, 2)"), RequiredCapability.extractRange(reqs[0].getMatches()));

		map.put("requires.1.namespace", "testNamespace2");
		map.put("requires.1.name", "testName2");
		map.put("requires.1.range", "$version$");
		map.put("requires.1.greedy", Boolean.FALSE.toString());
		map.put("requires.1.optional", Boolean.FALSE.toString());
		//default 
		//		map.put("requires.1.multiple", Boolean.FALSE.toString());

		parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		reqs = parser.getRequiredCapabilities();
		assertEquals(2, reqs.length);
		assertEquals("testNamespace1", RequiredCapability.extractNamespace(reqs[0].getMatches()));
		assertEquals("testName1", RequiredCapability.extractName(reqs[0].getMatches()));
		assertEquals(new VersionRange("[1.2.3, 2)"), RequiredCapability.extractRange(reqs[0].getMatches()));
		assertEquals(true, reqs[0].isGreedy());
		assertEquals(0, reqs[0].getMin());
		assertEquals("testNamespace2", RequiredCapability.extractNamespace(reqs[1].getMatches()));
		assertEquals("testName2", RequiredCapability.extractName(reqs[1].getMatches()));
		assertEquals(new VersionRange(Version.emptyVersion.toString()), RequiredCapability.extractRange(reqs[1].getMatches()));
		assertEquals(false, reqs[1].isGreedy());
		assertEquals(1, reqs[1].getMin());
	}

	public void testRequireWithExpression() {
		Map map = new HashMap();
		String matchExp = "properties[abc] == 'def'";
		map.put("requires.0.matchExp", matchExp);
		map.put("requires.0.greedy", Boolean.TRUE.toString());
		map.put("requires.0.min", "1");
		map.put("requires.0.max", "1");

		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		IRequirement[] reqs = parser.getRequiredCapabilities();
		reqs = parser.getRequiredCapabilities();
		assertEquals(1, reqs.length);

		IMatchExpression<IInstallableUnit> matchExpression = ExpressionUtil.getFactory().matchExpression(ExpressionUtil.parse(matchExp));
		assertEquals(matchExpression, reqs[0].getMatches());
	}

	public void testRequireWithExpressionAndOptional() {
		Map map = new HashMap();
		String matchExp = "properties[abc] == 'def'";
		map.put("requires.0.matchExp", matchExp);
		map.put("requires.0.greedy", Boolean.TRUE.toString());
		map.put("requires.0.optional", Boolean.TRUE.toString());

		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		IRequirement[] reqs = parser.getRequiredCapabilities();
		reqs = parser.getRequiredCapabilities();
		assertEquals(1, reqs.length);

		IMatchExpression<IInstallableUnit> matchExpression = ExpressionUtil.getFactory().matchExpression(ExpressionUtil.parse(matchExp));
		assertEquals(matchExpression, reqs[0].getMatches());
		assertEquals(0, reqs[0].getMin());
		assertEquals(1, reqs[0].getMax());
	}

	public void testMetaRequiresAdvice() {
		Map map = new HashMap();
		map.put("metaRequirements.0.namespace", "testNamespace1");
		map.put("metaRequirements.0.name", "testName1");
		map.put("metaRequirements.0.range", "[1.2.3.$qualifier$, 2)");
		map.put("metaRequirements.0.greedy", Boolean.TRUE.toString());
		map.put("metaRequirements.0.optional", Boolean.TRUE.toString());
		map.put("metaRequirements.0.multiple", Boolean.TRUE.toString());

		AdviceFileParser parser = new AdviceFileParser("id", Version.create("1.0.0.v20090909"), map);
		parser.parse();
		IRequirement[] reqs = parser.getMetaRequiredCapabilities();
		assertEquals(1, reqs.length);
		assertEquals("testNamespace1", RequiredCapability.extractNamespace(reqs[0].getMatches()));
		assertEquals("testName1", RequiredCapability.extractName(reqs[0].getMatches()));
		assertEquals(new VersionRange("[1.2.3.v20090909, 2)"), RequiredCapability.extractRange(reqs[0].getMatches()));

		map.put("metaRequirements.1.namespace", "testNamespace2");
		map.put("metaRequirements.1.name", "testName2");
		map.put("metaRequirements.1.range", "$version$");
		map.put("metaRequirements.1.greedy", Boolean.FALSE.toString());
		map.put("metaRequirements.1.optional", Boolean.FALSE.toString());
		//default 
		//		map.put("requires.1.multiple", Boolean.FALSE.toString());

		parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		reqs = parser.getMetaRequiredCapabilities();
		assertEquals(2, reqs.length);
		assertEquals("testNamespace1", RequiredCapability.extractNamespace(reqs[0].getMatches()));
		assertEquals("testName1", RequiredCapability.extractName(reqs[0].getMatches()));
		assertEquals(new VersionRange("[1.2.3, 2)"), RequiredCapability.extractRange(reqs[0].getMatches()));
		assertEquals(true, reqs[0].isGreedy());
		assertEquals(0, reqs[0].getMin());
		assertEquals("testNamespace2", RequiredCapability.extractNamespace(reqs[1].getMatches()));
		assertEquals("testName2", RequiredCapability.extractName(reqs[1].getMatches()));
		assertEquals(new VersionRange(Version.emptyVersion.toString()), RequiredCapability.extractRange(reqs[1].getMatches()));
		assertEquals(false, reqs[1].isGreedy());
		assertEquals(1, reqs[1].getMin());
	}

	public void testInstructionsAdvice() {
		Map map = new HashMap();
		map.put("instructions.configure", "addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);");

		map.put("instructions.unconfigure", "removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)");
		map.put("instructions.unconfigure.import", "some.removeProgramArg");

		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		ITouchpointInstruction configure = parser.getTouchpointInstructions().get("configure");
		assertEquals(null, configure.getImportAttribute());
		assertEquals("addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);", configure.getBody());

		ITouchpointInstruction unconfigure = parser.getTouchpointInstructions().get("unconfigure");
		assertEquals("some.removeProgramArg", unconfigure.getImportAttribute());
		assertEquals("removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)", unconfigure.getBody());
	}

	public void testAdditionalInstallableUnitDescriptionsAdvice() {
		Map map = new HashMap();
		map.put("units.0.id", "testid0");
		map.put("units.0.version", "1.2.3");

		map.put("units.1.id", "testid1");
		map.put("units.1.version", "1.2.4");
		map.put("units.1.singleton", "true");
		map.put("units.1.copyright", "testCopyright");
		map.put("units.1.copyright.location", "http://localhost/test");
		map.put("units.1.filter", "(test=testFilter)");
		map.put("units.1.touchpoint.id", "testTouchpointId");
		map.put("units.1.touchpoint.version", "1.2.5");
		map.put("units.1.update.id", "testid1");
		map.put("units.1.update.range", "(1,2)");
		map.put("units.1.update.severity", "2");
		map.put("units.1.update.description", "some description");
		map.put("units.1.artifacts.0.id", "testArtifact1");
		map.put("units.1.artifacts.0.version", "1.2.6");
		map.put("units.1.artifacts.0.classifier", "testClassifier1");
		map.put("units.1.artifacts.1.id", "testArtifact2");
		map.put("units.1.artifacts.1.version", "1.2.7");
		map.put("units.1.artifacts.1.classifier", "testClassifier2");
		map.put("units.1.licenses.0", "testLicense");
		map.put("units.1.licenses.0.location", "http://localhost/license");
		map.put("units.1.properties.0.name", "testName1");
		map.put("units.1.properties.0.value", "testValue1");
		map.put("units.1.properties.1.name", "testName2");
		map.put("units.1.properties.1.value", "testValue2");
		map.put("units.1.requires.0.namespace", "testNamespace1");
		map.put("units.1.requires.0.name", "testName1");
		map.put("units.1.requires.0.range", "[1.2.3.$qualifier$, 2)");
		map.put("units.1.requires.0.greedy", Boolean.TRUE.toString());
		map.put("units.1.requires.0.optional", Boolean.TRUE.toString());
		map.put("units.1.requires.0.multiple", Boolean.TRUE.toString());
		map.put("units.1.requires.1.namespace", "testNamespace2");
		map.put("units.1.requires.1.name", "testName2");
		map.put("units.1.requires.1.range", "$version$");
		map.put("units.1.requires.1.greedy", Boolean.FALSE.toString());
		map.put("units.1.requires.1.optional", Boolean.FALSE.toString());
		map.put("units.1.metaRequirements.0.namespace", "testNamespace1");
		map.put("units.1.metaRequirements.0.name", "testName1");
		map.put("units.1.metaRequirements.0.range", "[1.2.3.$qualifier$, 2)");
		map.put("units.1.metaRequirements.0.greedy", Boolean.TRUE.toString());
		map.put("units.1.metaRequirements.0.optional", Boolean.TRUE.toString());
		map.put("units.1.metaRequirements.0.multiple", Boolean.TRUE.toString());
		map.put("units.1.metaRequirements.1.namespace", "testNamespace2");
		map.put("units.1.metaRequirements.1.name", "testName2");
		map.put("units.1.metaRequirements.1.range", "$version$");
		map.put("units.1.metaRequirements.1.greedy", Boolean.FALSE.toString());
		map.put("units.1.metaRequirements.1.optional", Boolean.FALSE.toString());
		map.put("units.1.provides.0.namespace", "testNamespace1");
		map.put("units.1.provides.0.name", "testName1");
		map.put("units.1.provides.0.version", "1.2.3.$qualifier$");
		map.put("units.1.provides.1.namespace", "testNamespace2");
		map.put("units.1.provides.1.name", "testName2");
		map.put("units.1.provides.1.version", "$version$");
		map.put("units.1.instructions.configure", "addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);");
		map.put("units.1.instructions.unconfigure", "removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)");
		map.put("units.1.instructions.unconfigure.import", "some.removeProgramArg");

		map.put("units.1.hostRequirements.0.namespace", "testNamespace1");
		map.put("units.1.hostRequirements.0.name", "testName1");
		map.put("units.1.hostRequirements.0.range", "[1.2.3.$qualifier$, 2)");
		map.put("units.1.hostRequirements.0.greedy", Boolean.TRUE.toString());
		map.put("units.1.hostRequirements.0.optional", Boolean.TRUE.toString());
		map.put("units.1.hostRequirements.0.multiple", Boolean.TRUE.toString());
		map.put("units.1.hostRequirements.1.namespace", "testNamespace2");
		map.put("units.1.hostRequirements.1.name", "testName2");
		map.put("units.1.hostRequirements.1.range", "$version$");
		map.put("units.1.hostRequirements.1.greedy", Boolean.FALSE.toString());
		map.put("units.1.hostRequirements.1.optional", Boolean.FALSE.toString());

		AdviceFileParser parser = new AdviceFileParser("id", Version.emptyVersion, map);
		parser.parse();
		MetadataFactory.InstallableUnitDescription[] descriptions = parser.getAdditionalInstallableUnitDescriptions();
		IInstallableUnit iu0 = MetadataFactory.createInstallableUnit(descriptions[0]);
		assertEquals("testid0", iu0.getId());
		assertEquals(Version.create("1.2.3"), iu0.getVersion());
		assertFalse(iu0.isSingleton());
		assertFalse(QueryUtil.isFragment(iu0));
		assertEquals(0, iu0.getArtifacts().size());
		assertEquals(null, iu0.getCopyright());
		assertEquals(null, iu0.getFilter());
		assertEquals(0, iu0.getLicenses().size());
		assertEquals(0, iu0.getProperties().size());
		assertEquals(0, iu0.getRequirements().size());
		assertEquals(0, iu0.getProvidedCapabilities().size());
		assertEquals(0, iu0.getMetaRequirements().size());
		assertEquals(0, iu0.getTouchpointData().size());
		assertEquals(ITouchpointType.NONE, iu0.getTouchpointType());
		assertEquals(null, iu0.getUpdateDescriptor());

		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(descriptions[1]);
		assertEquals("testid1", iu1.getId());
		assertEquals(Version.create("1.2.4"), iu1.getVersion());
		assertTrue(iu1.isSingleton());
		assertEquals(2, iu1.getArtifacts().size());
		Iterator it = iu1.getArtifacts().iterator();
		IArtifactKey key0 = (IArtifactKey) it.next();
		IArtifactKey key1 = (IArtifactKey) it.next();
		assertEquals("testArtifact1", key0.getId());
		assertEquals(Version.create("1.2.6"), key0.getVersion());
		assertEquals("testClassifier1", key0.getClassifier());
		assertEquals("testArtifact2", key1.getId());
		assertEquals(Version.create("1.2.7"), key1.getVersion());
		assertEquals("testClassifier2", key1.getClassifier());
		assertEquals("testCopyright", iu1.getCopyright().getBody());
		assertEquals("http://localhost/test", iu1.getCopyright().getLocation().toString());
		assertEquals(InstallableUnit.parseFilter("(test=testFilter)"), iu1.getFilter());
		assertEquals("testLicense", iu1.getLicenses().iterator().next().getBody());
		assertEquals("http://localhost/license", iu1.getLicenses().iterator().next().getLocation().toString());
		assertEquals("testValue1", iu1.getProperty("testName1"));
		assertEquals("testValue2", iu1.getProperty("testName2"));

		Collection<IRequirement> reqs = iu1.getRequirements();
		Iterator it2 = reqs.iterator();
		IRequirement req0 = (IRequirement) it2.next();
		IRequirement req1 = (IRequirement) it2.next();
		assertEquals(2, reqs.size());
		assertEquals("testNamespace1", RequiredCapability.extractNamespace(req0.getMatches()));
		assertEquals("testName1", RequiredCapability.extractName(req0.getMatches()));
		assertEquals(new VersionRange("[1.2.3, 2)"), RequiredCapability.extractRange(req0.getMatches()));
		assertEquals(true, req0.isGreedy());
		assertEquals(0, req0.getMin());
		assertEquals("testNamespace2", RequiredCapability.extractNamespace(req1.getMatches()));
		assertEquals("testName2", RequiredCapability.extractName(req1.getMatches()));
		assertEquals(new VersionRange(Version.emptyVersion.toString()), RequiredCapability.extractRange(req1.getMatches()));
		assertEquals(false, req1.isGreedy());
		assertEquals(1, req1.getMin());

		Collection<IProvidedCapability> provided = iu1.getProvidedCapabilities();
		Iterator<IProvidedCapability> it4 = provided.iterator();
		IProvidedCapability cap1 = it4.next();
		IProvidedCapability cap2 = it4.next();
		assertEquals(2, provided.size());
		assertEquals("testNamespace1", cap1.getNamespace());
		assertEquals("testName1", cap1.getName());
		assertEquals(Version.create("1.2.3"), cap1.getVersion());
		assertEquals("testNamespace2", cap2.getNamespace());
		assertEquals("testName2", cap2.getName());
		assertEquals(Version.emptyVersion, cap2.getVersion());

		Collection<IRequirement> metarequirements = iu1.getMetaRequirements();
		assertEquals(2, metarequirements.size());
		Iterator it3 = metarequirements.iterator();
		IRequirement metaReq0 = (IRequirement) it3.next();
		IRequirement metaReq1 = (IRequirement) it3.next();

		assertEquals("testNamespace1", RequiredCapability.extractNamespace(metaReq0.getMatches()));
		assertEquals("testName1", RequiredCapability.extractName(metaReq0.getMatches()));
		assertEquals(new VersionRange("[1.2.3, 2)"), RequiredCapability.extractRange(metaReq0.getMatches()));
		assertEquals(true, metaReq0.isGreedy());
		assertEquals(0, metaReq0.getMin());
		assertEquals("testNamespace2", RequiredCapability.extractNamespace(metaReq1.getMatches()));
		assertEquals("testName2", RequiredCapability.extractName(metaReq1.getMatches()));
		assertEquals(new VersionRange(Version.emptyVersion.toString()), RequiredCapability.extractRange(metaReq1.getMatches()));
		assertEquals(false, metaReq1.isGreedy());
		assertEquals(1, metaReq1.getMin());

		assertEquals(1, iu1.getTouchpointData().size());
		ITouchpointInstruction configure = iu1.getTouchpointData().iterator().next().getInstruction("configure");
		assertEquals(null, configure.getImportAttribute());
		assertEquals("addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);", configure.getBody());

		ITouchpointInstruction unconfigure = iu1.getTouchpointData().iterator().next().getInstruction("unconfigure");
		assertEquals("some.removeProgramArg", unconfigure.getImportAttribute());
		assertEquals("removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)", unconfigure.getBody());

		assertEquals(MetadataFactory.createTouchpointType("testTouchpointId", Version.create("1.2.5")), iu1.getTouchpointType());
		assertEquals("testid1", RequiredCapability.extractName(iu1.getUpdateDescriptor().getIUsBeingUpdated().iterator().next()));
		assertEquals(new VersionRange("(1,2)"), RequiredCapability.extractRange(iu1.getUpdateDescriptor().getIUsBeingUpdated().iterator().next()));
		assertEquals(2, iu1.getUpdateDescriptor().getSeverity());
		assertEquals("some description", iu1.getUpdateDescriptor().getDescription());

		assertTrue(QueryUtil.isFragment(iu1));
		Collection<IRequirement> hostRequired = ((IInstallableUnitFragment) iu1).getHost();
		assertEquals(2, hostRequired.size());
		Iterator<IRequirement> iterator = hostRequired.iterator();
		IRequirement requirement = iterator.next();
		assertEquals("testNamespace1", RequiredCapability.extractNamespace(requirement.getMatches()));
		assertEquals("testName1", RequiredCapability.extractName(requirement.getMatches()));
		assertEquals(new VersionRange("[1.2.3, 2)"), RequiredCapability.extractRange(requirement.getMatches()));
		assertEquals(true, requirement.isGreedy());
		assertEquals(0, requirement.getMin());

		IRequirement secondRequirement = iterator.next();
		assertEquals("testNamespace2", RequiredCapability.extractNamespace(secondRequirement.getMatches()));
		assertEquals("testName2", RequiredCapability.extractName(secondRequirement.getMatches()));
		assertEquals(new VersionRange(Version.emptyVersion.toString()), RequiredCapability.extractRange(secondRequirement.getMatches()));
		assertEquals(false, secondRequirement.isGreedy());
		assertEquals(1, secondRequirement.getMin());
	}
}
