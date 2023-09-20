/*******************************************************************************
 * Copyright (c) 2008, 2022 Code 9 and others.
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

import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.errorStatus;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.okStatus;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.statusWithMessageWhich;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointInstruction;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.TestMetadataRepository;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JREActionTest extends ActionTest {

	private File J14 = new File(TestActivator.getTestDataFolder(), "JREActionTest/1.4/"); //$NON-NLS-1$
	private File J15 = new File(TestActivator.getTestDataFolder(), "JREActionTest/1.5/"); //$NON-NLS-1$
	private File J16 = new File(TestActivator.getTestDataFolder(), "JREActionTest/1.6/"); //$NON-NLS-1$
	private File jreWithPackageVersionsFolder = new File(TestActivator.getTestDataFolder(), "JREActionTest/packageVersions/"); //$NON-NLS-1$
	private File jreWithPackageVersionsProfile = new File(TestActivator.getTestDataFolder(), "JREActionTest/packageVersions/test-1.0.0.profile"); //$NON-NLS-1$

	protected TestArtifactRepository artifactRepository = new TestArtifactRepository(getAgent());
	protected TestMetadataRepository metadataRepository;

	@Override
	@Before
	public void setUp() throws Exception {
		setupPublisherInfo();
		setupPublisherResult();
	}

	// TODO this name is misleading: the test doesn't test the real Java 1.4 JRE IU but a broken local copy of the 1.4 profile
	@Test
	public void test14() throws Exception {
		performAction(new JREAction(J14));

		Version jreVersion = Version.create("1.4.0");
		verifyMetadataIU("a.jre.j2se", 91, 0, jreVersion);
		verifyConfigIU("a.jre.j2se", jreVersion); //$NON-NLS-1$
		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre.j2se,1.4.0"), J14, "J2SE-1.4.profile"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void test15() throws Exception {
		performAction(new JREAction(J15));

		Version jreVersion = Version.create("1.5.0");
		verifyMetadataIU("a.jre.j2se", 118, 0, jreVersion);
		verifyConfigIU("a.jre.j2se", jreVersion); //$NON-NLS-1$
		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre.j2se,1.5.0"), J15, "J2SE-1.5.profile"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void test16() throws Exception {
		performAction(new JREAction(J16));

		Version jreVersion = Version.create("1.6.0");
		verifyMetadataIU("a.jre.javase", 116, 0, jreVersion);
		verifyConfigIU("a.jre.javase", jreVersion); //$NON-NLS-1$
		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre.javase,1.6.0"), J16, "JavaSE-1.6.profile"); //$NON-NLS-1$//$NON-NLS-2$
	}

	@Test
	public void testOSGiMin() throws Exception {
		performAction(new JREAction("OSGi/Minimum-1.2"));

		Version jreVersion = Version.create("1.2.0");
		verifyMetadataIU("a.jre.osgi.minimum", 16, 3, jreVersion);
		// verifyConfigIU("a.jre.osgi.minimum", jreVersion); // TODO config IU is not needed!?
	}

	@Test
	public void testPackageVersionsFromJreFolder() throws Exception {
		performAction(new JREAction(jreWithPackageVersionsFolder));

		Collection<IProvidedCapability> providedCapabilities = getPublishedCapabilitiesOf("a.jre.test");
		assertThat(providedCapabilities, hasItem(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", null)));
		assertThat(providedCapabilities, hasItem(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", Version.create("1.0.0"))));

		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre.test,1.0.0"), jreWithPackageVersionsFolder, "test-1.0.0.profile"); //$NON-NLS-1$//$NON-NLS-2$
	}

	@Test
	public void testPackageVersionsFromJavaProfile() throws Exception {
		// introduced for bug 334519: directly point to a profile file
		performAction(new JREAction(jreWithPackageVersionsProfile));

		Collection<IProvidedCapability> providedCapabilities = getPublishedCapabilitiesOf("a.jre.test");
		assertThat(providedCapabilities, hasItem(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", null)));
		assertThat(providedCapabilities, hasItem(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", Version.create("1.0.0"))));
	}

	@Test
	public void testDefaultJavaProfile() throws Exception {
		performAction(new JREAction((String) null));

		// these assertions need to be changed each time the default java profile, hardcoded in o.e.e.p2.publisher.actions.JREAction, is changed;
		verifyMetadataIU("a.jre.javase", 226, 23, Version.parseVersion("17.0.0"));
		// verifyConfigIU(DEFAULT_JRE_NAME, DEFAULT_JRE_VERSION); // TODO config IU is not needed!?
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonExistingJreLocation() {
		File nonExistingProfile = new File(jreWithPackageVersionsFolder, "no.profile");
		performAction(new JREAction(nonExistingProfile));
	}

	@Test
	public void testOsgiEECapabilities() {
		// added for bug 388566
		performAction(new JREAction("J2SE-1.5"));

		Collection<IProvidedCapability> capabilities = getPublishedCapabilitiesOf("a.jre.j2se");
		assertThat(capabilities, not(hasItem(createEECapability("JavaSE", "1.6"))));
		assertThat(capabilities, hasItem(createEECapability("JavaSE", "1.5")));
		assertThat(capabilities, hasItem(createEECapability("OSGi/Minimum", "1.0")));

		assertThat(capabilities, not(hasItem(createEECapability("J2SE", "1.5"))));
	}

	@Test
	public void testSingleOsgiEECapability() {
		// contains a single version:Version attribute instead of the common version:List<Version>
		performAction(new JREAction("OSGi/Minimum-1.0"));

		Collection<IProvidedCapability> capabilities = getPublishedCapabilitiesOf("a.jre.osgi.minimum");
		assertThat(capabilities, not(hasItem(createEECapability("JavaSE", "1.5"))));
		assertThat(capabilities, hasItem(createEECapability("OSGi/Minimum", "1.0")));
	}

	@Test
	public void testInvalidOsgiEECapabilitySpec() {
		testAction = new JREAction(new File(TestActivator.getTestDataFolder(), "JREActionTest/invalidOsgiEE/ee-capability-syntax-test.profile"));
		IStatus status = testAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		assertThat(status, is(errorStatus()));

		IStatus eeStatus = status.getChildren()[0];
		assertThat(eeStatus.getMessage(), containsString("org.osgi.framework.system.capabilities"));
		assertThat(Arrays.asList(eeStatus.getChildren()), hasItem(statusWithMessageWhich(containsString("Attribute 'osgi.ee' is missing"))));
		assertThat(Arrays.asList(eeStatus.getChildren()), hasItem(statusWithMessageWhich(containsString("Either 'version:Version' or 'version:List<Version>' must be specified"))));
		assertThat(Arrays.asList(eeStatus.getChildren()), hasItem(statusWithMessageWhich(containsString("Syntax error in version '1.a.invalidversion'"))));
		assertThat(Arrays.asList(eeStatus.getChildren()), hasItem(statusWithMessageWhich(containsString("Ignoring unknown capability namespace 'other.namespace'"))));
		assertThat(Arrays.asList(eeStatus.getChildren()), hasItem(statusWithMessageWhich(containsString("Cannot specify both 'version:Version' and 'version:List<Version>'"))));
		assertEquals(5, eeStatus.getChildren().length);
	}

	private void performAction(JREAction jreAction) {
		IStatus status = jreAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		assertThat(status, is(okStatus()));
	}

	private void verifyMetadataIU(String id, int expectedProvidedPackages, int expectedProvidedEEs, Version jreVersion) {
		IInstallableUnit foo = getPublishedUnit(id);

		// check version
		assertEquals(jreVersion, foo.getVersion());

		// check touchpointType
		assertTrue(foo.getTouchpointType().getId().equalsIgnoreCase("org.eclipse.equinox.p2.native")); //$NON-NLS-1$
		assertEquals(Version.create("1.0.0"), foo.getTouchpointType().getVersion()); //$NON-NLS-1$

		// check provided capabilities
		Collection<IProvidedCapability> fooProvidedCapabilities = foo.getProvidedCapabilities();
		int expected = expectedProvidedPackages + expectedProvidedEEs;
		assertTrue(fooProvidedCapabilities.size() >= expected);
	}

	private void verifyConfigIU(String id, Version jreVersion) {
		IInstallableUnit bar = getPublishedUnit("config." + id);

		Map<?, ?> instructions = bar.getTouchpointData().iterator().next().getInstructions();
		assertEquals("unzip(source:@artifact, target:${installFolder});", //$NON-NLS-1$
				((ITouchpointInstruction) instructions.get("install")).getBody()); //$NON-NLS-1$
		assertEquals("cleanupzip(source:@artifact, target:${installFolder});", //$NON-NLS-1$
				((ITouchpointInstruction) instructions.get("uninstall")).getBody()); //$NON-NLS-1$
		assertTrue(bar instanceof IInstallableUnitFragment);
		Collection<IRequirement> requiredCapability = ((IInstallableUnitFragment) bar).getHost();
		verifyRequirement(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, id, new VersionRange(jreVersion, true, Version.MAX_VERSION, true));
		assertEquals(1, requiredCapability.size());

		Collection<IProvidedCapability> providedCapability = bar.getProvidedCapabilities();
		verifyProvidedCapability(providedCapability, IInstallableUnit.NAMESPACE_IU_ID, "config." + id, jreVersion); //$NON-NLS-1$
		assertEquals(1, providedCapability.size());

		assertEquals("true", bar.getProperty("org.eclipse.equinox.p2.type.fragment")); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals(jreVersion, bar.getVersion());
	}

	private void verifyArtifactRepository(IArtifactKey key, File JRELocation, final String fileName) throws IOException {
		assertTrue(artifactRepository.contains(key));
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		FileFilter fileFilter = file -> file.getName().endsWith(fileName);
		File[] contentBytes = JRELocation.listFiles(fileFilter);
		FileUtils.copyStream(new FileInputStream(contentBytes[0]), false, content, true);
		ZipInputStream zipInputStream = artifactRepository.getZipInputStream(key);

		Map<String, Object[]> fileMap = new HashMap<>();
		fileMap.put(fileName, new Object[] {contentBytes[0], content.toByteArray()});
		TestData.assertContains(fileMap, zipInputStream, true);
	}

	private IInstallableUnit getPublishedUnit(String id) {
		Collection<IInstallableUnit> units = publisherResult.getIUs(id, IPublisherResult.ROOT);
		assertEquals(1, units.size());
		return units.iterator().next();
	}

	private Collection<IProvidedCapability> getPublishedCapabilitiesOf(String id) {
		Collection<IInstallableUnit> ius = publisherResult.getIUs(id, IPublisherResult.ROOT);
		assertEquals(1, ius.size());
		IInstallableUnit iu = ius.iterator().next();
		return iu.getProvidedCapabilities();
	}

	private static IProvidedCapability createEECapability(String ee, String version) {
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("osgi.ee", ee);
		attrs.put("version", Version.parseVersion(version));

		return MetadataFactory.createProvidedCapability("osgi.ee", attrs);
	}

	@Override
	protected void insertPublisherInfoBehavior() {
		when(publisherInfo.getArtifactRepository()).thenReturn(artifactRepository);
		when(publisherInfo.getArtifactOptions()).thenReturn(IPublisherInfo.A_PUBLISH);
	}
}
