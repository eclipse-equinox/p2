/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.equinox.internal.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.CollectAction;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
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
		Map<String, String> profileProperties = new HashMap<>();
		File installFolder = getTempFolder();
		profileProperties.put(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
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

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(ActionConstants.PARM_AGENT, getAgent());
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put(Collect.PARM_ARTIFACT_REQUESTS, new ArrayList<>());
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
			for (IArtifactRequest req : request) {
				if (key.equals(req.getArtifactKey())) {
					return true;
				}
			}
		}
		return false;
	}
}