/*******************************************************************************
 *  Copyright (c) 2007, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
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
		IProfileRegistry registry = getProfileRegistry();
		assertNull(registry.getProfile(PROFILE_NAME));
		Map<String, String> properties = new HashMap<String, String>();
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
		IProfileRegistry registry = getProfileRegistry();
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		assertTrue(profile.query(QueryUtil.createIUAnyQuery(), null).isEmpty());
		profile.addInstallableUnit(createIU("test"));
		assertEquals(1, queryResultSize(profile.query(QueryUtil.createIUAnyQuery(), null)));
		profile.removeInstallableUnit(createIU("test"));
		assertTrue(profile.query(QueryUtil.createIUAnyQuery(), null).isEmpty());
		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testAddIUTwice() throws ProvisionException {
		IProfileRegistry registry = getProfileRegistry();
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		assertTrue(profile.query(QueryUtil.createIUAnyQuery(), null).isEmpty());
		profile.addInstallableUnit(createIU("test"));
		assertEquals(1, queryResultSize(profile.query(QueryUtil.createIUAnyQuery(), null)));
		profile.addInstallableUnit(createIU("test"));
		assertEquals(1, queryResultSize(profile.query(QueryUtil.createIUAnyQuery(), null)));
		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testAddRemoveIUProperty() throws ProvisionException {
		IProfileRegistry registry = getProfileRegistry();
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		assertTrue(profile.query(QueryUtil.createIUAnyQuery(), null).isEmpty());
		profile.addInstallableUnit(createIU("test"));
		assertNull(profile.getInstallableUnitProperty(createIU("test"), "test"));
		assertNull(profile.removeInstallableUnitProperty(createIU("test"), "test"));
		Map<String, String> iuProperties = new HashMap<String, String>();
		iuProperties.put("test", "test");
		profile.addInstallableUnitProperties(createIU("test"), iuProperties);
		assertEquals("test", profile.getInstallableUnitProperty(createIU("test"), "test"));
		profile.removeInstallableUnitProperty(createIU("test"), "test");
		assertNull(profile.getInstallableUnitProperty(createIU("test"), "test"));
		assertEquals(1, queryResultSize(profile.query(QueryUtil.createIUAnyQuery(), null)));
		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testAvailable() throws ProvisionException {
		IProfileRegistry registry = getProfileRegistry();
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		assertTrue(profile.available(QueryUtil.createIUAnyQuery(), null).isEmpty());
		profile.addInstallableUnit(createIU("test"));
		assertEquals(1, queryResultSize(profile.available(QueryUtil.createIUAnyQuery(), null)));
		profile.setSurrogateProfileHandler(new ISurrogateProfileHandler() {
			public IProfile createProfile(String id) {
				return null;
			}

			public boolean isSurrogate(IProfile profile) {
				return false;
			}

			public IQueryResult queryProfile(IProfile profile, IQuery query, IProgressMonitor monitor) {
				return new Collector();
			}

		});
		assertTrue(profile.available(QueryUtil.createIUAnyQuery(), null).isEmpty());
		assertTrue(profile.snapshot().available(QueryUtil.createIUAnyQuery(), null).isEmpty());
		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	private static String PROFILE_TEST_TARGET = "profileTest";
	private static Version PROFILE_TEST_VERSION = Version.create("0.0.1");

	private static String PROFILE_TEST_ELEMENT = "test";
	public static final String PROFILES_ELEMENT = "profiles"; //$NON-NLS-1$

	class ProfileStringWriter extends ProfileWriter {

		public ProfileStringWriter(ByteArrayOutputStream stream) throws IOException {
			super(stream, new ProcessingInstruction[] {ProcessingInstruction.makeTargetVersionInstruction(PROFILE_TEST_TARGET, PROFILE_TEST_VERSION)});
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

			public void startElement(String name, Attributes attributes) {
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
				profileHandlers = (size != null ? new HashMap(Integer.parseInt(size)) : new HashMap(4));
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

				Profile profile = new Profile(getAgent(), profileId, parentProfile, profileHandler.getProperties());
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
}
