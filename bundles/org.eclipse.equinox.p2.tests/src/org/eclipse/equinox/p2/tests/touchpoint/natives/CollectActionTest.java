/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.CollectAction;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class CollectActionTest extends AbstractProvisioningTest {

	public CollectActionTest(String name) {
		super(name);
	}

	public CollectActionTest() {
		super("");
	}

	public void testExecuteUndo() {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		IProfile profile = createProfile("test", profileProperties);

		//		File zipSource = getTestData("1.0", "/testData/nativeTouchpoint/a.zip");
		//		File zipTarget = new File(installFolder, "a.zip");
		//		copy("2.0", zipSource, zipTarget);

		InstallableUnitDescription iuDesc = new MetadataFactory.InstallableUnitDescription();
		iuDesc.setId("test");
		iuDesc.setVersion(DEFAULT_VERSION);
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("test", DEFAULT_VERSION);
		iuDesc.setArtifacts(new IArtifactKey[] {key});
		iuDesc.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDesc);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put(Collect.PARM_ARTIFACT_REQUESTS, new ArrayList<IArtifactRequest[]>());
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		parameters.put("iu", iu);
		touchpoint.initializeOperand(profile, parameters);
		parameters = Collections.unmodifiableMap(parameters);

		List<IArtifactRequest[]> requests = (List<IArtifactRequest[]>) parameters.get(Collect.PARM_ARTIFACT_REQUESTS);
		assertFalse(hasRequest(requests, key));
		CollectAction action = new CollectAction();
		action.execute(parameters);
		assertTrue(hasRequest(requests, key));
		// does nothing so should not alter parameters
		action.undo(parameters);
		assertTrue(hasRequest(requests, key));
	}

	private boolean hasRequest(List<IArtifactRequest[]> requests, IArtifactKey key) {
		for (IArtifactRequest[] request : requests) {
			for (int i = 0; i < request.length; i++) {
				if (key.equals(request[i].getArtifactKey()))
					return true;
			}
		}
		return false;
	}
}