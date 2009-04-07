/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.updatechecker;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateChecker;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Bundle;

/**
 * Tests for API of {@link IUpdateChecker}.
 */
public class UpdateCheckerTest extends AbstractProvisioningTest {
	IProfile profile;
	IInstallableUnit toInstallIU, update;

	protected IUpdateChecker getChecker() {
		IUpdateChecker checker = (IUpdateChecker) ServiceHelper.getService(Activator.context, IUpdateChecker.SERVICE_NAME);
		assertNotNull(checker);
		return checker;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TestActivator.getBundle("org.eclipse.equinox.p2.updatechecker").start(Bundle.START_TRANSIENT);
		String id = "toInstall." + getName();
		toInstallIU = createIU(id, new Version(1, 0, 0));
		IUpdateDescriptor updateDescriptor = createUpdateDescriptor(id, new Version(2, 0, 0));
		update = createIU(id, new Version(2, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, true, updateDescriptor, null);

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
		TestUpdateListener listener = new TestUpdateListener(new UpdateEvent(profile.getProfileId(), new IInstallableUnit[] {toInstallIU}));
		checker.addUpdateCheck(profile.getProfileId(), InstallableUnitQuery.ANY, IUpdateChecker.ONE_TIME_CHECK, 0, listener);
		listener.waitForEvent();
		listener.verify(1);

		//adding the listener again should not result in an event
		listener.reset();
		checker.addUpdateCheck(profile.getProfileId(), InstallableUnitQuery.ANY, IUpdateChecker.ONE_TIME_CHECK, 0, listener);
		listener.waitForEvent();
		listener.verify(0);

		//removing and re-adding the listener should result in an event
		listener.reset();
		checker.removeUpdateCheck(listener);
		checker.addUpdateCheck(profile.getProfileId(), InstallableUnitQuery.ANY, IUpdateChecker.ONE_TIME_CHECK, 0, listener);
		listener.waitForEvent();
		listener.verify(1);
	}
}