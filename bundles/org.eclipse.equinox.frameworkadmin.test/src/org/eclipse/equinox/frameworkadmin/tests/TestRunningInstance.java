/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.frameworkadmin.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdmin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.junit.Test;
import org.osgi.framework.*;

public class TestRunningInstance extends AbstractFwkAdminTest {

	@Test
	public void testRunningInstance() throws BundleException {
		// TODO Commented out due to NPE failure on Windows on test machines only
		if (Platform.OS_WIN32.equals(Platform.getOS()))
			return;
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator m = fwkAdmin.getRunningManipulator();
		BundleInfo[] infos = m.getConfigData().getBundles();

		Bundle[] bundles = Activator.getContext().getBundles();

		assertEquals(bundles.length, infos.length);
		for (Bundle bundle : bundles) {
			boolean found = false;
			for (int j = 0; j < infos.length && found == false; j++) {
				found = same(infos[j], bundle);
			}
			if (found == false) {
				fail("Can't find: " + bundle);
			}
		}
	}

	private boolean same(BundleInfo info, Bundle bundle) {
		if (info.getSymbolicName().equals(bundle.getSymbolicName())) {
			if (new Version(bundle.getHeaders().get(Constants.BUNDLE_VERSION)).equals(new Version(info.getVersion())))
				return true;
		}
		return false;
	}
}
