/*******************************************************************************
 *  Copyright (c) 2007, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnitPatch;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataParser;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IRequirementChange;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class IUPatchPersistenceTest extends AbstractProvisioningTest {

	// Randomly chose org.eclipse.osgi.services as the IU for testing persistence
	// but 'enhanced' it for better coverage.
	private static String PackagesNS = "osgi.packages";

	private static String id = "org.eclipse.osgi.services";
	private static Version version = Version.create("3.1.200.v20070605");
	private static IMatchExpression<IInstallableUnit> filter = InstallableUnit.parseFilter("(& (osgi.ws=win32) (osgi.os=win32) (osgi.arch=x86))"); // not really

	private static String[][] properties = new String[][] {new String[] {"equinox.p2.name", "OSGi Release 4.0.1 Services"}, //
			new String[] {"equinox.p2.description", "OSGi Service Platform Release 4.0.1 Service Interfaces and Classes"}, //
			new String[] {"equinox.p2.provider", "Eclipse.org"}, //
			new String[] {"equinox.p2.contact", "www.eclipse.org"}, //
			new String[] {"equinox.p2.copyright", "Copyright (c) 2003, 2004 IBM Corporation and others. A long-winded copyright notice."}};

	private static String[][] provides = new String[][] {new String[] {PackagesNS, "org.osgi.service.cm", "1.2.0"}, //
			new String[] {PackagesNS, "org.osgi.service.component", "1.0.0"}, //
			new String[] {PackagesNS, "org.osgi.service.device", "1.1.0"}, //
			new String[] {PackagesNS, "org.osgi.service.event", "1.1.0"}, //
			new String[] {PackagesNS, "org.osgi.service.http", "1.2.0"}, //
			new String[] {PackagesNS, "org.osgi.service.io", "1.0.0"}, //
			new String[] {PackagesNS, "org.osgi.service.log", "1.3.0"}, //
			new String[] {PackagesNS, "org.osgi.service.metatype", "1.1.0"}, //
			new String[] {PackagesNS, "org.osgi.service.provisioning", "1.1.0"}, //
			new String[] {PackagesNS, "org.osgi.service.upnp", "1.1.0"}, //
			new String[] {PackagesNS, "org.osgi.service.useradmin", "1.1.0"}, //
			new String[] {PackagesNS, "org.osgi.service.wireadmin", "1.0.0"}}; //

	private static String[][] requires = new String[][] {new String[] {PackagesNS, "javax.servlet", "0.0.0", "true"}, //
			new String[] {PackagesNS, "javax.servlet.http", "0.0.0", "true"}, //
			new String[] {PackagesNS, "org.osgi.framework", "1.2.0", "false"}}; //

	private static String[][] metaRequires = new String[][] {new String[] {PackagesNS, "some.actions1", "0.0.0", "true"}, //
			new String[] {PackagesNS, "some.actions2", "1.2.0", "false"}}; //

	private static String[][] instructions = new String[][] {new String[] {"manifest", "Manifest-Version: 1.0\\Bundle-Vendor: Eclipse.org\\Bundle-ContactAddress: www.eclipse.org\\...a whole bunch of other manifest content..."}, new String[] {"zipped", "true"}, //
			new String[] {"configure", "addProgramArg(programArg:-startup);addProgramArg(programArg:@artifact);"}}; //

	public static IInstallableUnit createPersistenceTestIU() {
		Map<String, String> propertyMap = createProperties(properties);
		IProvidedCapability[] additionalProvides = createProvided(provides);
		IRequirement[] requirements = createRequired(requires);
		IRequirement[] metaRequirements = createRequired(metaRequires);
		ITouchpointData tpData = createTouchpointData(instructions);
		IUpdateDescriptor update = createUpdateDescriptor();
		boolean singleton = false;
		IInstallableUnit iu = createIU(id, version, filter, requirements, additionalProvides, propertyMap, TOUCHPOINT_OSGI, tpData, singleton, update, metaRequirements);
		return iu;
	}

	private static IUpdateDescriptor createUpdateDescriptor() {
		return MetadataFactory.createUpdateDescriptor(id, new VersionRange(IU_TEST_VERSION, true, IU_TEST_VERSION, true), IUpdateDescriptor.HIGH, "desc");
	}

	private static Map<String, String> createProperties(String[][] keyValuePairs) {
		OrderedProperties props = new OrderedProperties(keyValuePairs.length);
		for (String[] nextPair : keyValuePairs) {
			props.put(nextPair[0], nextPair[1]);
		}
		return props;
	}

	private static IProvidedCapability[] createProvided(String[][] provideTuples) {
		IProvidedCapability[] provided = new IProvidedCapability[provideTuples.length];
		for (int i = 0; i < provideTuples.length; i++) {
			String[] nextTuple = provideTuples[i];
			provided[i] = MetadataFactory.createProvidedCapability(nextTuple[0], nextTuple[1], Version.create(nextTuple[2]));
		}
		// provided[provideTuples.length] = BUNDLE_CAPABILITY;
		return provided;
	}

	private static IRequirement[] createRequired(String[][] requireTuples) {
		IRequirement[] required = new IRequirement[requireTuples.length];
		for (int i = 0; i < requireTuples.length; i++) {
			String[] nextTuple = requireTuples[i];
			required[i] = MetadataFactory.createRequirement(nextTuple[0], nextTuple[1], new VersionRange(nextTuple[2]), null, Boolean.parseBoolean(nextTuple[3]), false);
		}
		return required;
	}

	private static ITouchpointData createTouchpointData(String[][] instructionData) {
		Map<String, Object> map = new LinkedHashMap<>(instructionData.length);
		for (String[] nextInstruction : instructionData) {
			map.put(nextInstruction[0], nextInstruction[1]);
		}
		return MetadataFactory.createTouchpointData(map);
	}

	private final static String IU_TEST_TARGET = "installableUnitTest";
	final static Version IU_TEST_VERSION = Version.create("0.0.1");

	private final static String IU_TEST_ELEMENT = "test";

	class IUStringWriter extends MetadataWriter {

		public IUStringWriter(ByteArrayOutputStream stream) {
			super(stream, new ProcessingInstruction[] {ProcessingInstruction.makeTargetVersionInstruction(IU_TEST_TARGET, IU_TEST_VERSION)});
		}

		public void writeTest(IInstallableUnit iu) {
			start(IU_TEST_ELEMENT);
			writeInstallableUnit(iu);
			end(IU_TEST_ELEMENT);
			flush();
		}
	}

	class IUStringParser extends MetadataParser {

		public IUStringParser(String bundleId) {
			super(bundleId);
		}

		public void parse(String profileString) throws IOException {
			this.status = null;
			try {
				XMLReader reader = getParser().getXMLReader();
				TestHandler testHandler = new TestHandler();
				reader.setContentHandler(new IUDocHandler(IU_TEST_ELEMENT, testHandler));
				reader.parse(new InputSource(new StringReader(profileString)));
				if (isValidXML()) {
					theIU = testHandler.getIU();
				}
			} catch (SAXException e) {
				throw new IOException(e.getMessage());
			} catch (ParserConfigurationException e) {
				fail();
			}
		}

		private IInstallableUnit theIU = null;

		private final class IUDocHandler extends DocHandler {

			public IUDocHandler(String rootName, RootHandler rootHandler) {
				super(rootName, rootHandler);
			}

			@Override
			public void processingInstruction(String target, String data) throws SAXException {
				if (IU_TEST_TARGET.equals(target)) {
					Version iuTestVersion = extractPIVersion(target, data);
					if (!IU_TEST_VERSION.equals(iuTestVersion)) {
						throw new SAXException("Bad iu test version.");
					}
				}
			}
		}

		private final class TestHandler extends RootHandler {

			private InstallableUnitHandler iuHandler = null;

			private InstallableUnit iu = null;
			private List<InstallableUnitDescription> singleton = new ArrayList<>(1);

			public TestHandler() {
				super();
			}

			public InstallableUnit getIU() {
				return iu;
			}

			@Override
			protected void handleRootAttributes(Attributes attributes) {
				parseAttributes(attributes, noAttributes, noAttributes);
			}

			@Override
			public void startElement(String name, Attributes attributes) {
				if (INSTALLABLE_UNIT_ELEMENT.equals(name)) {
					if (iuHandler == null) {
						iuHandler = new InstallableUnitHandler(this, attributes, singleton);
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
					if (iuHandler != null && singleton.size() == 1) {
						iu = (InstallableUnit) iuHandler.getInstallableUnit();
					}
				}
			}
		}

		@Override
		protected String getErrorMessage() {
			return "Error parsing installable unit string";
		}

		@Override
		protected Object getRootObject() {
			return theIU;
		}

	}

	public void testIUPersistence() throws IOException {
		IInstallableUnit iu0 = createPersistenceTestIU();
		validateIU(iu0);
		try (ByteArrayOutputStream output0 = new ByteArrayOutputStream(3072)) {
			IUStringWriter writer0 = new IUStringWriter(output0);
			writer0.writeTest(iu0);
			String iuText0 = output0.toString();
			IUStringParser parser = new IUStringParser(TestActivator.PI_PROV_TESTS);
			parser.parse(iuText0);
			assertTrue("Error parsing test iu: " + parser.getStatus().getMessage(), parser.getStatus().isOK());
			InstallableUnit iu1 = (InstallableUnit) parser.getRootObject();
			validateIU(iu1);
			try (ByteArrayOutputStream output1 = new ByteArrayOutputStream(1492)) {
				IUStringWriter writer = new IUStringWriter(output1);
				writer.writeTest(iu1);
				String iuText1 = output1.toString();
				assertTrue("Installable unit write after read after write produced different XML", iuText1.equals(iuText0));
			}
		}
	}

	public void testIUPatchPersistence() throws IOException {
		IInstallableUnit iu0 = createPatchIU();
		validateIU(iu0);
		try (ByteArrayOutputStream output0 = new ByteArrayOutputStream(3072)) {
			IUStringWriter writer0 = new IUStringWriter(output0);
			writer0.writeTest(iu0);
			String iuText0 = output0.toString();

			IUStringParser parser = new IUStringParser(TestActivator.PI_PROV_TESTS);
			parser.parse(iuText0);
			assertTrue("Error parsing test iu: " + parser.getStatus().getMessage(), parser.getStatus().isOK());
			InstallableUnitPatch iu1 = (InstallableUnitPatch) parser.getRootObject();
			validateIU(iu1);
			validateIUPatch(iu1);
			try (ByteArrayOutputStream output1 = new ByteArrayOutputStream(1492)) {
				IUStringWriter writer = new IUStringWriter(output1);
				writer.writeTest(iu1);
				String iuText1 = output1.toString();
				assertTrue("Installable unit write after read after write produced different XML", iuText1.equals(iuText0));
			}
		}

	}

	private IInstallableUnitPatch createPatchIU() {
		Map<String, String> propertyMap = createProperties(properties);
		IProvidedCapability[] additionalProvides = createProvided(provides);
		IRequirement[] requirements = createRequired(requires);
		IRequirement[] metaRequirements = createRequired(metaRequires);
		ITouchpointData tpData = createTouchpointData(instructions);
		IUpdateDescriptor update = createUpdateDescriptor();
		boolean singleton = false;
		IRequirementChange change1 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequirementChange change2 = MetadataFactory.createRequirementChange(null, MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequirementChange change3 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), null);
		IRequirement[][] scope = new IRequirement[][] {{MetadataFactory.createRequirement("foo", "bar", null, null, true, true), MetadataFactory.createRequirement("foo", "bar", null, null, true, true)}, {MetadataFactory.createRequirement("zoo", "far", null, null, true, true)}};
		IRequirement lifeCycle = MetadataFactory.createRequirement("zoo", "x", null, null, false, false, false);
		IInstallableUnitPatch iu = createIUPatch(id, version, filter, requirements, additionalProvides, propertyMap, TOUCHPOINT_OSGI, tpData, singleton, update, new IRequirementChange[] {change1, change2, change3}, scope, lifeCycle, metaRequirements);
		return iu;
	}

	private static void validateIUPatch(IInstallableUnitPatch iu) {
		validateIU(iu);
		assertTrue(iu.getApplicabilityScope() != null);
		assertTrue(iu.getRequirements() != null);
		assertEquals(3, iu.getRequirementsChange().size());
		assertEquals(null, iu.getRequirementsChange().get(1).applyOn());
		assertNotNull(iu.getRequirementsChange().get(1).newValue());
		assertEquals(null, iu.getRequirementsChange().get(2).newValue());
		assertNotNull(iu.getRequirementsChange().get(2).applyOn());
		assertEquals(2, iu.getApplicabilityScope().length);
		assertEquals(2, iu.getApplicabilityScope()[0].length);
		assertEquals(1, iu.getApplicabilityScope()[1].length);
		assertNotNull(iu.getLifeCycle());
	}

	private static void validateIU(IInstallableUnit iu) {
		assertTrue("Installable unit id is not correct", id.equals(iu.getId()));
		assertTrue("Installable unit version is not correct", version.equals(iu.getVersion()));
		assertTrue("Installable unit filter is not correct", filter.equals(iu.getFilter()));
		assertTrue("Installable unit properties are not correct", equal(properties, extractProperties(iu)));
		assertTrue("Installable unit provided capabilities are not correct", equal(addSelfCapability(iu, provides), extractProvides(iu)));
		assertTrue("Installable unit required capabilities are not correct", equal(requires, extractRequires(iu))); // The lifecycle is added as a requirement for now to make things easier
		assertTrue("Installable unit meta required capabilities are not correct", equal(metaRequires, extractMetaRequires(iu)));
		assertTrue("Installable unit update descriptor are not correct", id.equals(RequiredCapability.extractName(iu.getUpdateDescriptor().getIUsBeingUpdated().iterator().next())));
		assertTrue("Installable unit update descriptor are not correct", IUpdateDescriptor.HIGH == iu.getUpdateDescriptor().getSeverity());
		assertTrue("Installable unit update descriptor are not correct", "desc".equals(iu.getUpdateDescriptor().getDescription()));
		assertTrue("Installable unit update descriptor are not correct", new VersionRange(IU_TEST_VERSION, true, IU_TEST_VERSION, true).equals(RequiredCapability.extractRange(iu.getUpdateDescriptor().getIUsBeingUpdated().iterator().next())));
	}

	private static String[][] extractProperties(IInstallableUnit iu) {
		Map<String, String> props = iu.getProperties();
		Set<String> keys = props.keySet();
		String[][] pairs = new String[keys.size()][2];
		int index = 0;
		for (Entry<String, String> entry : props.entrySet()) {
			pairs[index] = new String[] {entry.getKey(), entry.getValue()};
			index++;
		}
		return pairs;
	}

	private static String[][] addSelfCapability(IInstallableUnit iu, String[][] provideTuples) {
		String[][] augmentedProvides = new String[provideTuples.length + 1][3];
		IProvidedCapability self = getSelfCapability(iu);
		augmentedProvides[0] = new String[] {self.getNamespace(), self.getName(), self.getVersion().toString()};
		for (int i = 0; i < provideTuples.length; i++) {
			augmentedProvides[i + 1] = provideTuples[i];
		}
		return augmentedProvides;
	}

	private static String[][] extractProvides(IInstallableUnit iu) {
		Collection<IProvidedCapability> provydes = iu.getProvidedCapabilities();
		String[][] tuples = new String[provydes.size()][3];
		int i = 0;
		for (IProvidedCapability capability : provydes) {
			tuples[i++] = new String[] {capability.getNamespace(), capability.getName(), capability.getVersion().toString()};
		}
		return tuples;
	}

	private static String[][] extractRequires(IInstallableUnit iu) {
		Collection<IRequirement> reqs = iu.getRequirements();
		String[][] tuples = new String[reqs.size()][4];
		int i = 0;
		for (IRequirement iRequirement : reqs) {
			IRequiredCapability next = (IRequiredCapability) iRequirement;
			tuples[i++] = new String[] {next.getNamespace(), next.getName(), next.getRange().toString(), Boolean.valueOf(next.getMin() == 0).toString()};
		}
		return tuples;
	}

	private static String[][] extractMetaRequires(IInstallableUnit iu) {
		Collection<IRequirement> requyres = iu.getMetaRequirements();
		String[][] tuples = new String[requyres.size()][4];
		int i = 0;
		for (IRequirement iRequirement : requyres) {
			IRequiredCapability next = (IRequiredCapability) iRequirement;
			tuples[i++] = new String[] {next.getNamespace(), next.getName(), next.getRange().toString(), Boolean.valueOf(next.getMin() == 0).toString()};
		}
		return tuples;
	}

}
