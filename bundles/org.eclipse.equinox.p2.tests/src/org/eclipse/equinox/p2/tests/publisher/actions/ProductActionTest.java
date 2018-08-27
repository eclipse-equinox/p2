/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
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
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   SAP AG - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.createNiceMock;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.errorStatus;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.okStatus;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.statusWithMessageWhich;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.publisher.actions.QueryableFilterAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.IConfigAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;

public class ProductActionTest extends ActionTest {

	private static final String WIN_FILTER = "(& (osgi.ws=win32)(osgi.os=win32)(osgi.arch=x86))";
	private static final String LINUX_FILTER = "(& (osgi.ws=gtk)(osgi.os=linux)(osgi.arch=x86))";

	private static final String WIN_CONFIG_SPEC = AbstractPublisherAction.createConfigSpec("win32", "win32", "x86");
	private static final String LINUX_CONFIG_SPEC = AbstractPublisherAction.createConfigSpec("gtk", "linux", "x86");

	File executablesFeatureLocation = null;
	String productLocation = "";
	String source = "";
	protected TestArtifactRepository artifactRepository = new TestArtifactRepository(getAgent());

	@Override protected IPublisherInfo createPublisherInfoMock() {
		//override to create a nice mock, because we don't care about other method calls.
		return createNiceMock(IPublisherInfo.class);
	}

	@Override public void setUp() throws Exception {
		setupPublisherInfo();
		setupPublisherResult();
	}

	@Override public void setupPublisherInfo() {
		PublisherInfo publisherInfoImpl = new PublisherInfo();
		publisherInfoImpl.setArtifactRepository(artifactRepository);
		publisherInfoImpl.setArtifactOptions(IPublisherInfo.A_PUBLISH);
		publisherInfoImpl.setConfigurations(new String[] {configSpec});

		publisherInfo = publisherInfoImpl;
	}

