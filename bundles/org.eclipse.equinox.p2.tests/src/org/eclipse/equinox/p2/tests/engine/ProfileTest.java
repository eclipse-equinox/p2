/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.BundleContext;
import org.xml.sax.*;

/**
 * Simple test of the engine API.
 */
public class ProfileTest extends AbstractProvisioningTest {

	private static final String PROFILE_NAME = "ProfileTest";

	public ProfileTest(String name) {
		super(name);
	}

	public ProfileTest() {
		super("");
	}

	public void testNullProfile() {
		try {
			createProfile(null);
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testEmptyProfile() {
		try {
			createProfile("");
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail();
	}

	public void testAddRemoveProperty() throws ProvisionException {
		IProfileRegistry registry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		assertNull(registry.getProfile(PROFILE_NAME));
		Properties properties = new Properties();
		properties.put("test", "test");
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME, properties);
		assertTrue(profile.getProperties().containsKey("test"));
		assertEquals("test", profile.getProperty("test"));
		profile.removeProperty("test");
		assertNull(profile.getProperty("test"));
		profile.addProperties(properties);
		assertEquals("test", profile.getProperty("test"));
		profile.setProperty("test", "newvalue");
		assertEquals("newvalue", profile.getProperty("test"));
		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testAddRemoveIU() throws ProvisionException {
		IProfileRegistry registry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		assertTrue(profile.query(InstallableUnitQuery.ANY, new Collector(), null).isEmpty());
		profile.addInstallableUnit(createIU("test"));
		assertEquals(1, profile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		profile.removeInstallableUnit(createIU("test"));
		assertTrue(profile.query(InstallableUnitQuery.ANY, new Collector(), null).isEmpty());
		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testAddIUTwice() throws ProvisionException {
		IProfileRegistry registry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		assertTrue(profile.query(InstallableUnitQuery.ANY, new Collector(), null).isEmpty());
		profile.addInstallableUnit(createIU("test"));
		assertEquals(1, profile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		profile.addInstallableUnit(createIU("test"));
		assertEquals(1, profile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testAddRemoveIUProperty() throws ProvisionException {
		IProfileRegistry registry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		assertTrue(profile.query(InstallableUnitQuery.ANY, new Collector(), null).isEmpty());
		profile.addInstallableUnit(createIU("test"));
		assertNull(profile.getInstallableUnitProperty(createIU("test"), "test"));
		assertNull(profile.removeInstallableUnitProperty(createIU("test"), "test"));
		Properties iuProperties = new Properties();
		iuProperties.put("test", "test");
		profile.addInstallableUnitProperties(createIU("test"), iuProperties);
		assertEquals("test", profile.getInstallableUnitProperty(createIU("test"), "test"));
		profile.removeInstallableUnitProperty(createIU("test"), "test");
		assertNull(profile.getInstallableUnitProperty(createIU("test"), "test"));
		assertEquals(1, profile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testAvailable() throws ProvisionException {
		IProfileRegistry registry = (IProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		assertTrue(profile.available(InstallableUnitQuery.ANY, new Collector(), null).isEmpty());
		profile.addInstallableUnit(createIU("test"));
		assertEquals(1, profile.available(InstallableUnitQuery.ANY, new Collector(), null).size());
		profile.setSurrogateProfileHandler(new ISurrogateProfileHandler() {
			public IProfile createProfile(String id) {
				return null;
			}

			public boolean isSurrogate(IProfile profile) {
				return false;
			}

			public Collector queryProfile(IProfile profile, Query query, Collector collector, IProgressMonitor monitor) {
				return collector;
			}

			public boolean updateProfile(IProfile selfProfile) {
				return false;
			}
		});
		assertTrue(profile.available(InstallableUnitQuery.ANY, new Collector(), null).isEmpty());
		assertTrue(profile.snapshot().available(InstallableUnitQuery.ANY, new Collector(), null).isEmpty());
		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testNestedProfileStructure() {
		Properties properties = new Properties();
		properties.put("test", "test");
		IProfile parent = createProfile("parent", null, properties);
		IProfile child = createProfile("child", "parent");
		parent = getProfile("parent");
		assertTrue(parent.hasSubProfiles());
		assertFalse(child.hasSubProfiles());
		assertNotNull(parent.getLocalProperty("test"));
		assertNotNull(child.getProperty("test"));
		assertNotNull(child.getProperties().get("test"));
		assertNull(child.getLocalProperty("test"));
		assertNull(child.getLocalProperties().get("test"));

		assertTrue("Parentless profile should be a root.", parent.isRootProfile());
		assertFalse("Child profile should not be a root.", child.isRootProfile());
		assertTrue("Parent should be parent of child", child.getParentProfile().getProfileId().equals(parent.getProfileId()));
		assertTrue("Parent should have one child.", parent.getSubProfileIds().length == 1);
		assertTrue("Child should have no children.", child.getSubProfileIds().length == 0);

		IProfile grandchild = createProfile("grand", "child");
		child = getProfile("child");
		assertFalse("Grandchild profile should not be a root.", grandchild.isRootProfile());
		assertTrue("Parent should have one child.", parent.getSubProfileIds().length == 1);
		assertTrue("Child should have one child.", child.getSubProfileIds().length == 1);
		assertTrue("Grandparent of grandchild should be parent of child.", grandchild.getParentProfile().getParentProfile().getProfileId().equals(parent.getProfileId()));
	}

	/*	The test profile has the following structure and properties where
	 *  	id{x,y}  indicates a profile with id "id" and properties defined
	 *				 with keys "x" and "y"
	 *
	 *                                    grandchild00{foo}
	 *                                   /
	 *                                  /
	 *                      child0{foo} | --- grandchild01{}
	 *                     /             \
	 *					  /               \
	 *                   /                 grandchild01{bar}
	 *	parent{foo,bar} |				   
	 *                   \            grandchild10{foo}
	 *                    \          /
	 *                     child1{} |
	 *								 \
	 *                                grandchild11{}
	 *
	 */
	private static String parentId = "parent";
	private static String child0Id = "child0";
	private static String grandchild00Id = "grand00";
	private static String grandchild01Id = "grand01";
	private static String grandchild02Id = "grand02";
	private static String child1Id = "child1";
	private static String grandchild10Id = "grand10";
	private static String grandchild11Id = "grand11";

	private static String key = "org.eclipse.p2.foo";
	private static String parentValue = "parent";
	private static String child0Value = "child0";
	private static String grandchild00Value = "grandchild00";
	private static String grandchild02Value = "grandchild02";
	private static String grandchild10Value = "grandchild10";
	private static String otherKey = "org.eclipse.p2.bar";
	private static String otherValue = "other";

	// Create the profiles and test get after set
	// for associated properties.
	private IProfile[] createTestProfiles() {

		Map properties = new HashMap();

		properties.put(key, parentValue);
		properties.put(otherKey, otherValue);
		IProfile parent = createProfile(parentId, null, properties);
		properties.clear();
		assertTrue(parentValue.equals(parent.getProperty(key)));
		assertTrue(otherValue.equals(parent.getProperty(otherKey)));

		properties.put(key, child0Value);
		IProfile child0 = createProfile(child0Id, parentId, properties);
		properties.clear();
		assertTrue(child0Value.equals(child0.getProperty(key)));

		IProfile child1 = createProfile(child1Id, parentId, properties);
		// no value in child1

		properties.put(key, grandchild00Value);
		IProfile grandchild00 = createProfile(grandchild00Id, child0Id, properties);
		properties.clear();
		assertTrue(grandchild00Value.equals(grandchild00.getProperty(key)));

		IProfile grandchild01 = createProfile(grandchild01Id, child0Id);
		// no value in grandchild01

		properties.put(otherKey, grandchild02Value);
		IProfile grandchild02 = createProfile(grandchild02Id, child0Id, properties);
		properties.clear();
		assertTrue(grandchild02Value.equals(grandchild02.getProperty(otherKey)));

		properties.put(key, grandchild10Value);
		IProfile grandchild10 = createProfile(grandchild10Id, child1Id, properties);
		properties.clear();
		assertTrue(grandchild10Value.equals(grandchild10.getProperty(key)));

		IProfile grandchild11 = createProfile(grandchild11Id, child1Id);
		// no value in grandchild11

		parent = getProfile(parentId);
		child0 = getProfile(child0Id);
		child1 = getProfile(child1Id);
		grandchild00 = getProfile(grandchild00Id);
		grandchild01 = getProfile(grandchild01Id);
		grandchild02 = getProfile(grandchild02Id);
		grandchild10 = getProfile(grandchild10Id);
		grandchild11 = getProfile(grandchild11Id);

		IProfile[] profiles = {parent, child0, child1, grandchild00, grandchild01, grandchild02, grandchild10, grandchild11};
		return profiles;
	}

	public void testNestedProfileProperties() {
		validateProfiles(createTestProfiles());
	}

	public void validateProfiles(IProfile[] profiles) {
		IProfile parent = profiles[0];
		IProfile child0 = profiles[1];
		IProfile child1 = profiles[2];
		IProfile grandchild00 = profiles[3];
		IProfile grandchild01 = profiles[4];
		IProfile grandchild02 = profiles[5];
		IProfile grandchild10 = profiles[6];
		IProfile grandchild11 = profiles[7];

		assertTrue(parentId.equals(parent.getProfileId()));
		assertTrue("Profile should have 3 local properties", parent.getLocalProperties().size() == 2);
		assertTrue(parentValue.equals(parent.getProperty(key)));
		assertTrue(otherValue.equals(parent.getProperty(otherKey)));
		assertTrue("Parent should have 2 children.", parent.getSubProfileIds().length == 2);

		assertTrue(child0Id.equals(child0.getProfileId()));
		assertTrue("First Child should have 1 local properties.", child0.getLocalProperties().size() == 1);
		assertTrue(child0Value.equals(child0.getProperty(key)));
		assertTrue(otherValue.equals(child0.getProperty(otherKey)));
		assertTrue("First Child should have 3 children.", child0.getSubProfileIds().length == 3);

		assertTrue(child1Id.equals(child1.getProfileId()));
		assertTrue("Second Child should have 0 local properties.", child1.getLocalProperties().size() == 0);
		assertTrue(parentValue.equals(child1.getProperty(key)));
		assertTrue(otherValue.equals(child1.getProperty(otherKey)));
		assertTrue("Second Child should have 2 children.", child1.getSubProfileIds().length == 2);

		assertTrue(grandchild00Id.equals(grandchild00.getProfileId()));
		assertTrue("First Grandchild of first Child should have 1 property.", grandchild00.getLocalProperties().size() == 1);
		assertTrue(grandchild00Value.equals(grandchild00.getProperty(key)));
		assertTrue(otherValue.equals(grandchild00.getProperty(otherKey)));

		assertTrue(grandchild01Id.equals(grandchild01.getProfileId()));
		assertTrue("Second Grandchild of first Child should have 0 properties.", grandchild01.getLocalProperties().size() == 0);
		assertTrue(child0Value.equals(grandchild01.getProperty(key)));
		assertTrue(otherValue.equals(grandchild01.getProperty(otherKey)));

		assertTrue(grandchild02Id.equals(grandchild02.getProfileId()));
		assertTrue("Third Grandchild of first Child should have 1 property.", grandchild02.getLocalProperties().size() == 1);
		assertTrue(child0Value.equals(grandchild02.getProperty(key)));
		assertTrue(grandchild02Value.equals(grandchild02.getProperty(otherKey)));

		assertTrue(grandchild10Id.equals(grandchild10.getProfileId()));
		assertTrue("First Grandchild of second Child should have 1 property.", grandchild10.getLocalProperties().size() == 1);
		assertTrue(grandchild10Value.equals(grandchild10.getProperty(key)));
		assertTrue(otherValue.equals(grandchild10.getProperty(otherKey)));

		assertTrue(grandchild11Id.equals(grandchild11.getProfileId()));
		assertTrue("Second Grandchild of second Child should have 0 properties.", grandchild11.getLocalProperties().size() == 0);
		assertTrue(parentValue.equals(grandchild11.getProperty(key)));
		assertTrue(otherValue.equals(grandchild11.getProperty(otherKey)));
	}

	private static String PROFILE_TEST_TARGET = "profileTest";
	private static Version PROFILE_TEST_VERSION = new Version("0.0.1");

	private static String PROFILE_TEST_ELEMENT = "test";
	public static final String PROFILES_ELEMENT = "profiles"; //$NON-NLS-1$

	class ProfileStringWriter extends ProfileWriter {

		public ProfileStringWriter(ByteArrayOutputStream stream) throws IOException {
			super(stream, new ProcessingInstruction[] {ProcessingInstruction.makeClassVersionInstruction(PROFILE_TEST_TARGET, Profile.class, PROFILE_TEST_VERSION)});
		}

		public void writeTest(IProfile[] profiles) {
			start(PROFILE_TEST_ELEMENT);
			writeProfiles(profiles);
			end(PROFILE_TEST_ELEMENT);
			flush();
		}

		public void writeProfiles(IProfile[] profiles) {
			if (profiles.length > 0) {
				start(PROFILES_ELEMENT);
				attribute(COLLECTION_SIZE_ATTRIBUTE, profiles.length);
				for (int i = 0; i < profiles.length; i++) {
					writeProfile(profiles[i]);
				}
				end(PROFILES_ELEMENT);
			}
		}
	}

	class ProfileStringParser extends ProfileParser {

		private IProfile[] profiles = null;

		public ProfileStringParser(BundleContext context, String bundleId) {
			super(context, bundleId);
		}

		public void parse(String profileString) throws IOException {
			this.status = null;
			try {
				getParser();
				TestHandler testHandler = new TestHandler();
				xmlReader.setContentHandler(new ProfileDocHandler(PROFILE_TEST_ELEMENT, testHandler));
				xmlReader.parse(new InputSource(new StringReader(profileString)));
				if (isValidXML()) {
					profiles = testHandler.profiles;
				}
			} catch (SAXException e) {
				throw new IOException(e.getMessage());
			} catch (ParserConfigurationException e) {
				fail();
			}
		}

		private final class ProfileDocHandler extends DocHandler {

			public ProfileDocHandler(String rootName, RootHandler rootHandler) {
				super(rootName, rootHandler);
			}

			public void processingInstruction(String target, String data) throws SAXException {
				if (PROFILE_TEST_TARGET.equals(target)) {
					String clazz = extractPIClass(data);
					try {
						if (!Class.forName(clazz).equals(Profile.class)) {
							throw new SAXException("Wrong class '" + clazz + "' in processing instruction"); //$NON-NLS-1$//$NON-NLS-2$
						}
					} catch (ClassNotFoundException e) {
						throw new SAXException("Profile class '" + clazz + "' not found"); //$NON-NLS-1$//$NON-NLS-2$
					}

					Version profileTestVersion = extractPIVersion(target, data);
					if (!PROFILE_TEST_VERSION.equals(profileTestVersion)) {
						throw new SAXException("Bad profile test version.");
					}
				}
			}
		}

		private final class TestHandler extends RootHandler {

			private ProfilesHandler profilesHandler;
			IProfile[] profiles;

			protected void handleRootAttributes(Attributes attributes) {
			}

			public void startElement(String name, Attributes attributes) throws SAXException {
				if (PROFILES_ELEMENT.equals(name)) {
					if (profilesHandler == null) {
						profilesHandler = new ProfilesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML()) {
					if (profilesHandler != null) {
						profiles = profilesHandler.getProfiles();
					}
				}
			}

		}

		protected class ProfilesHandler extends AbstractHandler {

			private final Map profileHandlers;

			public ProfilesHandler(AbstractHandler parentHandler, Attributes attributes) {
				super(parentHandler, PROFILES_ELEMENT);
				String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
				profileHandlers = (size != null ? new HashMap(new Integer(size).intValue()) : new HashMap(4));
			}

			public IProfile[] getProfiles() {
				if (profileHandlers.isEmpty())
					return new IProfile[0];

				Map profileMap = new LinkedHashMap();
				for (Iterator it = profileHandlers.keySet().iterator(); it.hasNext();) {
					String profileId = (String) it.next();
					addProfile(profileId, profileMap);
				}

				return (IProfile[]) profileMap.values().toArray(new IProfile[profileMap.size()]);
			}

			private void addProfile(String profileId, Map profileMap) {
				if (profileMap.containsKey(profileId))
					return;

				ProfileHandler profileHandler = (ProfileHandler) profileHandlers.get(profileId);
				Profile parentProfile = null;

				String parentId = profileHandler.getParentId();
				if (parentId != null) {
					addProfile(parentId, profileMap);
					parentProfile = (Profile) profileMap.get(parentId);
				}

				Profile profile = new Profile(profileId, parentProfile, profileHandler.getProperties());
				profile.setTimestamp(profileHandler.getTimestamp());
				IInstallableUnit[] ius = profileHandler.getInstallableUnits();
				if (ius != null) {
					for (int i = 0; i < ius.length; i++) {
						IInstallableUnit iu = ius[i];
						profile.addInstallableUnit(iu);
						Map iuProperties = profileHandler.getIUProperties(iu);
						if (iuProperties != null) {
							for (Iterator it = iuProperties.entrySet().iterator(); it.hasNext();) {
								Entry entry = (Entry) it.next();
								String key = (String) entry.getKey();
								String value = (String) entry.getValue();
								profile.setInstallableUnitProperty(iu, key, value);
							}
						}
					}
				}
				profileMap.put(profileId, profile);
			}

			public void startElement(String name, Attributes attributes) {
				if (name.equals(PROFILE_ELEMENT)) {
					new ProfilesProfileHandler(this, attributes, profileHandlers);
				} else {
					invalidElement(name, attributes);
				}
			}
		}

		public class ProfilesProfileHandler extends ProfileHandler {
			private final Map profileHandlers;

			public ProfilesProfileHandler(ProfilesHandler profilesHandler, Attributes attributes, Map profileHandlers) {
				this.profileHandlers = profileHandlers;
				this.parentHandler = profilesHandler;
				xmlReader.setContentHandler(this);
				handleRootAttributes(attributes);
			}

			protected void finished() {
				if (isValidXML()) {
					profileHandlers.put(getProfileId(), this);
				}
			}
		}

		protected String getErrorMessage() {
			return "Error parsing profile string";
		}

		protected Object getRootObject() {
			Map result = new HashMap();
			for (int i = 0; i < profiles.length; i++) {
				result.put(profiles[i].getProfileId(), profiles[i]);
			}
			return result;
		}
	}

	public void testProfilePersistence() throws IOException {
		IProfile[] testProfiles = createTestProfiles();
		ByteArrayOutputStream output0 = new ByteArrayOutputStream(1492);
		ProfileStringWriter writer0 = new ProfileStringWriter(output0);
		writer0.writeTest(testProfiles);
		String profileText0 = output0.toString();
		output0.close();

		ProfileStringParser parser = new ProfileStringParser(TestActivator.context, TestActivator.PI_PROV_TESTS);
		parser.parse(profileText0);
		assertTrue("Error parsing test profile: " + parser.getStatus().getMessage(), parser.getStatus().isOK());
		Map profileMap = (Map) parser.getRootObject();
		IProfile parent = (IProfile) profileMap.get(parentId);
		IProfile child0 = (IProfile) profileMap.get(child0Id);
		IProfile child1 = (IProfile) profileMap.get(child1Id);
		IProfile grandchild00 = (IProfile) profileMap.get(grandchild00Id);
		IProfile grandchild01 = (IProfile) profileMap.get(grandchild01Id);
		IProfile grandchild02 = (IProfile) profileMap.get(grandchild02Id);
		IProfile grandchild10 = (IProfile) profileMap.get(grandchild10Id);
		IProfile grandchild11 = (IProfile) profileMap.get(grandchild11Id);
		IProfile[] profiles = {parent, child0, child1, grandchild00, grandchild01, grandchild02, grandchild10, grandchild11};
		validateProfiles(profiles);
		ByteArrayOutputStream output1 = new ByteArrayOutputStream(1492);
		ProfileStringWriter writer = new ProfileStringWriter(output1);

		writer.writeTest(profiles);
		String profileText1 = output1.toString();
		output1.close();
		assertTrue("Profile write after read after write produced different XML", profileText1.equals(profileText0));
	}
}
