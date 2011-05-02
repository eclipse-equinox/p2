/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.util.Collection;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.eclipse.DefaultCUsAction;

@SuppressWarnings({"unchecked"})
public class DefaultCUsActionTest extends ActionTest {

	private Version version = Version.create("1.0.0"); //$NON-NLS-1$

	public void testAll() throws Exception {
		testAction = new DefaultCUsAction(publisherInfo, flavorArg, 4 /*start level*/, true /*start*/);

		setupPublisherResult();
		setupPublisherInfo();
		assertEquals(Status.OK_STATUS, testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor()));
		verifyDefaultCUs();
		debug("Completed DefaultCUsActionTest."); //$NON-NLS-1$
	}

	private void verifyDefaultCUs() {
		Collection<IInstallableUnit> ius = publisherResult.getIUs(null, null);
		assertTrue(ius.size() == 3);
		InstallableUnitFragment iuf1 = new InstallableUnitFragment();
		iuf1.setId(flavorArg + ".source.default"); //$NON-NLS-1$
		iuf1.setVersion(version);
		assertTrue(ius.contains(iuf1));

		InstallableUnitFragment iuf2 = new InstallableUnitFragment();
		iuf2.setId(flavorArg + ".org.eclipse.update.feature.default"); //$NON-NLS-1$
		iuf2.setVersion(version);
		assertTrue(ius.contains(iuf2));

		InstallableUnitFragment iuf3 = new InstallableUnitFragment();
		iuf3.setId(flavorArg + ".osgi.bundle.default"); //$NON-NLS-1$
		iuf3.setVersion(version);
		assertTrue(ius.contains(iuf3));
	}
}
