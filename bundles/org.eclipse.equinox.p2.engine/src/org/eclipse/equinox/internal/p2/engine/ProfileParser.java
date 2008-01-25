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

import java.util.ArrayList;
import java.util.List;
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

		List profyles = null;
		Profile currentProfile = null;

		private String profileId = null;

		private PropertiesHandler propertiesHandler = null;
		private ProfilesHandler profilesHandler = null;

		public ProfileHandler(AbstractHandler parentHandler, Attributes attributes, Profile parent, List profiles) {
			super(parentHandler, PROFILE_ELEMENT);
			profileId = parseRequiredAttributes(attributes, required)[0];
			this.profyles = profiles;
			currentProfile = new Profile((profileId != null ? profileId : "##invalid##"), parent); //$NON-NLS-1$
			profiles.add(currentProfile);
		}

		public void startElement(String name, Attributes attributes) {
			if (PROPERTIES_ELEMENT.equals(name)) {
				if (propertiesHandler == null) {
					propertiesHandler = new PropertiesHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else if (name.equals(PROFILES_ELEMENT)) {
				if (profilesHandler == null) {
					profilesHandler = new ProfilesHandler(this, attributes, currentProfile);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else {
				invalidElement(name, attributes);
			}
		}

		protected void finished() {
			if (isValidXML() && currentProfile != null) {
				if (propertiesHandler != null) {
					currentProfile.internalAddProperties(propertiesHandler.getProperties());
				}
			}
		}
	}

	protected class ProfilesHandler extends AbstractHandler {

		private Profile parentProfile = null;
		private List profyles = null;

		public ProfilesHandler(AbstractHandler parentHandler, Attributes attributes, Profile parent) {
			super(parentHandler, PROFILES_ELEMENT);
			this.parentProfile = parent;
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			profyles = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public Profile[] getProfiles() {
			return (Profile[]) profyles.toArray(new Profile[profyles.size()]);
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(PROFILE_ELEMENT)) {
				new ProfileHandler(this, attributes, parentProfile, profyles);
			} else {
				invalidElement(name, attributes);
			}
		}
	}
}
