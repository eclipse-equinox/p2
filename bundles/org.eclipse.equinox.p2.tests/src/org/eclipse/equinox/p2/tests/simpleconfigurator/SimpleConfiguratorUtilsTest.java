package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import org.eclipse.equinox.internal.simpleconfigurator.utils.BundleInfo;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorUtils;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleConfiguratorUtilsTest extends AbstractProvisioningTest {

	public void testParseBundleInfo() throws MalformedURLException {

		File baseFile = getTempFolder();
		URI baseURI = baseFile.toURI();

		String canonicalForm = "javax.servlet,2.4.0.v200806031604,plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo canonicalInfo = SimpleConfiguratorUtils.parseBundleInfoLine(canonicalForm, baseURI);
		File canonicalFile = new File(baseFile, "plugins/javax.servlet_2.4.0.v200806031604.jar");

		String line[] = new String[7];
		line[0] = "javax.servlet,2.4.0.v200806031604,file:plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[1] = "javax.servlet,2.4.0.v200806031604,plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[2] = "javax.servlet,2.4.0.v200806031604,file:plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[3] = "javax.servlet,2.4.0.v200806031604,file:plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[4] = "javax.servlet,2.4.0.v200806031604,file:" + canonicalFile.toString() + ",4,false";
		line[5] = "javax.servlet,2.4.0.v200806031604," + canonicalFile.toURL().toExternalForm() + ",4,false";
		line[6] = "javax.servlet,2.4.0.v200806031604," + canonicalFile.toURI().toString() + ",4,false";

		String relativeBundleLocation = "reference:file:plugins/javax.servlet_2.4.0.v200806031604.jar";
		String absoluteBundleLocation = "reference:" + canonicalFile.toURL().toExternalForm();

		for (int i = 0; i < line.length; i++) {
			BundleInfo info = SimpleConfiguratorUtils.parseBundleInfoLine(line[i], baseURI);
			assertEquals("[" + i + "]", canonicalInfo, info);
			if (info.getLocation().isAbsolute())
				assertEquals("[" + i + "]", absoluteBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
			else
				assertEquals("[" + i + "]", relativeBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
		}
	}

	public void testParseUNCBundleInfo() throws MalformedURLException {

		File baseFile = new File("\\\\127.0.0.1\\somefolder\\");
		URI baseURI = baseFile.toURI();

		String canonicalForm = "javax.servlet,2.4.0.v200806031604,plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo canonicalInfo = SimpleConfiguratorUtils.parseBundleInfoLine(canonicalForm, baseURI);
		File canonicalFile = new File(baseFile, "plugins/javax.servlet_2.4.0.v200806031604.jar");

		String line[] = new String[4];
		line[0] = "javax.servlet,2.4.0.v200806031604,file:plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[1] = "javax.servlet,2.4.0.v200806031604,plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[2] = "javax.servlet,2.4.0.v200806031604,file:plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[3] = "javax.servlet,2.4.0.v200806031604,file:plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";

		//TODO: we need to fix URI.resolve for UNC paths
		//line[4] = "javax.servlet,2.4.0.v200806031604," + canonicalFile.toURI().toString() + ",4,false";

		String relativeBundleLocation = "reference:file:plugins/javax.servlet_2.4.0.v200806031604.jar";
		String absoluteBundleLocation = "reference:" + canonicalFile.toURL().toExternalForm();

		for (int i = 0; i < line.length; i++) {
			BundleInfo info = SimpleConfiguratorUtils.parseBundleInfoLine(line[i], baseURI);
			assertEquals("[" + i + "]", canonicalInfo, info);
			if (info.getLocation().isAbsolute())
				assertEquals("[" + i + "]", absoluteBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
			else
				assertEquals("[" + i + "]", relativeBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
		}
	}
}
