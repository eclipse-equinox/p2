package org.eclipse.equinox.p2.tests.simpleconfigurator.manipulator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.equinox.internal.simpleconfigurator.manipulator.SimpleConfiguratorManipulatorUtils;
import org.eclipse.equinox.internal.simpleconfigurator.utils.BundleInfo;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorUtils;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleConfiguratorManipulatorUtilsTest extends AbstractProvisioningTest {

	public void testWriteBundleInfoLine() throws URISyntaxException, IOException {
		String expectedLine1 = "javax.servlet,2.4.0.v200806031604,plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo bundleInfo1 = new BundleInfo("javax.servlet", "2.4.0.v200806031604", new URI("plugins/javax.servlet_2.4.0.v200806031604.jar"), 4, false);
		String line1 = SimpleConfiguratorManipulatorUtils.createBundleInfoLine(bundleInfo1);
		assertEquals(expectedLine1, line1);
		assertEquals(bundleInfo1, SimpleConfiguratorUtils.parseBundleInfoLine(line1, null));

		String expectedLine2 = "javax.servlet,2.4.0.v200806031604,plugin%2Cs/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo bundleInfo2 = new BundleInfo("javax.servlet", "2.4.0.v200806031604", new URI("plugin,s/javax.servlet_2.4.0.v200806031604.jar"), 4, false);
		String line2 = SimpleConfiguratorManipulatorUtils.createBundleInfoLine(bundleInfo2);
		assertEquals(expectedLine2, line2);
		assertEquals(bundleInfo2, SimpleConfiguratorUtils.parseBundleInfoLine(line2, null));
	}
}
