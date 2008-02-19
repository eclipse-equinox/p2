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
package org.eclipse.equinox.internal.p2.engine;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;

public class ProfileWriter extends MetadataWriter implements ProfileXMLConstants {

	public ProfileWriter(OutputStream output, ProcessingInstruction[] processingInstructions) throws IOException {
		super(output, processingInstructions);
	}

	public void writeProfile(IProfile profile) {
		start(PROFILE_ELEMENT);
		attribute(ID_ATTRIBUTE, profile.getProfileId());
		IProfile parentProfile = profile.getParentProfile();
		if (parentProfile != null)
			attribute(PARENT_ID_ATTRIBUTE, parentProfile.getProfileId());
		writeProperties(profile.getLocalProperties());
		Collector collector = profile.query(InstallableUnitQuery.ANY, new Collector(), null);
		writeInstallableUnits(collector.iterator(), collector.size());
		writeInstallableUnitsProperties(collector.iterator(), collector.size(), profile);
		end(PROFILE_ELEMENT);
	}

	private void writeInstallableUnitsProperties(Iterator it, int size, IProfile profile) {
		if (size == 0)
			return;
		start(IUS_PROPERTIES_ELEMENT);
		attribute(COLLECTION_SIZE_ATTRIBUTE, size);
		while (it.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) it.next();
			Map properties = profile.getInstallableUnitProperties(iu);
			if (properties.isEmpty())
				continue;

			start(IU_PROPERTIES_ELEMENT);
			attribute(ID_ATTRIBUTE, iu.getId());
			attribute(VERSION_ATTRIBUTE, iu.getVersion().toString());
			writeProperties(properties);
			end(IU_PROPERTIES_ELEMENT);
		}
		end(IUS_PROPERTIES_ELEMENT);
	}
}
