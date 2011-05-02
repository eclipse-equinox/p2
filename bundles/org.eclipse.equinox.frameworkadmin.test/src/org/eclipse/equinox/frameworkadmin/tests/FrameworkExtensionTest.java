/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.osgi.framework.Constants;

public class FrameworkExtensionTest extends FwkAdminAndSimpleConfiguratorTest {

	public FrameworkExtensionTest(String name) {
		super(name);
	}
	
	public void testAddRemoveFrameworkExtension() throws Exception  {
		Manipulator manipulator = createMinimalConfiguration(FrameworkExtensionTest.class.getName());
		BundleInfo bundleInfo = new BundleInfo("dummy.frameworkextension", "1.0.0", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/dummy.frameworkextension_1.0.0.jar"))), 4, false);
		bundleInfo.setFragmentHost(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		manipulator.getConfigData().addBundle(bundleInfo);
		manipulator.save(false);
		assertContent(getBundleTxt(), "dummy.frameworkextension");
		assertPropertyContains(getConfigIni(),"osgi.framework.extensions", "dummy.frameworkextension");
		assertNotPropertyContains(getConfigIni(),"osgi.bundles", "dummy.frameworkextension");
		
		BundleInfo basicBundleInfo = new BundleInfo("dummy.frameworkextension", "1.0.0", null, -1, false);
		manipulator.getConfigData().removeBundle(basicBundleInfo);
		manipulator.save(false);		
		assertNotContent(getBundleTxt(), "dummy.frameworkextension");
		assertNotPropertyContains(getConfigIni(),"osgi.framework.extensions", "dummy.frameworkextension");
	}

}
