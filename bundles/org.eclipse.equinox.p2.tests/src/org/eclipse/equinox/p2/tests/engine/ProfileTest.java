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
package org.eclipse.equinox.p2.tests.engine;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.equinox.internal.p2.engine.ProfileParser;
import org.eclipse.equinox.internal.p2.engine.ProfileWriter;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.xml.sax.*;

/**
 * Simple test of the engine API.
 */
public class ProfileTest extends AbstractProvisioningTest {
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

	public void testNestedProfileStructure() {
		Profile parent = createProfile("parent");
		Profile child = createProfile("child", parent);
		assertTrue("Parentless profile should be a root.", parent.isARootProfile());
		assertFalse("Child profile should not be a root.", child.isARootProfile());
		assertTrue("Parent should be parent of child", child.getParentProfile() == parent);
		assertTrue("Parent should have one child.", parent.getSubProfiles().length == 1);
		assertTrue("Child should have no children.", child.getSubProfiles().length == 0);

		Profile grandchild = createProfile("grand", child);
		assertFalse("Grandchild profile should not be a root.", grandchild.isARootProfile());
		assertTrue("Parent should have one child.", parent.getSubProfiles().length == 1);
		assertTrue("Child should have one child.", child.getSubProfiles().length == 1);
		assertTrue("Grandparent of grandchild should be parent of child.", grandchild.getParentProfile().getParentProfile() == parent);
		try {
			// Add a subprofile with id collision.
			createProfile("grand", child);
		} catch (IllegalArgumentException expected) {
			assertTrue("Child should have one child.", child.getSubProfiles().length == 1);
			return;
		}
		fail();
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
	private Profile createTestProfile() {
		Profile parent = createProfile(parentId);
		parent.setValue(key, parentValue);
		assertTrue(parentValue.equals(parent.getValue(key)));
		parent.setValue(otherKey, otherValue);
		assertTrue(otherValue.equals(parent.getValue(otherKey)));

		Profile child0 = createProfile(child0Id, parent);
		child0.setValue(key, child0Value);
		assertTrue(child0Value.equals(child0.getValue(key)));

		Profile child1 = createProfile(child1Id, parent);
		// no value in child1

		Profile grandchild00 = createProfile(grandchild00Id, child0);
		grandchild00.setValue(key, grandchild00Value);
		assertTrue(grandchild00Value.equals(grandchild00.getValue(key)));

		Profile grandchild01 = createProfile(grandchild01Id, child0);
		// no value in grandchild01

		Profile grandchild02 = createProfile(grandchild02Id, child0);
		grandchild02.setValue(otherKey, grandchild02Value);
		assertTrue(grandchild02Value.equals(grandchild02.getValue(otherKey)));

		Profile grandchild10 = createProfile(grandchild10Id, child1);
		grandchild10.setValue(key, grandchild10Value);
		assertTrue(grandchild10Value.equals(grandchild10.getValue(key)));

		Profile grandchild11 = createProfile(grandchild11Id, child1);
		// no value in grandchild11

		return parent;
	}

	public void testNestedProfileProperties() {
		validateProfile(createTestProfile());
	}

	public void validateProfile(Profile profile) {
		assertTrue(parentId.equals(profile.getProfileId()));
		assertTrue("Profile should have 3 local properties", profile.getProperties().size() == 3);
		assertTrue(parentValue.equals(profile.getValue(key)));
		assertTrue(otherValue.equals(profile.getValue(otherKey)));
		assertTrue("Parent should have 2 children.", profile.getSubProfiles().length == 2);

		Profile child0 = profile.getSubProfiles()[0];
		assertTrue(child0Id.equals(child0.getProfileId()));
		assertTrue("First Child should have 2 local properties.", child0.getProperties().size() == 2);
		assertTrue(child0Value.equals(child0.getValue(key)));
		assertTrue(otherValue.equals(child0.getValue(otherKey)));
		assertTrue("First Child should have 3 children.", child0.getSubProfiles().length == 3);
		Profile grandchild00 = child0.getSubProfiles()[0];
		Profile grandchild01 = child0.getSubProfiles()[1];
		Profile grandchild02 = child0.getSubProfiles()[2];

		Profile child1 = profile.getSubProfiles()[1];
		assertTrue(child1Id.equals(child1.getProfileId()));
		assertTrue("Second Child should have 1 local property.", child1.getProperties().size() == 1);
		assertTrue(parentValue.equals(child1.getValue(key)));
		assertTrue(otherValue.equals(child1.getValue(otherKey)));
		assertTrue("Second Child should have 2 children.", child1.getSubProfiles().length == 2);
		Profile grandchild10 = child1.getSubProfiles()[0];
		Profile grandchild11 = child1.getSubProfiles()[1];

		assertTrue(grandchild00Id.equals(grandchild00.getProfileId()));
		assertTrue("First Grandchild of first Child should have 2 properties.", grandchild00.getProperties().size() == 2);
		assertTrue(grandchild00Value.equals(grandchild00.getValue(key)));
		assertTrue(otherValue.equals(grandchild00.getValue(otherKey)));

		assertTrue(grandchild01Id.equals(grandchild01.getProfileId()));
		assertTrue("Second Grandchild of first Child should have 1 property.", grandchild01.getProperties().size() == 1);
		assertTrue(child0Value.equals(grandchild01.getValue(key)));
		assertTrue(otherValue.equals(grandchild01.getValue(otherKey)));

		assertTrue(grandchild02Id.equals(grandchild02.getProfileId()));
		assertTrue("Third Grandchild of first Child should have 2 properties.", grandchild02.getProperties().size() == 2);
		assertTrue(child0Value.equals(grandchild02.getValue(key)));
		assertTrue(grandchild02Value.equals(grandchild02.getValue(otherKey)));

		assertTrue(grandchild10Id.equals(grandchild10.getProfileId()));
		assertTrue("First Grandchild of second Child should have 2 properties.", grandchild10.getProperties().size() == 2);
		assertTrue(grandchild10Value.equals(grandchild10.getValue(key)));
		assertTrue(otherValue.equals(grandchild10.getValue(otherKey)));

		assertTrue(grandchild11Id.equals(grandchild11.getProfileId()));
		assertTrue("Second Grandchild of second Child should have 1 property.", grandchild11.getProperties().size() == 1);
		assertTrue(parentValue.equals(grandchild11.getValue(key)));
		assertTrue(otherValue.equals(grandchild11.getValue(otherKey)));
	}

	private static String PROFILE_TEST_TARGET = "profileTest";
	private static Version PROFILE_TEST_VERSION = new Version("0.0.1");

	private static String PROFILE_TEST_ELEMENT = "test";

	class ProfileStringWriter extends ProfileWriter {

		public ProfileStringWriter(ByteArrayOutputStream stream) throws IOException {
			super(stream, new ProcessingInstruction[] {ProcessingInstruction.makeClassVersionInstruction(PROFILE_TEST_TARGET, Profile.class, PROFILE_TEST_VERSION)});
		}

		public void writeTest(Profile profile) {
			start(PROFILE_TEST_ELEMENT);
			writeProfile(profile);
			end(PROFILE_TEST_ELEMENT);
		}
	}

	class ProfileStringParser extends ProfileParser {

		public ProfileStringParser(BundleContext context, String bundleId) {
			super(context, bundleId);
			// TODO Auto-generated constructor stub
		}

		public void parse(String profileString) throws IOException {
			this.status = null;
			try {
				getParser();
				TestHandler testHandler = new TestHandler();
				xmlReader.setContentHandler(new ProfileDocHandler(PROFILE_TEST_ELEMENT, testHandler));
				xmlReader.parse(new InputSource(new StringReader(profileString)));
				if (isValidXML()) {
					theProfile = testHandler.getProfile();
				}
			} catch (SAXException e) {
				throw new IOException(e.getMessage());
			} catch (ParserConfigurationException e) {
				fail();
			}
		}

		private Profile theProfile = null;

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

			private ProfileHandler profileHandler = null;

			private Profile profile = null;
			private List singleton = new ArrayList(1);

			public TestHandler() {
				super();
			}

			public Profile getProfile() {
				return profile;
			}

			protected void handleRootAttributes(Attributes attributes) {
				String[] values = parseAttributes(attributes, noAttributes, noAttributes);
			}

			public void startElement(String name, Attributes attributes) {
				if (PROFILE_ELEMENT.equals(name)) {
					if (profileHandler == null) {
						profileHandler = new ProfileHandler(this, attributes, null, singleton);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML()) {
					if (profileHandler != null && singleton.size() == 1) {
						profile = (Profile) singleton.get(0);
					}
				}
			}
		}

		protected String getErrorMessage() {
			return "Error parsing profile string";
		}

		protected Object getRootObject() {
			return theProfile;
		}
	}

	public void testProfilePersistence() throws IOException {
		Profile profile0 = createTestProfile();
		ByteArrayOutputStream output0 = new ByteArrayOutputStream(1492);
		ProfileStringWriter writer0 = new ProfileStringWriter(output0);
		writer0.writeTest(profile0);
		String profileText0 = output0.toString();
		output0.close();

		ProfileStringParser parser = new ProfileStringParser(TestActivator.context, TestActivator.PI_PROV_TESTS);
		parser.parse(profileText0);
		assertTrue("Error parsing test profile: " + parser.getStatus().getMessage(), parser.getStatus().isOK());
		Profile profile1 = (Profile) parser.getRootObject();
		validateProfile(profile1);
		ByteArrayOutputStream output1 = new ByteArrayOutputStream(1492);
		ProfileStringWriter writer = new ProfileStringWriter(output1);
		writer.writeTest(profile1);
		String profileText1 = output1.toString();
		output1.close();
		assertTrue("Profile write after read after write produced different XML", profileText1.equals(profileText0));
	}

}
