/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.RootIUAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;

@SuppressWarnings({"unchecked"})
public class ProductActionTest extends ActionTest {

	private String winFitler = "(& (osgi.ws=win32)(osgi.os=win32)(osgi.arch=x86))";
	private String linuxFilter = "(& (osgi.ws=gtk)(osgi.os=linux)(osgi.arch=x86))";

	File executablesFeatureLocation = null;
	String productLocation = "";
	private Capture<RootIUAdvice> rootIUAdviceCapture;
	private Capture<ProductFileAdvice> productFileAdviceCapture;
	String source = "";
	protected TestArtifactRepository artifactRepository = new TestArtifactRepository(getAgent());

	@Override
	protected IPublisherInfo createPublisherInfoMock() {
		//override to create a nice mock, because we don't care about other method calls.
		return createNiceMock(IPublisherInfo.class);
	}

	protected void insertPublisherInfoBehavior() {
		publisherInfo.addAdvice(EasyMock.and(EasyMock.isA(RootIUAdvice.class), EasyMock.capture(rootIUAdviceCapture)));
		publisherInfo.addAdvice(EasyMock.and(EasyMock.isA(ProductFileAdvice.class), EasyMock.capture(productFileAdviceCapture)));
		expect(publisherInfo.getArtifactRepository()).andReturn(artifactRepository).anyTimes();
		expect(publisherInfo.getArtifactOptions()).andReturn(IPublisherInfo.A_PUBLISH).anyTimes();
		//Return an empty list every time getAdvice is called
		expect(publisherInfo.getAdvice((String) anyObject(), anyBoolean(), (String) anyObject(), (Version) anyObject(), (Class) anyObject())).andReturn(Collections.emptyList());
		expectLastCall().anyTimes();
	}

	public void setUp() throws Exception {
		rootIUAdviceCapture = new Capture<RootIUAdvice>();
		productFileAdviceCapture = new Capture<ProductFileAdvice>();
		setupPublisherInfo();
		setupPublisherResult();
	}

