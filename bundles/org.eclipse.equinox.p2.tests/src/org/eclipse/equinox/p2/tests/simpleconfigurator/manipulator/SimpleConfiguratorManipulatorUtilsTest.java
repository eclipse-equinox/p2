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
		String expectedLine = "javax.servlet,2.4.0.v200806031604,plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo bundleInfo = new BundleInfo("javax.servlet", "2.4.0.v200806031604", new URI("plugins/javax.servlet_2.4.0.v200806031604.jar"), 4, false);
		String line = SimpleConfiguratorManipulatorUtils.createBundleInfoLine(bundleInfo);
		assertEquals(expectedLine, line);
		assertEquals(bundleInfo, SimpleConfiguratorUtils.parseBundleInfoLine(line, null));
	}

	public void testWriteBundleInfoLineWithComma() throws URISyntaxException, IOException {
		String expectedLine = "javax.servlet,2.4.0.v200806031604,plugin%2Cs/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo bundleInfo = new BundleInfo("javax.servlet", "2.4.0.v200806031604", new URI("plugin,s/javax.servlet_2.4.0.v200806031604.jar"), 4, false);
		String line = SimpleConfiguratorManipulatorUtils.createBundleInfoLine(bundleInfo);
		assertEquals(expectedLine, line);
		assertEquals(bundleInfo, SimpleConfiguratorUtils.parseBundleInfoLine(line, null));
	}

	public void testWriteBundleInfoLineWithSpace() throws URISyntaxException, IOException {
		String expectedLine = "javax.servlet,2.4.0.v200806031604,plugin%20s/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo bundleInfo = new BundleInfo("javax.servlet", "2.4.0.v200806031604", new URI("plugin%20s/javax.servlet_2.4.0.v200806031604.jar"), 4, false);
		String line = SimpleConfiguratorManipulatorUtils.createBundleInfoLine(bundleInfo);
		assertEquals(expectedLine, line);
		assertEquals(bundleInfo, SimpleConfiguratorUtils.parseBundleInfoLine(line, null));
	}

}
