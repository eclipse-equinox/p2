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

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
import org.eclipse.equinox.p2.engine.Profile;

public class ProfileWriter extends XMLWriter implements ProfileXMLConstants {

	public ProfileWriter(OutputStream output, ProcessingInstruction[] processingInstructions) throws IOException {
		super(output, processingInstructions);
	}

	public void writeProfile(Profile profile) {
		start(PROFILE_ELEMENT);
		attribute(ID_ATTRIBUTE, profile.getProfileId());
		Profile parentProfile = profile.getParentProfile();
		if (parentProfile != null)
			attribute(PARENT_ID_ATTRIBUTE, parentProfile.getProfileId());
		writeProperties(profile.getLocalProperties());
		end(PROFILE_ELEMENT);
	}

	public void writeProfiles(Profile[] profiles) {
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
