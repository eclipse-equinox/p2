/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB (Hamdan Msheik) - Bypass install license wizard page via plugin_customization
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import java.io.File;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.p2.ui.sdk.SimpleLicenseManager;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.*;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.osgi.framework.Constants;

/**
 * Tests for the install wizard
 */
public class RemediationTest extends WizardTest {

	//	private static final String AVAILABLE_SOFTWARE_PAGE = "AvailableSoftwarePage";
	//	private static final String MAIN_IU = "MainIU";
	public static final int INSTALLATION_SUCCEEDED = 1;
	public static final int INSTALLATION_REMEDIATED = 2;
	public static final int INSTALLATION_FAILED = 3;
	public static final int CHECK_FOR_UPDATES = 4;
	public static final int UPDATE_ONE_IU = 5;
	private String name = "PROFILE_";

	@IUDescription(content = "package: jboss \n" + "singleton: true\n" + "version: 6 \n" + "depends: m2e = 2")
	public IInstallableUnit jboss60;

	@IUDescription(content = "package: jboss \n" + "singleton: true\n" + "version: 5 \n" + "depends: m2e = 1")
	public IInstallableUnit jboss55;

	@IUDescription(content = "package: m2e \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit m2e11;

	@IUDescription(content = "package: m2e \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit m2e12;

	IInstallableUnit toInstall;

	public void setUp() throws Exception {
	}

	public void visibleSetup(int type) throws Exception {
		//Clearout repositories
		name = "PROFILE_" + type;
		URI[] repos = getMetadataRepositoryManager().getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);
		for (URI uri : repos) {
			getMetadataRepositoryManager().removeRepository(uri);
		}

		// create test profile
		profile = createProfile(name);

		// copy of provisioning UI that uses a different profile
		ui = ProvisioningUI.getDefaultUI();
		ui = new ProvisioningUI(ui.getSession(), name, ui.getPolicy());
		ui.getOperationRunner().suppressRestart(true);
		ui.getPolicy().setRepositoriesVisible(false);

		// register alternate services
		SimpleLicenseManager manager = new SimpleLicenseManager(name);
		Dictionary<String, Object> properties = new Hashtable<String, Object>(5);
		properties.put(Constants.SERVICE_RANKING, Integer.valueOf(1));
		regLicenseManager = TestActivator.getContext().registerService(LicenseManager.class.getName(), manager, properties);
		profileElement = new ProfileElement(null, name);
		IULoader.loadIUs(this);
		ILicense[] licenses = new ILicense[] {MetadataFactory.createLicense(URI.create("http://eclipse.org"), "license text"), MetadataFactory.createLicense(URI.create("http://apache.org"), "license text2")};
		((InstallableUnit) jboss60).setLicenses(licenses);
		switch (type) {
			case INSTALLATION_SUCCEEDED :
				createTestMetdataRepository(new IInstallableUnit[] {jboss60, m2e11, m2e12});
				install(m2e12, true, false);
				toInstall = jboss60;
				break;
			case INSTALLATION_REMEDIATED :
				createTestMetdataRepository(new IInstallableUnit[] {jboss60, m2e11, m2e12});
				install(m2e11, true, false);
				toInstall = jboss60;
				break;
			case INSTALLATION_FAILED :
				createTestMetdataRepository(new IInstallableUnit[] {jboss60, m2e11});
				install(m2e11, true, false);
				toInstall = jboss60;
				break;
			case CHECK_FOR_UPDATES :
				createTestMetdataRepository(new IInstallableUnit[] {jboss60, m2e11, m2e12});
				install(m2e11, true, false);
				break;
			case UPDATE_ONE_IU :
				createTestMetdataRepository(new IInstallableUnit[] {jboss55, jboss60, m2e11, m2e12});
				install(jboss55, true, false);
				install(m2e11, true, false);
				break;
			default :
				createTestMetdataRepository(new IInstallableUnit[] {jboss60, m2e11});
				install(m2e11, true, false);

		}

		metaManager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		artifactManager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		File site = new File(TestActivator.getTestDataFolder().toString(), TEST_REPO_PATH);
		testRepoLocation = site.toURI();
		metaManager.addRepository(testRepoLocation);
		artifactManager.addRepository(testRepoLocation);
	}

	public void testInstallWizard() throws Exception {
		LoadMetadataRepositoryJob job = new LoadMetadataRepositoryJob(getProvisioningUI());
		getPolicy().setGroupByCategory(false);
		getPolicy().setShowLatestVersionsOnly(false);
		job.runModal(getMonitor());
		InstallWizard wizard = new InstallWizard(getProvisioningUI(), null, null, job);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);

		dialog.create();
		dialog.setBlockOnOpen(false);
		dialog.open();

	}

	public void testUpdateWizard() throws Exception {
		LoadMetadataRepositoryJob job = new LoadMetadataRepositoryJob(getProvisioningUI());
		getPolicy().setGroupByCategory(false);
		getPolicy().setShowLatestVersionsOnly(false);
		job.runModal(getMonitor());
		UpdateOperation op = new UpdateOperation(getSession());
		op.setProfileId(name);
		op.resolveModal(getMonitor());
		UpdateWizard wizard = new UpdateWizard(getProvisioningUI(), op, new Update[] {}, job);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.create();
		dialog.setBlockOnOpen(false);
		dialog.open();
	}
}