	/**
	 * Tests publishing a product containing a branded application with a custom
	 * splash screen, icon, etc.
	 */
	public void testBrandedApplication() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "brandedProduct/branded.product").toString());
		addContextIU("org.eclipse.platform.feature.group", "1.2.3");

		performProductAction(productFile);
		Collection<IInstallableUnit> ius = publisherResult.getIUs("branded.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, ius.size());

		//TODO assert branding was done correctly
	}

	public void testLicense() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "productWithLicense.product").toString());
		performProductAction(productFile);
		Collection<IInstallableUnit> ius = publisherResult.getIUs("licenseIU.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, ius.size());
		IInstallableUnit iu = ius.iterator().next();
		assertEquals("1.1", "http://www.example.com", iu.getLicenses().iterator().next().getLocation().toString());
		assertEquals("1.2", "This is the liCenSE.", iu.getLicenses().iterator().next().getBody().trim());
	}

	public void testLicenseNoURL() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "licenseNoURL.product").toString());
		performProductAction(productFile);
		Collection<IInstallableUnit> ius = publisherResult.getIUs("licenseIU.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, ius.size());
		IInstallableUnit iu = ius.iterator().next();
		assertEquals("1.1", "", iu.getLicenses().iterator().next().getLocation().toString());
		assertEquals("1.2", "This is the liCenSE.", iu.getLicenses().iterator().next().getBody().trim());
	}

	public void testLicenseNoText() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "licenseNoText.product").toString());
		performProductAction(productFile);
		Collection<IInstallableUnit> ius = publisherResult.getIUs("licenseIU.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, ius.size());
		IInstallableUnit iu = ius.iterator().next();
		assertEquals("1.1", "http://www.example.com", iu.getLicenses().iterator().next().getLocation().toString());
		assertEquals("1.2", "", iu.getLicenses().iterator().next().getBody().trim());
	}

	public void testMissingLicense() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "productWithNoLicense.product").toString());
		performProductAction(productFile);
		Collection<IInstallableUnit> ius = publisherResult.getIUs("licenseIU.product", IPublisherResult.NON_ROOT);
		assertEquals("1.0", 1, ius.size());
		IInstallableUnit iu = ius.iterator().next();
		assertEquals(0, iu.getLicenses().size());
	}

	public void testMultiProductPublishing() throws Exception {
		ProductFile productFile1 = new ProductFile(TestData.getFile("ProductActionTest", "boundedVersionConfigurations.product").toString());
		ProductFile productFile2 = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());

		addContextIU("org.eclipse.core.runtime", "4.0.0");
		performProductAction(productFile1);

		setupPublisherResult();
		addContextIU("org.eclipse.core.runtime", "4.0.0");
		performProductAction(productFile2);
		assertThat(publisherResult, containsUniqueIU(flavorArg + configSpec + "org.eclipse.core.runtime"));
	}

	public void testMultiPlatformCUs_DifferentPlatforms() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		setConfiguration(LINUX_CONFIG_SPEC);
		addContextIU("org.eclipse.core.runtime", "0.0.0", WIN_FILTER);

		performProductAction(productFile);

		assertThat(publisherResult, not(containsIU(flavorArg + LINUX_CONFIG_SPEC + "org.eclipse.core.runtime")));
		assertThat(publisherResult, not(containsIU(flavorArg + WIN_CONFIG_SPEC + "org.eclipse.core.runtime")));
	}

	public void testMultiPlatformCUs_SamePlatforms() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		setConfiguration(LINUX_CONFIG_SPEC);
		addContextIU("org.eclipse.core.runtime", "0.0.0", LINUX_FILTER);

		performProductAction(productFile);

		assertThat(publisherResult, containsUniqueIU(flavorArg + LINUX_CONFIG_SPEC + "org.eclipse.core.runtime"));
		assertThat(publisherResult, not(containsIU(flavorArg + WIN_CONFIG_SPEC + "org.eclipse.core.runtime")));
	}

	public void testMultiPlatformCUs_SamePlatforms_NoVersion() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		setConfiguration(LINUX_CONFIG_SPEC);
		addContextIU("org.eclipse.core.runtime", null, LINUX_FILTER);

		performProductAction(productFile);

		assertThat(publisherResult, containsUniqueIU(flavorArg + LINUX_CONFIG_SPEC + "org.eclipse.core.runtime"));
		assertThat(publisherResult, not(containsIU(flavorArg + WIN_CONFIG_SPEC + "org.eclipse.core.runtime")));
	}

	public void testMultiPlatformCUs_SamePlatforms_BoundedVersions() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		setConfiguration(LINUX_CONFIG_SPEC);

		// Set a specific version number, the one in the .product file uses 0.0.0.  Let's see if it binds properly
		//filter is different from linuxConfigSpec, but will still match
		addContextIU("org.eclipse.core.runtime", "4.0.0", "(osgi.os=linux)");

		performProductAction(productFile);

		assertThat(publisherResult, containsUniqueIU(flavorArg + LINUX_CONFIG_SPEC + "org.eclipse.core.runtime"));
		assertThat(publisherResult, not(containsIU(flavorArg + WIN_CONFIG_SPEC + "org.eclipse.core.runtime")));
	}

	public void testCUsHost() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		setConfiguration(LINUX_CONFIG_SPEC);

		// Set a specific version number, the one in the .product file uses 0.0.0.  Let's see if it binds properly
		//filter is different from linuxConfigSpec, but will still match
		addContextIU("org.eclipse.core.runtime", "4.0.0", "(osgi.os=linux)");

		performProductAction(productFile);

		IInstallableUnitFragment fragment = (IInstallableUnitFragment) getUniquePublishedIU(flavorArg + LINUX_CONFIG_SPEC + "org.eclipse.core.runtime");
		assertEquals("1.1", "org.eclipse.core.runtime", RequiredCapability.extractName(fragment.getHost().iterator().next().getMatches()));
		assertEquals("1.2", Version.create("4.0.0"), RequiredCapability.extractRange(fragment.getHost().iterator().next().getMatches()).getMinimum());
		assertEquals("1.3", Version.create("1.0.0"), fragment.getVersion());

	}

	public void testMultiConfigspecProductPublishing() throws IOException, Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "platform.product").toString());
		((PublisherInfo) publisherInfo).setConfigurations(new String[] {"carbon.macos.x86", "cocoa.macos.x86"});
		addContextIU("org.eclipse.platform.feature.group", "1.2.3");

		performProductAction(productFile);

		Collection<IConfigAdvice> advice = publisherInfo.getAdvice("carbon.macos.x86", false, null, null, IConfigAdvice.class);
		assertEquals("1.0", 1, advice.size());
	}

	public void testANYConfigSpecPublishing_GeneralBundle() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		String configSpecANY = AbstractPublisherAction.createConfigSpec("ANY", "ANY", "ANY"); // configuration spec to create CUs without filters
		setConfiguration(configSpecANY);

		addContextIU("org.eclipse.core.runtime", "4.0.0");

		performProductAction(productFile);

		// there is a CU for the IU because it applies to all platforms
		IInstallableUnitFragment fragment = (IInstallableUnitFragment) getUniquePublishedIU(flavorArg + configSpecANY + "org.eclipse.core.runtime");
		assertEquals("1.1", "org.eclipse.core.runtime", RequiredCapability.extractName(fragment.getHost().iterator().next().getMatches()));
		assertEquals("1.2", Version.create("4.0.0"), RequiredCapability.extractRange(fragment.getHost().iterator().next().getMatches()).getMinimum());
		assertEquals("1.3", Version.create("1.0.0"), fragment.getVersion());
		assertNull("1.3", fragment.getFilter());
	}

	public void testANYConfigSpecPublishing_PlatformSpecificBundle() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "unboundedVersionConfigurations.product").toString());
		String configSpecANY = AbstractPublisherAction.createConfigSpec("ANY", "ANY", "ANY"); // configuration spec to create CUs without filters
		setConfiguration(configSpecANY);

		addContextIU("org.eclipse.core.runtime", "4.0.0", WIN_FILTER); // any valid filter can be set here

		performProductAction(productFile);

		// there is no CU for the IU because it is platform specific
		assertThat(publisherResult, not(containsIU(flavorArg + configSpecANY + "org.eclipse.core.runtime")));
	}

	public void testProductFileWithRepoAdvice() throws Exception {
		URI location;
		try {
			location = TestData.getFile("ProductActionTest", "contextRepos").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "platform.product").toString());
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		IMetadataRepository repository = metadataRepositoryManager.loadRepository(location, new NullProgressMonitor());
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		PublisherInfo info = new PublisherInfo();
		info.setContextMetadataRepository(repository);
		// TODO this line doesn't have any effect -> is this a bug in the implementation?
		info.addAdvice(new QueryableFilterAdvice(info.getContextMetadataRepository()));

		IStatus status = testAction.perform(info, publisherResult, null);
		assertThat(status, is(okStatus()));

		IQueryResult<IInstallableUnit> results = publisherResult.query(QueryUtil.createIUQuery("org.eclipse.platform.ide", Version.create("3.5.0.I20081118")), null);
		assertEquals("1.0", 1, queryResultSize(results));
		IInstallableUnit unit = results.iterator().next();
		Collection<IRequirement> requiredCapabilities = unit.getRequirements();

		IRequiredCapability capability = null;
		for (Iterator<IRequirement> iterator = requiredCapabilities.iterator(); iterator.hasNext();) {
			IRequiredCapability req = (IRequiredCapability) iterator.next();
			if (req.getName().equals("org.eclipse.platform.feature.group")) {
				capability = req;
				break;
			}
		}
		assertTrue("1.1", capability != null);
		assertEquals("1.2", InstallableUnit.parseFilter("(org.eclipse.update.install.features=true)"), capability.getFilter());
	}

	public void testProductWithAdviceFile() throws Exception {
		// product file that has a corresponding advice file (p2.inf).
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest/productWithAdvice", "productWithAdvice.product").toString());
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		IStatus status = testAction.perform(new PublisherInfo(), publisherResult, null);
		assertThat(status, is(okStatus()));

		IInstallableUnit product = getUniquePublishedIU("productWithAdvice.product");
		Collection<ITouchpointData> data = product.getTouchpointData();
		assertEquals("1.1", 1, data.size());
		String configure = data.iterator().next().getInstruction("configure").getBody();
		assertEquals("1.2", "addRepository(type:0,location:http${#58}//download.eclipse.org/releases/fred);addRepository(type:1,location:http${#58}//download.eclipse.org/releases/fred);", configure);

		//update.id = com.zoobar
		//update.range = [4.0,4.3)
		//update.severity = 0
		//update.description = This is the description
		IUpdateDescriptor update = product.getUpdateDescriptor();
		assertEquals("2.0", 0, update.getSeverity());
		assertEquals("2.1", "This is the description", update.getDescription());
		//unit that fits in range
		assertTrue("2.2", update.isUpdateOf(createIU("com.zoobar", Version.createOSGi(4, 1, 0))));
		//unit that is too old for range
		assertFalse("2.3", update.isUpdateOf(createIU("com.zoobar", Version.createOSGi(3, 1, 0))));
		//version that is too new and outside of range
		assertFalse("2.4", update.isUpdateOf(createIU("com.zoobar", Version.createOSGi(6, 1, 0))));
		//unit with matching version but not matching id
		assertFalse("2.6", update.isUpdateOf(createIU("com.other", Version.createOSGi(4, 1, 0))));
	}

	public void testFiltersOfInclusions() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "productIncludingFragments.product").toString());
		addContextIU("generalbundle", "1.0.1");
		addContextIU("fragment.win", "1.0.2", WIN_FILTER);
		// no fragment.linux in the context

		IStatus status = performProductActionAndReturnStatus(productFile);

		IInstallableUnit productIU = getUniquePublishedIU("productIncludingFragments.uid");
		assertThat(productIU.getRequirements(), hasItem(createIURequirement("generalbundle", createStrictVersionRange("1.0.1"))));
		assertThat(productIU.getRequirements(), hasItem(createIURequirement("fragment.win", createStrictVersionRange("1.0.2"), WIN_FILTER)));

		// this is bug 390361: the Linux fragment is required without filter, so the product cannot be installed for Windows ...
		assertThat(productIU.getRequirements(), hasItem(createIURequirement("fragment.linux", ANY_VERSION)));

		// ... therefore the action shall report an error
		assertThat(status, is(errorStatus()));
		assertThat(Arrays.asList(status.getChildren()), hasItem(statusWithMessageWhich(containsString("Included element fragment.linux 0.0.0 is missing"))));
	}

	public void testMessageForProductWithIgnoredContent() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "mixedContentIgnored.product").toString());
		IStatus status = performProductActionAndReturnStatus(productFile);

		// expect a warning about redundant, ignored content in product file -> requested in bug 325611
		assertThat(Arrays.asList(status.getChildren()), hasItem(statusWithMessageWhich(containsString("are ignored"))));
		// TODO the message should have a code identifying it
	}

	public void testJREIncluded() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "brandedProduct/branded.product").toString());
		addContextIU("org.eclipse.platform.feature.group", "1.2.3");

		performProductAction(productFile);
		Collection<IInstallableUnit> ius = publisherResult.getIUs("branded.product", IPublisherResult.NON_ROOT);
		assertEquals(1, ius.size());
		assertEquals("Missing a.jre.javase", 1, publisherResult.getIUs("a.jre.javase", IPublisherResult.ROOT).size());
		assertEquals("Missing config.a.jre.javase", 1, publisherResult.getIUs("config.a.jre.javase", IPublisherResult.ROOT).size());
	}

	public void testRequiredEEAsSpecified() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "productFileActionTest.product").toString());
		addContextIU("org.eclipse.core.commands", "5.0.0");

		performProductAction(productFile);
		Collection<IInstallableUnit> ius = publisherResult.getIUs("SampleProduct", IPublisherResult.NON_ROOT);
		assertEquals(1, ius.size());
		IInstallableUnit productIU = ius.iterator().next();
		IInstallableUnit aJre = publisherResult.getIUs("a.jre.javase", IPublisherResult.ROOT).iterator().next();
		boolean found = false;
		for (IRequirement req : productIU.getRequirements()) {
			if (req instanceof RequiredCapability) {
				RequiredCapability required = (RequiredCapability) req;
				if (JREAction.NAMESPACE_OSGI_EE.equals(required.getNamespace())) {
					found = true;
					assertEquals("OSGi/Minimum", required.getName());
					assertEquals("1.0.0", required.getRange().getMinimum().toString());
					assertEquals("1.0.0", required.getRange().getMaximum().toString());
					assertTrue(req.isMatch(aJre));
				} else if (required.getName().contains("a.jre")) {
					fail("instead of unit requirement, should use a osgi.ee requirement");
				}
			}
		}
		assertTrue(found);
	}

	private void performProductAction(ProductFile productFile) {
		IStatus status = performProductActionAndReturnStatus(productFile);
		assertThat(status, is(okStatus()));
	}

	private IStatus performProductActionAndReturnStatus(ProductFile productFile) {
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		return testAction.perform(publisherInfo, publisherResult, null);
	}

	private void setConfiguration(String configSpec) {
		((PublisherInfo) publisherInfo).setConfigurations(new String[] {configSpec});
	}

}
