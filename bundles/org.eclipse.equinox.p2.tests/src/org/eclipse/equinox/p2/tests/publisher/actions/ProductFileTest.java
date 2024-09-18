/*******************************************************************************
* Copyright (c) 2009, 2024 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
*   SAP SE - support macOS bundle URL types
******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.eclipse.IMacOsBundleUrlType;
import org.eclipse.equinox.p2.tests.TestData;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the product file parser found in the publisher.
 */
public class ProductFileTest {

	String productFileLocation = null;
	ProductFile productFile = null;
	ProductFile noLauncherFlag = null;
	ProductFile falseLauncherFlag = null;
	ProductFile trueLauncherFlag = null;
	ProductFile rootFeaturesProduct;

	String configFile = "/org.eclipse.equinox.p2.tests/testData/ProductActionTest/productWithConfig/config.ini";
	private String uidProductFileLocation;
	private ProductFile uidProductFile;

	@Before
	public void setUp() throws Exception {
		productFileLocation = TestData.getFile("ProductActionTest/productWithConfig", "sample.product").toString();
		noLauncherFlag = new ProductFile(TestData.getFile("ProductActionTest/launcherFlags", "noLauncherFlag.product").toString());
		falseLauncherFlag = new ProductFile(TestData.getFile("ProductActionTest/launcherFlags", "falseLauncherFlag.product").toString());
		trueLauncherFlag = new ProductFile(TestData.getFile("ProductActionTest/launcherFlags", "trueLauncherFlag.product").toString());
		productFile = new ProductFile(productFileLocation);
		uidProductFileLocation = TestData.getFile("ProductActionTest/productWithConfig", "uidproduct.product").toString();
		uidProductFile = new ProductFile(uidProductFileLocation);
		rootFeaturesProduct = new ProductFile(TestData.getFile("ProductActionTest", "rootFeatures.product").toString());
	}

	/**
	 * Test method for {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getLauncherName()}.
	 */
	@Test
	public void testGetLauncherName() {
		assertEquals("sample", productFile.getLauncherName());
	}

	/**
	 * Test method for {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getLocation()}.
	 */
	@Test
	public void testGetLocation() {
		assertEquals(productFileLocation, productFile.getLocation().toString());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getConfigProperties()}.
	 */
	@Test
	public void testGetConfigurationProperties() {
		Map<String, String> properties = productFile.getConfigurationProperties();
		assertEquals(4, properties.size());
		assertEquals("bar", properties.get("foo"));
		assertTrue(properties.get("foo1").isEmpty());
		assertEquals("test.product", properties.get("eclipse.product"));
		assertEquals("test.app", properties.get("eclipse.application"));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getBundles()}.
	 */
	@Test
	public void testGetBundles() {
		List<IVersionedId> bundles = productFile.getBundles();
		assertEquals(2, bundles.size());
		assertEquals("org.eclipse.core.runtime", bundles.get(0).getId());
		assertEquals(Version.createOSGi(1, 0, 4), bundles.get(0).getVersion());
		assertEquals("org.eclipse.swt.win32.win32.x86", bundles.get(1).getId());
		assertEquals(Version.emptyVersion, bundles.get(1).getVersion());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getBundleInfos()}.
	 */
	@Test
	public void testGetBundleInfos() {
		List<BundleInfo> bundleInfos = productFile.getBundleInfos();
		BundleInfo info = bundleInfos.iterator().next();
		assertEquals(1, bundleInfos.size());
		assertEquals("org.eclipse.core.runtime", info.getSymbolicName());
		assertEquals(2, info.getStartLevel());
		assertTrue(info.isMarkedAsStarted());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getFeatures()}.
	 */
	@Test
	public void testGetFeatures() {
		List<IVersionedId> features = productFile.getFeatures();
		assertEquals(1, features.size());
		assertEquals("org.eclipse.rcp", features.get(0).getId());
		assertEquals(Version.create("3.5.0.v20081110-9C9tEvNEla71LZ2jFz-RFB-t"), features.get(0).getVersion());
	}

	@Test
	public void testGetRootFeatures() {
		List<IVersionedId> features = rootFeaturesProduct.getFeatures(IProductDescriptor.ROOT_FEATURES);
		assertThat(features, hasItem(new VersionedId("org.eclipse.help", "2.0.102.v20140128")));
		assertThat(features, hasItem(new VersionedId("org.eclipse.egit", "0.0.0")));
		assertEquals(2, features.size());
	}

	@Test
	public void testGetIncludedFeatures() {
		List<IVersionedId> features = rootFeaturesProduct.getFeatures(IProductDescriptor.INCLUDED_FEATURES);
		assertThat(features, hasItem(new VersionedId("org.eclipse.rcp", "4.4.0.v20140128")));
		assertThat(features, hasItem(new VersionedId("org.eclipse.e4.rcp", "0.0.0")));
		assertEquals(2, features.size());
	}

	@Test
	public void testGetFeaturesOnlyReturnsIncludedFeatures() {
		assertThat(rootFeaturesProduct.getFeatures(), is(rootFeaturesProduct.getFeatures(IProductDescriptor.INCLUDED_FEATURES)));
	}

