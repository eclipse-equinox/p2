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
package org.eclipse.equinox.frameworkadmin.tests;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdmin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.osgi.framework.*;

public class TestRunningInstance extends AbstractFwkAdminTest {

	public TestRunningInstance(String name) {
		super(name);
	}

	public void testRunningInstance() throws BundleException {
		//TODO Commented out due to NPE failure on Windows on test machines only
		if (Platform.OS_WIN32.equals(Platform.getOS()))
			return;
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator m = fwkAdmin.getRunningManipulator();
		BundleInfo[] infos = m.getConfigData().getBundles();
		
		Bundle[] bundles = Activator.getContext().getBundles();
		
		assertEquals(bundles.length, infos.length);
		for (int i = 0; i < bundles.length; i++) {
			boolean found = false;
			for (int j = 0; j < infos.length && found == false; j++) {
				found = same(infos[j], bundles[i]);
			}
			if (found == false) {
				fail("Can't find: " + bundles[i]);
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