	/**
	 * Tests publishing a product containing a branded application with a custom
	 * splash screen, icon, etc.
	 */
	public void testBrandedApplication() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "brandedProduct/branded.product").toString());
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		testAction.perform(publisherInfo, publisherResult, null);
		Collection ius = publisherResult.getIUs("branded.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, ius.size());

		//TODO assert branding was done correctly
	}

	public void testLicense() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "productWithLicense.product").toString());
		PublisherInfo info = new PublisherInfo();
		info.setConfigurations(new String[] {"win32.win32.x86"});
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		testAction.perform(info, publisherResult, null);
		Collection ius = publisherResult.getIUs("licenseIU.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, ius.size());
		IInstallableUnit iu = (IInstallableUnit) ius.iterator().next();
		assertEquals("1.1", "http://www.example.com", iu.getLicenses().iterator().next().getLocation().toString());
		assertEquals("1.2", "This is the liCenSE.", iu.getLicenses().iterator().next().getBody().trim());
	}

	public void testLicenseNoURL() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "licenseNoURL.product").toString());
		PublisherInfo info = new PublisherInfo();
		info.setConfigurations(new String[] {"win32.win32.x86"});
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		testAction.perform(info, publisherResult, null);
		Collection ius = publisherResult.getIUs("licenseIU.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, ius.size());
		IInstallableUnit iu = (IInstallableUnit) ius.iterator().next();
		assertEquals("1.1", "", iu.getLicenses().iterator().next().getLocation().toString());
		assertEquals("1.2", "This is the liCenSE.", iu.getLicenses().iterator().next().getBody().trim());
	}

	public void testLicenseNoText() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "licenseNoText.product").toString());
		PublisherInfo info = new PublisherInfo();
		info.setConfigurations(new String[] {"win32.win32.x86"});
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		testAction.perform(info, publisherResult, null);
		Collection ius = publisherResult.getIUs("licenseIU.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, ius.size());
		IInstallableUnit iu = (IInstallableUnit) ius.iterator().next();
		assertEquals("1.1", "http://www.example.com", iu.getLicenses().iterator().next().getLocation().toString());
		assertEquals("1.2", "", iu.getLicenses().iterator().next().getBody().trim());
	}

	public void testMissingLicense() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "productWithNoLicense.product").toString());
		PublisherInfo info = new PublisherInfo();
		info.setConfigurations(new String[] {"win32.win32.x86"});
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		testAction.perform(info, publisherResult, null);
		Collection ius = publisherResult.getIUs("licenseIU.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, ius.size());
		IInstallableUnit iu = (IInstallableUnit) ius.iterator().next();
		assertEquals(0, iu.getLicenses().size());
	}

	/**
	 * Tests that a product file containing bundle configuration data produces appropriate 
	 * IConfigAdvice (start levels, auto-start).
	 */
	public void testSetBundleConfigData() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "startLevel.product").toString());
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);

		testAction.perform(publisherInfo, publisherResult, null);
		IConfigAdvice configAdvice = productFileAdviceCapture.getValue();
		BundleInfo[] bundles = configAdvice.getBundles();
		assertEquals("1.0", 2, bundles.length);
		assertEquals("1.1", "org.eclipse.equinox.common", bundles[0].getSymbolicName());
		assertEquals("1.2", "1.0.0", bundles[0].getVersion());
		assertEquals("1.3", 13, bundles[0].getStartLevel());
		assertEquals("1.4", false, bundles[0].isMarkedAsStarted());

		assertEquals("2.1", "org.eclipse.core.runtime", bundles[1].getSymbolicName());
		assertEquals("2.2", "2.0.0", bundles[1].getVersion());
		assertEquals("2.3", 6, bundles[1].getStartLevel());
		assertEquals("2.4", true, bundles[1].isMarkedAsStarted());
	}

	public void testMultiProductPublishing() throws Exception {
		ProductFile productFile1 = new ProductFile(TestData.getFile("ProductActionTest", "boundedVersionConfigurations.product").toString());
		ProductFile productFile2 = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		PublisherInfo info = new PublisherInfo();
		info.setConfigurations(getArrayFromString(configSpec, COMMA_SEPARATOR));
		PublisherResult results = new PublisherResult();

		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("org.eclipse.core.runtime");
		iuDescription.setVersion(Version.create("4.0.0"));
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDescription);

		results.addIU(iu, IPublisherResult.NON_ROOT);
		ProductAction action1 = new ProductAction(null, productFile1, flavorArg, executablesFeatureLocation);
		ProductAction action2 = new ProductAction(null, productFile2, flavorArg, executablesFeatureLocation);
		action1.perform(info, results, new NullProgressMonitor());
		results = new PublisherResult();

		results.addIU(iu, IPublisherResult.NON_ROOT);

		action2.perform(info, results, new NullProgressMonitor());
		IQueryResult queryResult = results.query(QueryUtil.createIUQuery(flavorArg + configSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(queryResult));
	}

	public void testMultiPlatformCUs_DifferentPlatforms() throws Exception {
		ProductFile productFile2 = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		PublisherInfo info = new PublisherInfo();
		String windowsConfigSpec = AbstractPublisherAction.createConfigSpec("win32", "win32", "x86");
		String linuxConfigSpec = AbstractPublisherAction.createConfigSpec("gtk", "linux", "x86");
		info.setConfigurations(getArrayFromString(linuxConfigSpec, COMMA_SEPARATOR));
		PublisherResult results = new PublisherResult();

		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("org.eclipse.core.runtime");
		iuDescription.setVersion(Version.create("0.0.0"));
		iuDescription.setFilter(winFitler);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDescription);

		results.addIU(iu, IPublisherResult.NON_ROOT);
		ProductAction action = new ProductAction(null, productFile2, flavorArg, executablesFeatureLocation);

		action.perform(info, results, new NullProgressMonitor());

		IQueryResult queryResult = results.query(QueryUtil.createIUQuery(flavorArg + linuxConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("1.0", 0, queryResultSize(queryResult));

		queryResult = results.query(QueryUtil.createIUQuery(flavorArg + windowsConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("2.0", 0, queryResultSize(queryResult));
	}

	public void testMultiPlatformCUs_SamePlatforms() throws Exception {
		ProductFile productFile2 = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		PublisherInfo info = new PublisherInfo();
		String windowsConfigSpec = AbstractPublisherAction.createConfigSpec("win32", "win32", "x86");
		String linuxConfigSpec = AbstractPublisherAction.createConfigSpec("gtk", "linux", "x86");
		info.setConfigurations(getArrayFromString(linuxConfigSpec, COMMA_SEPARATOR));
		PublisherResult results = new PublisherResult();

		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("org.eclipse.core.runtime");
		iuDescription.setVersion(Version.create("0.0.0"));
		iuDescription.setFilter(linuxFilter);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDescription);

		results.addIU(iu, IPublisherResult.NON_ROOT);
		ProductAction action = new ProductAction(null, productFile2, flavorArg, executablesFeatureLocation);

		action.perform(info, results, new NullProgressMonitor());

		IQueryResult queryResult = results.query(QueryUtil.createIUQuery(flavorArg + linuxConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(queryResult));

		queryResult = results.query(QueryUtil.createIUQuery(flavorArg + windowsConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("2.0", 0, queryResultSize(queryResult));
	}

	public void testMultiPlatformCUs_SamePlatforms_NoVersion() throws Exception {
		ProductFile productFile2 = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		PublisherInfo info = new PublisherInfo();
		String windowsConfigSpec = AbstractPublisherAction.createConfigSpec("win32", "win32", "x86");
		String linuxConfigSpec = AbstractPublisherAction.createConfigSpec("gtk", "linux", "x86");
		info.setConfigurations(getArrayFromString(linuxConfigSpec, COMMA_SEPARATOR));
		PublisherResult results = new PublisherResult();

		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("org.eclipse.core.runtime");
		iuDescription.setFilter(linuxFilter);
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDescription);

		results.addIU(iu, IPublisherResult.NON_ROOT);
		ProductAction action = new ProductAction(null, productFile2, flavorArg, executablesFeatureLocation);

		action.perform(info, results, new NullProgressMonitor());

		IQueryResult queryResult = results.query(QueryUtil.createIUQuery(flavorArg + linuxConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(queryResult));

		queryResult = results.query(QueryUtil.createIUQuery(flavorArg + windowsConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("2.0", 0, queryResultSize(queryResult));
	}

	public void testMultiPlatformCUs_SamePlatforms_BoundedVersions() throws Exception {
		ProductFile productFile2 = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		PublisherInfo info = new PublisherInfo();
		String windowsConfigSpec = AbstractPublisherAction.createConfigSpec("win32", "win32", "x86");
		String linuxConfigSpec = AbstractPublisherAction.createConfigSpec("gtk", "linux", "x86");
		info.setConfigurations(getArrayFromString(linuxConfigSpec, COMMA_SEPARATOR));
		PublisherResult results = new PublisherResult();

		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("org.eclipse.core.runtime");
		iuDescription.setVersion(Version.create("4.0.0")); // Set a specific version number, the one in the .product file uses 0.0.0.  Let's see if it binds properly
		iuDescription.setFilter("(osgi.os=linux)"); //filter is different from linuxConfigSpec, but will still match
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDescription);

		results.addIU(iu, IPublisherResult.NON_ROOT);
		ProductAction action = new ProductAction(null, productFile2, flavorArg, executablesFeatureLocation);

		action.perform(info, results, new NullProgressMonitor());

		IQueryResult queryResult = results.query(QueryUtil.createIUQuery(flavorArg + linuxConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(queryResult));

		queryResult = results.query(QueryUtil.createIUQuery(flavorArg + windowsConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("2.0", 0, queryResultSize(queryResult));
	}

	public void testCUsHost() throws Exception {
		ProductFile productFile2 = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		PublisherInfo info = new PublisherInfo();
		String linuxConfigSpec = AbstractPublisherAction.createConfigSpec("gtk", "linux", "x86");
		info.setConfigurations(getArrayFromString(linuxConfigSpec, COMMA_SEPARATOR));
		PublisherResult results = new PublisherResult();

		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("org.eclipse.core.runtime");
		iuDescription.setVersion(Version.create("4.0.0")); // Set a specific version number, the one in the .product file uses 0.0.0.  Let's see if it binds properly
		iuDescription.setFilter("(osgi.os=linux)"); //filter is different from linuxConfigSpec, but will still match
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDescription);

		results.addIU(iu, IPublisherResult.NON_ROOT);
		ProductAction action = new ProductAction(null, productFile2, flavorArg, executablesFeatureLocation);

		action.perform(info, results, new NullProgressMonitor());

		IQueryResult queryResult = results.query(QueryUtil.createIUQuery(flavorArg + linuxConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(queryResult));
		IInstallableUnitFragment fragment = (IInstallableUnitFragment) queryResult.iterator().next();
		assertEquals("1.1", "org.eclipse.core.runtime", RequiredCapability.extractName(fragment.getHost().iterator().next().getMatches()));
		assertEquals("1.2", Version.create("4.0.0"), RequiredCapability.extractRange(fragment.getHost().iterator().next().getMatches()).getMinimum());
		assertEquals("1.3", Version.create("1.0.0"), fragment.getVersion());

	}

	public void testCUNoHost() throws Exception {
		ProductFile productFile2 = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		PublisherInfo info = new PublisherInfo();
		String windowsConfigSpec = AbstractPublisherAction.createConfigSpec("win32", "win32", "x86");
		String linuxConfigSpec = AbstractPublisherAction.createConfigSpec("gtk", "linux", "x86");
		info.setConfigurations(getArrayFromString(linuxConfigSpec, COMMA_SEPARATOR));
		PublisherResult results = new PublisherResult();

		ProductAction action = new ProductAction(null, productFile2, flavorArg, executablesFeatureLocation);

		action.perform(info, results, new NullProgressMonitor());

		IQueryResult queryResult = results.query(QueryUtil.createIUQuery(flavorArg + linuxConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("1.0", 0, queryResultSize(queryResult));

		queryResult = results.query(QueryUtil.createIUQuery(flavorArg + windowsConfigSpec + "org.eclipse.core.runtime"), new NullProgressMonitor());
		assertEquals("2.0", 0, queryResultSize(queryResult));
	}

	public void testMultiConfigspecProductPublishing() throws IOException, Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "platform.product").toString());
		PublisherInfo info = new PublisherInfo();
		info.setConfigurations(new String[] {"carbon.macos.x86", "cocoa.macos.x86"});
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		testAction.perform(info, publisherResult, null);

		Collection advice = info.getAdvice("carbon.macos.x86", false, null, null, IConfigAdvice.class);
		assertEquals("1.0", 1, advice.size());
	}

	/**
	 * Tests that correct advice is created for the org.eclipse.platform product.
	 */
	public void testPlatformProduct() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "platform.product").toString());
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		testAction.perform(publisherInfo, publisherResult, null);

		IExecutableAdvice launchAdvice = productFileAdviceCapture.getValue();
		assertEquals("1.0", "eclipse", launchAdvice.getExecutableName());

		String[] programArgs = launchAdvice.getProgramArguments();
		assertEquals("2.0", 0, programArgs.length);

		String[] vmArgs = launchAdvice.getVMArguments();
		assertEquals("3.0", 0, vmArgs.length);

	}
}
