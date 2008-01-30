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
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import org.eclipse.equinox.internal.p2.persistence.XMLParser;
import org.eclipse.equinox.p2.engine.Profile;
import org.osgi.framework.BundleContext;
import org.xml.sax.Attributes;

/**
 *	An abstract XML parser class for parsing profiles as written by the ProfileWriter.
 */
public abstract class ProfileParser extends XMLParser implements ProfileXMLConstants {

	public ProfileParser(BundleContext context, String bundleId) {
		super(context, bundleId);
	}

	protected class ProfileHandler extends AbstractHandler {

		private final String[] required = new String[] {ID_ATTRIBUTE};

		private final Map profileHandlers;
		private String profileId = null;
		private String parentId;
		private PropertiesHandler propertiesHandler = null;

		public ProfileHandler(AbstractHandler parentHandler, Attributes attributes, Map profileHandlers) {
			super(parentHandler, PROFILE_ELEMENT);
			profileId = parseRequiredAttributes(attributes, required)[0];
			parentId = parseOptionalAttribute(attributes, PARENT_ID_ATTRIBUTE);
			this.profileHandlers = profileHandlers;
		}

		public void startElement(String name, Attributes attributes) {
			if (PROPERTIES_ELEMENT.equals(name)) {
				if (propertiesHandler == null) {
					propertiesHandler = new PropertiesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else {
				invalidElement(name, attributes);
			}
		}

		protected void finished() {
			if (isValidXML()) {
				profileHandlers.put(profileId, this);
			}
		}

		public String getProfileId() {
			return profileId;
		}

		public String getParentId() {
			return parentId;
		}

		public Map getProperties() {
			if (propertiesHandler == null)
				return null;
			return propertiesHandler.getProperties();
		}
	}

	protected class ProfilesHandler extends AbstractHandler {

		private Map profileHandlers = null;
		private Map profiles;

		public ProfilesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, PROFILES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			profileHandlers = (size != null ? new HashMap(new Integer(size).intValue()) : new HashMap(4));
		}

		public Profile[] getProfiles() {
			if (profileHandlers.isEmpty())
				return new Profile[0];

			profiles = new HashMap();
			for (Iterator it = profileHandlers.keySet().iterator(); it.hasNext();) {
				String profileId = (String) it.next();
				addProfile(profileId);
			}

			return (Profile[]) profiles.values().toArray(new Profile[profiles.size()]);
		}

		private void addProfile(String profileId) {
			if (profiles.containsKey(profileId))
				return;

			ProfileHandler profileHandler = (ProfileHandler) profileHandlers.get(profileId);
			Profile parentProfile = null;
			String parentId = profileHandler.parentId;
			if (parentId != null) {
				addProfile(parentId);
				parentProfile = (Profile) profiles.get(parentId);
			}
			Profile profile = new Profile(profileId, parentProfile, profileHandler.getProperties());
			profiles.put(profileId, profile);
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(PROFILE_ELEMENT)) {
				new ProfileHandler(this, attributes, profileHandlers);
			} else {
				invalidElement(name, attributes);
			}
		}
	}
}
