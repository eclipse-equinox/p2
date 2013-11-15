/*******************************************************************************
 * Copyright (c) 2008, 2012 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   SAP AG - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.expect;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.tests.*;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

@SuppressWarnings({"unchecked"})
public class JREActionTest extends ActionTest {

	private File J14 = new File(TestActivator.getTestDataFolder(), "JREActionTest/1.4/"); //$NON-NLS-1$
	private File J15 = new File(TestActivator.getTestDataFolder(), "JREActionTest/1.5/"); //$NON-NLS-1$
	private File J16 = new File(TestActivator.getTestDataFolder(), "JREActionTest/1.6/"); //$NON-NLS-1$
	private File jreWithPackageVersionsFolder = new File(TestActivator.getTestDataFolder(), "JREActionTest/packageVersions/"); //$NON-NLS-1$
	private File jreWithPackageVersionsProfile = new File(TestActivator.getTestDataFolder(), "JREActionTest/packageVersions/test-1.0.0.profile"); //$NON-NLS-1$

	protected TestArtifactRepository artifactRepository = new TestArtifactRepository(getAgent());
	protected TestMetadataRepository metadataRepository;

	public void setUp() throws Exception {
		setupPublisherInfo();
		setupPublisherResult();
	}

	// TODO this name is misleading: the test doesn't test the real Java 1.4 JRE IU but a broken local copy of the 1.4 profile 
	public void test14() throws Exception {
		performAction(new JREAction(J14));

		Version jreVersion = Version.create("1.4.0");
		verifyMetadataIU("a.jre.j2se", 91, 0, jreVersion);
		verifyConfigIU("a.jre.j2se", jreVersion); //$NON-NLS-1$
		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre.j2se,1.4.0"), J14, "J2SE-1.4.profile"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void test15() throws Exception {
		performAction(new JREAction(J15));

		Version jreVersion = Version.create("1.5.0");
		verifyMetadataIU("a.jre.j2se", 118, 0, jreVersion);
		verifyConfigIU("a.jre.j2se", jreVersion); //$NON-NLS-1$
		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre.j2se,1.5.0"), J15, "J2SE-1.5.profile"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void test16() throws Exception {
		performAction(new JREAction(J16));

		Version jreVersion = Version.create("1.6.0");
		verifyMetadataIU("a.jre.javase", 116, 0, jreVersion);
		verifyConfigIU("a.jre.javase", jreVersion); //$NON-NLS-1$
		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre.javase,1.6.0"), J16, "JavaSE-1.6.profile"); //$NON-NLS-1$//$NON-NLS-2$
	}

	public void testOSGiMin() throws Exception {
		performAction(new JREAction("OSGi/Minimum-1.2"));

		Version jreVersion = Version.create("1.2.0");
		verifyMetadataIU("a.jre.osgi.minimum", 1, 3, jreVersion);
		// verifyConfigIU("a.jre.osgi.minimum", jreVersion); // TODO config IU is not needed!?
	}

	public void testPackageVersionsFromJreFolder() throws Exception {
		performAction(new JREAction(jreWithPackageVersionsFolder));

		Collection<IProvidedCapability> providedCapabilities = getPublishedCapabilitiesOf("a.jre.test");
		assertThat(providedCapabilities, hasItem(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", null)));
		assertThat(providedCapabilities, hasItem(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", Version.create("1.0.0"))));

		verifyArtifactRepository(ArtifactKey.parse("binary,a.jre.test,1.0.0"), jreWithPackageVersionsFolder, "test-1.0.0.profile"); //$NON-NLS-1$//$NON-NLS-2$
	}

	public void testPackageVersionsFromJavaProfile() throws Exception {
		// introduced for bug 334519: directly point to a profile file
		performAction(new JREAction(jreWithPackageVersionsProfile));

		Collection<IProvidedCapability> providedCapabilities = getPublishedCapabilitiesOf("a.jre.test");
		assertThat(providedCapabilities, hasItem(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", null)));
		assertThat(providedCapabilities, hasItem(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", Version.create("1.0.0"))));
	}

	public void testDefaultJavaProfile() throws Exception {
		performAction(new JREAction((String) null));

		// these assertions need to be changed each time the default java profile, hardcoded in o.e.e.p2.publisher.actions.JREAction, is changed;
		verifyMetadataIU("a.jre.javase", 158, 12, Version.parseVersion("1.6"));
		// verifyConfigIU(DEFAULT_JRE_NAME, DEFAULT_JRE_VERSION); // TODO config IU is not needed!?
	}

	public void testNonExistingJreLocation() {
		File nonExistingProfile = new File(jreWithPackageVersionsFolder, "no.profile");
		try {
			performAction(new JREAction(nonExistingProfile));
			fail("Expected failure when the JRE location does not exists.");
			// TODO shouldn't this be an error status?
		} catch (IllegalArgumentException e) {
			// test is successful
		} catch (Exception e) {
			fail("Expected IllegalArgumentException when the JRE location does not exists, caught " + e.getClass().getName());
		}
	}

	public void testOsgiEECapabilities() {
		// added for bug 388566
		performAction(new JREAction("J2SE-1.5"));

		Collection<IProvidedCapability> capabilities = getPublishedCapabilitiesOf("a.jre.j2se");
		assertThat(capabilities, not(hasItem((IProvidedCapability) new ProvidedCapability("osgi.ee", "JavaSE", Version.parseVersion("1.6")))));
		assertThat(capabilities, hasItem((IProvidedCapability) new ProvidedCapability("osgi.ee", "JavaSE", Version.parseVersion("1.5"))));
		assertThat(capabilities, hasItem((IProvidedCapability) new ProvidedCapability("osgi.ee", "OSGi/Minimum", Version.parseVersion("1.0"))));

		assertThat(capabilities, not(hasItem((IProvidedCapability) new ProvidedCapability("osgi.ee", "J2SE", Version.parseVersion("1.5")))));
	}

	public void testSingleOsgiEECapability() {
		// contains a single version:Version attribute instead of the common version:List<Version>
		performAction(new JREAction("OSGi/Minimum-1.0"));

		Collection<IProvidedCapability> capabilities = getPublishedCapabilitiesOf("a.jre.osgi.minimum");
		assertThat(capabilities, not(hasItem((IProvidedCapability) new ProvidedCapability("osgi.ee", "JavaSE", Version.parseVersion("1.5")))));
		assertThat(capabilities, hasItem((IProvidedCapability) new ProvidedCapability("osgi.ee", "OSGi/Minimum", Version.parseVersion("1.0"))));
	}

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
		assertThat(eeStatus.getChildren().length, is(5));
	}

	private void performAction(JREAction jreAction) {
		IStatus status = jreAction.perform(publisherInfo, publisherResult, new NullProgressMonitor());
		assertThat(status, is(okStatus()));
	}

	private void verifyMetadataIU(String id, int expectedProvidedPackages, int expectedProvidedEEs, Version jreVersion) {
		IInstallableUnit foo = getPublishedUnit(id);

		// check version
		assertTrue(foo.getVersion().equals(jreVersion));

		// check touchpointType
		assertTrue(foo.getTouchpointType().getId().equalsIgnoreCase("org.eclipse.equinox.p2.native")); //$NON-NLS-1$
		assertTrue(foo.getTouchpointType().getVersion().equals(Version.create("1.0.0"))); //$NON-NLS-1$

		// check provided capabilities
		Collection<IProvidedCapability> fooProvidedCapabilities = foo.getProvidedCapabilities();
		assertThat(fooProvidedCapabilities.size(), is(1 + expectedProvidedPackages + expectedProvidedEEs));
	}

	private void verifyConfigIU(String id, Version jreVersion) {
		IInstallableUnit bar = getPublishedUnit("config." + id);

		Map instructions = bar.getTouchpointData().iterator().next().getInstructions();
		assertTrue(((ITouchpointInstruction) instructions.get("install")).getBody().equals("unzip(source:@artifact, target:${installFolder});")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(((ITouchpointInstruction) instructions.get("uninstall")).getBody().equals("cleanupzip(source:@artifact, target:${installFolder});")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(bar instanceof IInstallableUnitFragment);
		Collection<IRequirement> requiredCapability = ((IInstallableUnitFragment) bar).getHost();
		verifyRequiredCapability(requiredCapability, IInstallableUnit.NAMESPACE_IU_ID, id, new VersionRange(jreVersion, true, Version.MAX_VERSION, true)); //$NON-NLS-1$ 
		assertTrue(requiredCapability.size() == 1);

		Collection<IProvidedCapability> providedCapability = bar.getProvidedCapabilities();
		verifyProvidedCapability(providedCapability, IInstallableUnit.NAMESPACE_IU_ID, "config." + id, jreVersion); //$NON-NLS-1$ 
		assertTrue(providedCapability.size() == 1);

		assertTrue(bar.getProperty("org.eclipse.equinox.p2.type.fragment").equals("true")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(bar.getVersion().equals(jreVersion));
	}

	private void verifyArtifactRepository(IArtifactKey key, File JRELocation, final String fileName) throws IOException {
		assertTrue(artifactRepository.contains(key));
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		FileFilter fileFilter = new FileFilter() {
			public boolean accept(File file) {
				return file.getName().endsWith(fileName);
			}
		};
		File[] contentBytes = JRELocation.listFiles(fileFilter);
		FileUtils.copyStream(new FileInputStream(contentBytes[0]), false, content, true);
		ZipInputStream zipInputStream = artifactRepository.getZipInputStream(key);

		Map fileMap = new HashMap();
		fileMap.put(fileName, new Object[] {contentBytes[0], content.toByteArray()});
		TestData.assertContains(fileMap, zipInputStream, true);
	}

	private IInstallableUnit getPublishedUnit(String id) {
		Collection<IInstallableUnit> units = publisherResult.getIUs(id, IPublisherResult.ROOT);
		assertThat(units.size(), is(1));
		return units.iterator().next();
	}

	private Collection<IProvidedCapability> getPublishedCapabilitiesOf(String id) {
		Collection<IInstallableUnit> ius = publisherResult.getIUs(id, IPublisherResult.ROOT);
		assertThat(ius.size(), is(1));
		IInstallableUnit iu = ius.iterator().next();
		return iu.getProvidedCapabilities();
	}

	protected void insertPublisherInfoBehavior() {
		expect(publisherInfo.getArtifactRepository()).andReturn(artifactRepository).anyTimes();
		expect(publisherInfo.getArtifactOptions()).andReturn(IPublisherInfo.A_PUBLISH).anyTimes();
	}

}
