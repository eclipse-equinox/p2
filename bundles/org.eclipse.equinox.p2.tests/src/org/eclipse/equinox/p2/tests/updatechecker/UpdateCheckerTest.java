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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.updatechecker;

import java.util.ArrayList;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateChecker;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Tests for API of {@link IUpdateChecker}.
 */
public class UpdateCheckerTest extends AbstractProvisioningTest {
	IProfile profile;
	IInstallableUnit toInstallIU, update;

	protected IUpdateChecker getChecker() {
		IUpdateChecker checker = getAgent().getService(IUpdateChecker.class);
		assertNotNull(checker);
		return checker;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		startBundle(TestActivator.getBundle("org.eclipse.equinox.p2.updatechecker"));
		String id = "toInstall." + getName();
		toInstallIU = createIU(id, Version.createOSGi(1, 0, 0));
		IUpdateDescriptor updateDescriptor = createUpdateDescriptor(id, Version.createOSGi(2, 0, 0));
		update = createIU(id, Version.createOSGi(2, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, true, updateDescriptor, null);

		IInstallableUnit[] allUnits = new IInstallableUnit[] {toInstallIU, update};
		IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstallIU};
		createTestMetdataRepository(allUnits);

		profile = createProfile("TestProfile." + getName());

		IDirector director = createDirector();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(toInstallArray);
		IStatus result = director.provision(request, null, null);
		assertTrue("setup.1", result.isOK());
	}

	public void testAddListener() {
		IUpdateChecker checker = getChecker();
		ArrayList<IInstallableUnit> toUpdate = new ArrayList<>();
		toUpdate.add(toInstallIU);
		TestUpdateListener listener = new TestUpdateListener(new UpdateEvent(profile.getProfileId(), toUpdate));
		checker.addUpdateCheck(profile.getProfileId(), QueryUtil.createIUAnyQuery(), IUpdateChecker.ONE_TIME_CHECK, 0, listener);
		listener.waitForEvent();
		listener.verify(1);

		//adding the listener again should not result in an event
		listener.reset();
		checker.addUpdateCheck(profile.getProfileId(), QueryUtil.createIUAnyQuery(), IUpdateChecker.ONE_TIME_CHECK, 0, listener);
		listener.waitForEvent();
		listener.verify(0);

		//removing and re-adding the listener should result in an event
		listener.reset();
		checker.removeUpdateCheck(listener);
		checker.addUpdateCheck(profile.getProfileId(), QueryUtil.createIUAnyQuery(), IUpdateChecker.ONE_TIME_CHECK, 0, listener);
		listener.waitForEvent();
		listener.verify(1);
	}
}