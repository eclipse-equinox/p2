/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM Corporation - on-going maintenance
******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestData;

/**
 * Tests the product file advice
 */
public class ProductFileAdviceTest extends AbstractProvisioningTest {

	String productFileLocation = null;
	ProductFile productFile = null;
	ProductFileAdvice productFileAdviceWin32 = null;

	String productFileLocation2 = null;
	ProductFile productFile2 = null;
	ProductFileAdvice productFileAdvice2 = null;

	String configFile = "/org.eclipse.equinox.p2.tests/testData/ProductActionTest/productWithConfig/config.ini";

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		productFileLocation = TestData.getFile("ProductActionTest/productWithConfig", "sample.product").toString();
		productFile = new ProductFile(productFileLocation);
		productFileAdviceWin32 = new ProductFileAdvice(productFile, "x86.win32.*");

		productFileLocation2 = TestData.getFile("ProductActionTest", "productFileActionTest.product").toString();
		productFile2 = new ProductFile(productFileLocation2);
		productFileAdvice2 = new ProductFileAdvice(productFile2, "x86.win32.*");
	}

	/**
	 * Test method for {@link org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice#getProgramArguments()}.
	 */
	public void testGetProgramArguments() {
		String[] programArgumentsWin32 = productFileAdviceWin32.getProgramArguments();
		assertEquals("1.0", 3, programArgumentsWin32.length);
		assertEquals("1.1", "programArg", programArgumentsWin32[0]);
		assertEquals("1.2", "-name", programArgumentsWin32[1]);
		assertEquals("1.3", "My Name", programArgumentsWin32[2]);

		String[] programArguments2 = productFileAdvice2.getProgramArguments();
		assertEquals("2.0", 2, programArguments2.length);
		assertEquals("2.1", "-product", programArguments2[0]);
		assertEquals("2.2", "com,ma", programArguments2[1]);
	}

	/**
	 * Test method for {@link org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice#getVMArguments()}.
	 */
	public void testGetVMArguments() {
		String[] vmArgumentsWin32 = productFileAdviceWin32.getVMArguments();
		assertEquals("1.0", 2, vmArgumentsWin32.length);
		assertEquals("1.1", "vmArg", vmArgumentsWin32[0]);
		assertEquals("1.2", "-Dfoo=b a r", vmArgumentsWin32[1]);

		String[] vmArguments2 = productFileAdvice2.getVMArguments();
		assertEquals("2.0", 1, vmArguments2.length);
		assertEquals("2.1", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8272", vmArguments2[0]);
	}

	/**
	 * Test method for {@link org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice#getBundles()}.
	 */
	public void testGetBundles() {
		BundleInfo[] bundles = productFileAdviceWin32.getBundles();
		assertEquals("1.0", 4, bundles.length);
		for (int i = 0; i < 4; i++) {
			if (bundles[i].getSymbolicName().equals("org.eclipse.equinox.common")) {
				assertEquals(2, bundles[i].getStartLevel());
				assertEquals(true, bundles[i].isMarkedAsStarted());
			} else if (bundles[i].getSymbolicName().equals("org.eclipse.update.configurator")) {
				assertEquals(3, bundles[i].getStartLevel());
				assertEquals(true, bundles[i].isMarkedAsStarted());
			} else if (bundles[i].getSymbolicName().equals("org.eclipse.core.runtime")) {
				// nothing yet
			} else if (bundles[i].getSymbolicName().equals("org.eclipse.swt.win32.win32.x86")) {
				assertEquals(-1, bundles[i].getStartLevel());
				assertEquals(false, bundles[i].isMarkedAsStarted());
			} else
				fail("unknown bundle: " + bundles[i].getSymbolicName());
		}

		bundles = productFileAdvice2.getBundles();
		assertEquals("2.0", 1, bundles.length);
		for (int i = 0; i < 1; i++) {
			if (bundles[i].getSymbolicName().equals("org.eclipse.core.commands")) {
				assertTrue("2.1", bundles[i].getStartLevel() == 2);
				assertTrue("2.2", bundles[i].isMarkedAsStarted() == false);
			} else
				fail("unknown bundle: " + bundles[i].getSymbolicName());
		}
	}

	public void testBoundedVersionConfigurations() throws Exception {
		String location = TestData.getFile("ProductActionTest", "boundedVersionConfigurations.product").toString();
		ProductFile product = new ProductFile(location);
		ProductFileAdvice advice = new ProductFileAdvice(product, "x86.win32.*");

		BundleInfo[] bundles = advice.getBundles();
		assertEquals("1.0", 2, bundles.length);
		for (int i = 0; i < 2; i++) {
			if (bundles[i].getSymbolicName().equals("org.eclipse.core.commands")) {
				assertEquals(2, bundles[i].getStartLevel());
				assertEquals(true, bundles[i].isMarkedAsStarted());
			} else if (bundles[i].getSymbolicName().equals("org.eclipse.core.runtime")) {
				assertTrue("1.1", bundles[i].getStartLevel() == 2);
				assertTrue("1.2", bundles[i].isMarkedAsStarted() == true);
			} else
				fail("unknown bundle: " + bundles[i].getSymbolicName());
		}
	}

	public void testUnboundedVersionConfigurations() throws Exception {
		String location = TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString();
		ProductFile product = new ProductFile(location);
		ProductFileAdvice advice = new ProductFileAdvice(product, "x86.win32.*");

		BundleInfo[] bundles = advice.getBundles();
		assertEquals("1.0", 2, bundles.length);
		for (int i = 0; i < 2; i++) {
			if (bundles[i].getSymbolicName().equals("org.eclipse.core.commands")) {
				assertEquals(2, bundles[i].getStartLevel());
				assertEquals(true, bundles[i].isMarkedAsStarted());
			} else if (bundles[i].getSymbolicName().equals("org.eclipse.core.runtime")) {
				assertTrue("1.1", bundles[i].getStartLevel() == 2);
				assertTrue("1.2", bundles[i].isMarkedAsStarted() == true);
			} else
				fail("unknown bundle: " + bundles[i].getSymbolicName());
		}
	}

	/**
	 * Test method for {@link org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice#getProperties()}.
	 */
	public void testGetProperties() {
		Map<String, String> properties = productFileAdviceWin32.getProperties();
		assertEquals("1.0", 7, properties.size());
		assertEquals("1.2", "bar", properties.get("foo"));
		assertEquals("1.3", "", properties.get("foo1"));
		assertEquals("1.4", "true", properties.get("osgi.sharedConfiguration.area.readOnly"));
		assertEquals("1.5", "/d/sw/java64/jdk1.6.0_03/bin/java", properties.get("eclipse.vm"));
		assertEquals("1.6", "test.product", properties.get("eclipse.product"));
		assertEquals("1.7", "test.app", properties.get("eclipse.application"));
		assertEquals("1.1", "platform:/base/plugins/org.eclipse.equinox.p2.tests", properties.get("osgi.splashPath"));

		properties = productFileAdvice2.getProperties();
		assertEquals("2.0", 0, properties.size());
	}

	/**
	 * Test method for {@link org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice#getExecutableName()}.
	 */
	public void testGetExecutableName() {
		assertEquals("1.0", "sample", productFileAdviceWin32.getExecutableName());
		assertEquals("2.0", null, productFileAdvice2.getExecutableName());
	}

	/**
	 * Test method for {@link org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice#getProductFile()}.
	 */
	public void testGetProductFile() {
		assertEquals("1.0", productFile, productFileAdviceWin32.getProductFile());
		assertEquals("2.0", productFile2, productFileAdvice2.getProductFile());
	}

	/**
	 * Test method for {@link org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice#getIcons(java.lang.String)}.
	 */
	public void testGetIcons() {
		String[] icons = productFileAdviceWin32.getIcons();
		String absolutePath = new File(productFile.getLocation().getParentFile(), "test/icon.bmp").getAbsolutePath();
		assertEquals("1.0", 1, icons.length);
		assertEquals("1.1", absolutePath, icons[0]);

		icons = productFileAdvice2.getIcons();
		absolutePath = new File(productFile2.getLocation().getParentFile(), "icon.bmp").getAbsolutePath();
		assertEquals("2.0", 1, icons.length);
		assertEquals("2.1", absolutePath, icons[0]);
	}

	public void testSimpleConfiguratorConfigURL() throws Exception {
		File rootFolder = getTestFolder("simpleConfiguratorConfigURL");
		File sampleProduct = new File(rootFolder, "sample.product");
		copy("Copying sample.product", TestData.getFile("ProductActionTest/productWithConfig", "sample.product"), sampleProduct);

		Properties configProperties = new Properties();
		configProperties.put("org.eclipse.equinox.simpleconfigurator.configUrl", "file:org.eclipse.equinox.simpleconfigurator/bundles.info");
		configProperties.put("osgi.bundles", "org.eclipse.equinox.simpleconfigurator@1:start");
		writeProperties(new File(rootFolder, "config.ini"), configProperties);

		StringBuffer buffer = new StringBuffer();
		buffer.append("org.eclipse.equinox.common,3.5.100.v20090817,plugins/org.eclipse.equinox.common_3.5.100.v20090817.jar,2,true\n");
		buffer.append("org.eclipse.update.configurator,3.3.100.v20090813,plugins/org.eclipse.update.configurator_3.3.100.v20090813.jar,4,false\n");
		buffer.append("org.eclipse.equinox.simpleconfigurator,1.0.200.v20090729-1800,plugins/org.eclipse.equinox.simpleconfigurator_1.0.200.v20090729-1800.jar,1,true\n");
		writeBuffer(new File(rootFolder, "org.eclipse.equinox.simpleconfigurator/bundles.info"), buffer);

		ProductFile product = new ProductFile(sampleProduct.getCanonicalPath());
		ProductFileAdvice advice = new ProductFileAdvice(product, "x86.win32.win32");

		BundleInfo[] bundles = advice.getBundles();
		for (int i = 0; i < 2; i++) {
			if (bundles[i].getSymbolicName().equals("org.eclipse.equinox.common")) {
				assertEquals("equinox.common start level", 2, bundles[i].getStartLevel());
				assertEquals("equinox.common started", true, bundles[i].isMarkedAsStarted());
			} else if (bundles[i].getSymbolicName().equals("org.eclipse.update.configurator")) {
				assertEquals("update.configurator start level", 4, bundles[i].getStartLevel());
				assertEquals("update.configurator started", false, bundles[i].isMarkedAsStarted());
			}
		}
	}

	public void testConfigNullLauncher() throws Exception {
		File root = getTestFolder("configNullLauncher");
		File testProduct = new File(root, "test.product");

		StringBuffer buffer = new StringBuffer();
		buffer.append("<product id=\"test.product\" version=\"1\" useFeatures=\"false\">	\n");
		buffer.append("   <configIni use=\"default\">										\n");
		buffer.append("      <win32>config.ini</win32>										\n");
		buffer.append("   </configIni>														\n");
		buffer.append("   <plugins>															\n");
		buffer.append("      <plugin id=\"org.eclipse.core.runtime\" version=\"1.0.4\"/>	\n");
		buffer.append("      <plugin id=\"org.eclipse.equinox.simpleconfigurator\" />		\n");
		buffer.append("   </plugins>														\n");
		buffer.append("</product>															\n");
		writeBuffer(testProduct, buffer);

		Properties configProperties = new Properties();
		configProperties.put("osgi.bundles", "org.eclipse.equinox.simpleconfigurator@1:start");
		configProperties.put("eclipse.application", "test.application");
		configProperties.put("osgi.instance.area.default", "@user.home/workspace");
		writeProperties(new File(root, "config.ini"), configProperties);

		ProductFile product = new ProductFile(testProduct.getCanonicalPath());
		ProductFileAdvice advice = new ProductFileAdvice(product, "x86.win32.win32");

		BundleInfo[] bundles = advice.getBundles();
		assertEquals("bundlers length", 2, bundles.length);

		Map<String, String> adviceProperties = advice.getProperties();
		assertEquals("instance.area.default", "@user.home/workspace", adviceProperties.get("osgi.instance.area.default"));
		assertEquals("eclipse.application", "test.application", adviceProperties.get("eclipse.application"));
		assertEquals("eclipse.product", "test.product", adviceProperties.get("eclipse.product"));
	}
}