	@Test
	public void testHasFeatures() throws Exception {
		ProductFile featuresOnly = new ProductFile(TestData.getFile("ProductActionTest", "onlyFeatures.product").toString());
		assertTrue(featuresOnly.hasFeatures());
		assertFalse(featuresOnly.hasBundles());
	}

	@Test
	public void testHasBundles() throws Exception {
		ProductFile bundlesOnly = new ProductFile(TestData.getFile("ProductActionTest", "onlyBundles.product").toString());
		assertFalse(bundlesOnly.hasFeatures());
		assertTrue(bundlesOnly.hasBundles());
	}

	@Test
	public void testHasFragments() throws Exception {
		ProductFile bundlesOnly = new ProductFile(TestData.getFile("ProductActionTest", "onlyFragments.product").toString());
		assertFalse(bundlesOnly.hasFeatures());
		assertTrue(bundlesOnly.hasBundles());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getConfigIniPath()}.
	 */
	@Test
	public void testGetConfigIniPath() {
		String configIni = productFile.getConfigIniPath("win32");
		assertEquals("config.ini", configIni);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getId()}.
	 */
	@Test
	public void testGetId() {
		String id = productFile.getId();
		assertEquals("test.product", id);
	}

	@Test
	public void testGetUID() {
		String id = uidProductFile.getId();
		assertEquals("UID.test.product", id);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getSplashLocation()}.
	 */
	@Test
	public void testGetSplashLocation() {
		String splashLocation = productFile.getSplashLocation();
		assertEquals("org.eclipse.equinox.p2.tests", splashLocation);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getProductName()}.
	 */
	@Test
	public void testGetProductName() {
		String productName = productFile.getProductName();
		assertEquals("aaTestProduct", productName);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getApplication()}.
	 */
	@Test
	public void testGetApplication() {
		String application = productFile.getApplication();
		assertEquals("test.app", application);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#useFeatures()}.
	 */
	@Test
	public void testUseFeatures() {
		boolean useFeatures = productFile.useFeatures();
		assertFalse("1.0", useFeatures);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getVersion()}.
	 */
	@Test
	public void testGetVersion() {
		String version = productFile.getVersion();
		assertEquals("1.0", Version.create("1"), Version.create(version));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getVMArguments(java.lang.String)}.
	 */
	@Test
	public void testGetVMArguments() {
		String vmArguments = productFile.getVMArguments("");
		assertEquals("vmArg -Dfoo=\"b a r\"", vmArguments);
		vmArguments = productFile.getVMArguments(null);
		assertEquals("vmArg -Dfoo=\"b a r\"", vmArguments);
	}

	@Test
	public void testIncludeLaunchers() {
		assertTrue(noLauncherFlag.includeLaunchers());
		assertFalse(falseLauncherFlag.includeLaunchers());
		assertTrue(trueLauncherFlag.includeLaunchers());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile#getProgramArguments(java.lang.String)}.
	 */
	@Test
	public void testGetProgramArguments() {
		String programArguments = productFile.getProgramArguments("");
		assertEquals("programArg -name \"My Name\"", programArguments);
		programArguments = productFile.getProgramArguments(null);
		assertEquals("programArg -name \"My Name\"", programArguments);
	}

	@Test
	public void testGetLicenseURL() throws Exception {
		String productWithLicense = TestData.getFile("ProductActionTest", "productWithLicense.product").toString();
		ProductFile product = new ProductFile(productWithLicense);
		assertEquals("http://www.example.com", product.getLicenseURL());
	}

	@Test
	public void testGetLicenseText() throws Exception {
		String productWithLicense = TestData.getFile("ProductActionTest", "productWithLicense.product").toString();
		ProductFile product = new ProductFile(productWithLicense);
		assertEquals("This is the liCenSE.", product.getLicenseText().trim());
	}

	@Test
	public void testGetVM() throws Exception {
		String productWithVM = TestData.getFile("ProductActionTest", "productWithVM.product").toString();
		ProductFile product = new ProductFile(productWithVM);
		assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/OSGi%Minimum-1.2", product.getVM(Platform.OS_WIN32));
		assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-9", product.getVM(Platform.OS_LINUX));
		assertNull(product.getVM(Platform.OS_MACOSX));
	}

	@Test
	public void testGetMacOsBundleUrlTypes() throws Exception {
		String productWithMacOsBundleUrlTypes = TestData
				.getFile("ProductActionTest", "productWithMacOsBundleUrlTypes.product").toString();
		ProductFile product = new ProductFile(productWithMacOsBundleUrlTypes);
		List<IMacOsBundleUrlType> macOsBundleUrlTypes = product.getMacOsBundleUrlTypes();
		assertEquals(2, macOsBundleUrlTypes.size());
		assertEquals("eclipse+command", macOsBundleUrlTypes.get(0).getScheme());
		assertEquals("Eclipse Command", macOsBundleUrlTypes.get(0).getName());
		assertEquals("vendor", macOsBundleUrlTypes.get(1).getScheme());
		assertEquals("Vendor Application", macOsBundleUrlTypes.get(1).getName());
	}
}
