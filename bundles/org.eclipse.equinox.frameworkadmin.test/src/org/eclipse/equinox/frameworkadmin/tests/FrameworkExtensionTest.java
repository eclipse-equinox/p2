package org.eclipse.equinox.frameworkadmin.tests;

import org.osgi.framework.Constants;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;

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
