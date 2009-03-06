/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.io.File;
import java.util.*;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice;
import org.eclipse.equinox.p2.tests.TestData;

/**
 * Tests the product file advice
 */
public class ProductFileAdviceTest extends TestCase {

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
		List listOfArguments = Arrays.asList(programArgumentsWin32);
		assertEquals("1.0", 1, listOfArguments.size());
		assertTrue("1.0", listOfArguments.contains("programArg"));

		String[] programArguments2 = productFileAdvice2.getProgramArguments();
		assertEquals("2.0", 0, programArguments2.length);
	}

	/**
	 * Test method for {@link org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice#getVMArguments()}.
	 */
	public void testGetVMArguments() {
		String[] vmArgumentsWin32 = productFileAdviceWin32.getVMArguments();
		assertEquals("1.0", 1, vmArgumentsWin32.length);
		assertEquals("1.1", "vmArg", vmArgumentsWin32[0]);

		String[] vmArguments2 = productFileAdvice2.getVMArguments();
		assertEquals("2.0", 0, vmArguments2.length);
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
				// nothing yet
			} else
				fail("unknown bundle: " + bundles[i].getSymbolicName());
		}
	}

	/**
	 * Test method for {@link org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice#getProperties()}.
	 */
	public void testGetProperties() {
		Properties properties = productFileAdviceWin32.getProperties();
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
}
