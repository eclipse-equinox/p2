/*******************************************************************************
 *  Copyright (c) 2020 Red Hat Inc., and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.swt.widgets.Shell;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class EECompatibilityTest extends WizardTest {

	private static final int latestKnownJavaVersion = 14;

	private final class ProvisioningWizardDialogExtension extends ProvisioningWizardDialog {
		private ProvisioningWizardDialogExtension(Shell parent, ProvisioningOperationWizard wizard) {
			super(parent, wizard);
		}

		@Override
		public void nextPressed() {
			super.nextPressed();
		}
	}

	private boolean previewCheckJREValue = false;
	private IInstallableUnit iuRequiringTooRecentEE;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		addAJREJavaSEUnit(getProfile(TESTPROFILE));
		previewCheckJREValue = getPolicy().getCheckAgainstCurrentExecutionEnvironment();
		getPolicy().setCheckAgainstCurrentExecutionEnvironment(true);
		this.iuRequiringTooRecentEE = iuRequiringTooRecentEE();
		assumeTrue("Tests are skipped on latest known Java version", Integer
				.parseInt(System.getProperty("java.version").split("\\.")[0].split("-")[0]) < latestKnownJavaVersion);
	}

	@After
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		getPolicy().setCheckAgainstCurrentExecutionEnvironment(previewCheckJREValue);
	}

	@Test
	public void testSingleIUPreventInstallation() {
		PreselectedIUInstallWizard wizard = new PreselectedIUInstallWizard(getProvisioningUI(), null,
				Collections.singletonList(iuRequiringTooRecentEE), null);
		wizard.setBypassLicensePage(true);
		ProvisioningWizardDialogExtension dialog = new ProvisioningWizardDialogExtension(ProvUI.getDefaultParentShell(),
				wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();
		dialog.nextPressed();
		IResolutionErrorReportingPage page = (IResolutionErrorReportingPage) dialog.getCurrentPage();
		page.getMessage();
		assertFalse(wizard.canFinish());
	}

	@Test
	@Ignore(value = "This test is not relevant as it doesn't build an interesting remediation. Feel free to improve!")
	public void testEEIssueSkipsRemediation() {
		IInstallableUnit unsatisfiableUnit = unsatisfiableUnit();
		IInstallableUnit unsatisfiedFeature = createFeature(unsatisfiableUnit);
		createTestMetdataRepository(new IInstallableUnit[] { unsatisfiableUnit, unsatisfiedFeature });
		IInstallableUnit[] units = new IInstallableUnit[] { iuRequiringTooRecentEE, createFeature(unsatisfiedFeature) };
		PreselectedIUInstallWizard wizard = new PreselectedIUInstallWizard(getProvisioningUI(), null,
				Arrays.asList(units), null);
		wizard.setBypassLicensePage(true);
		ProvisioningWizardDialogExtension dialog = new ProvisioningWizardDialogExtension(ProvUI.getDefaultParentShell(),
				wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();
		dialog.nextPressed(); // to remediation page
		assertTrue(dialog.getCurrentPage() instanceof RemediationPage);
		// Here, we'd like to have remediation pre-selecting "Keep my installation the
		// same and modify the items being installed to be compatible" and keeping the
		// iuRequiringTooRecentEE as part of the plan.
		// but this is not happening with current test scenario, so test it ignored so
		// far.
		assertFalse(wizard.canFinish());
		dialog.nextPressed();
		assertFalse(wizard.canFinish());
		dialog.close();
	}

	private IInstallableUnit unsatisfiableUnit() {
		InstallableUnitDescription desc = new InstallableUnitDescription();
		desc.setId("unsatisfiable-unit"); //$NON-NLS-1$
		desc.setVersion(Version.create("0.0.1"));
		desc.addProvidedCapabilities(Collections.singletonList(
				new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, desc.getId(), desc.getVersion())));
		desc.addRequirements(Collections.singletonList(MetadataFactory.createRequirement(
				IInstallableUnit.NAMESPACE_IU_ID, "blah", VersionRange.create("1.0.0"), null, false, true)));
		return MetadataFactory.createInstallableUnit(desc);
	}

	private static IInstallableUnit iuRequiringTooRecentEE() {
		InstallableUnitDescription desc = new InstallableUnitDescription();
		desc.setId("dummyIU"); //$NON-NLS-1$
		desc.setVersion(Version.create("0.0.1"));
		desc.addProvidedCapabilities(Collections.singletonList(
				new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, desc.getId(), desc.getVersion())));
		desc.addRequirements(Collections.singletonList(MetadataFactory.createRequirement("osgi.ee", "JavaSE",
				VersionRange.create(latestKnownJavaVersion + ".0.0"), null, false, true)));
		return MetadataFactory.createInstallableUnit(desc);
	}

	private static IInstallableUnit createFeature(IInstallableUnit... included) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId("dummyFeatureIncluding"
				+ Stream.of(included).map(IInstallableUnit::getId).collect(Collectors.joining()) + ".feature.group");
		iu.setVersion(Version.create("0.0.1"));
		iu.addProvidedCapabilities(Collections.singleton(MetadataFactory
				.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), iu.getVersion())));
		iu.addRequirements(Stream.of(included)
				.map(unit -> MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, unit.getId(),
						new VersionRange(unit.getVersion(), true, iu.getVersion(), true), null, false, false))
				.collect(Collectors.toList()));
		iu.setProperty(InstallableUnitDescription.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		return MetadataFactory.createInstallableUnit(iu);
	}

	private void addAJREJavaSEUnit(IProfile profile) {
		InstallOperation installOperation = new InstallOperation(getSession(),
				Collections.singletonList(aJREJavaSE(latestKnownJavaVersion)));
		installOperation.setProfileId(TESTPROFILE);
		installOperation.resolveModal(new NullProgressMonitor());
		installOperation.getProvisioningJob(new NullProgressMonitor()).run(new NullProgressMonitor());
	}

	private IInstallableUnit aJREJavaSE(int javaSEVersion) {
		InstallableUnitDescription desc = new InstallableUnitDescription();
		desc.setId("a.jre.javase"); //$NON-NLS-1$
		desc.setVersion(Version.create(javaSEVersion + ".0.0"));
		desc.addProvidedCapabilities(Arrays.asList(new ProvidedCapability[] {
				new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, desc.getId(), desc.getVersion()),
				new ProvidedCapability("osgi.ee", "JavaSE", desc.getVersion()) }));
		return MetadataFactory.createInstallableUnit(desc);
	}
}
