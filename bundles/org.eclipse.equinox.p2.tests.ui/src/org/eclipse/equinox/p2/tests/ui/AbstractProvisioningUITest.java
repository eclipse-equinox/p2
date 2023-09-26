/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui;

import java.io.File;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.p2.ui.sdk.SimpleLicenseManager;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.operations.ProfileModificationJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.ui.*;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * Abstract class to set up the colocated UI test repo
 */
public abstract class AbstractProvisioningUITest extends AbstractProvisioningTest {

	protected static final String TEST_REPO_PATH = "testRepos/updateSite/";
	protected static final String TESTPROFILE = "TestProfile";
	protected static final String TOPLEVELIU = "TopLevelIU";
	protected static final String TOPLEVELIU2 = "TopLevelIU2";
	protected static final String NESTEDIU = "NestedIU";
	protected static final String LOCKEDIU = "LockedIU";
	protected static final String UNINSTALLEDIU = "UninstalledIU";
	protected static final String CATEGORYIU = "CategoryIU";
	protected IMetadataRepositoryManager metaManager;
	protected IArtifactRepositoryManager artifactManager;
	protected URI testRepoLocation;
	protected IProfile profile;
	protected ProfileElement profileElement;
	protected IInstallableUnit top1;
	protected IInstallableUnit top2;
	protected IInstallableUnit nested;
	protected IInstallableUnit locked;
	protected IInstallableUnit upgrade;
	protected IInstallableUnit uninstalled;
	protected IInstallableUnit category;
	protected ProvisioningUI ui;
	protected ServiceRegistration<LicenseManager> regLicenseManager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// create test profile
		profile = createProfile(TESTPROFILE);

		// copy of provisioning UI that uses a different profile
		ui = ProvisioningUI.getDefaultUI();
		ui = new ProvisioningUI(ui.getSession(), TESTPROFILE, ui.getPolicy());
		ui.getOperationRunner().suppressRestart(true);
		ui.getPolicy().setRepositoriesVisible(false);

		// register alternate services
		SimpleLicenseManager manager = new SimpleLicenseManager(TESTPROFILE);
		Dictionary<String, Object> properties = new Hashtable<>(5);
		properties.put(Constants.SERVICE_RANKING, Integer.valueOf(1));
		regLicenseManager = TestActivator.getContext().registerService(LicenseManager.class, manager, properties);

		profileElement = new ProfileElement(null, TESTPROFILE);
		install((top1 = createIU(TOPLEVELIU, Version.create("1.0.0"))), true, false);
		install((top2 = createIU(TOPLEVELIU2)), true, false);
		install((nested = createIU(NESTEDIU)), false, false);
		install((locked = createIU(LOCKEDIU)), true, true);
		uninstalled = createIU(UNINSTALLEDIU);
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor(TOPLEVELIU,
				new VersionRange("[1.0.0, 1.0.0]"), 0, "update description");
		upgrade = createIU(TOPLEVELIU, Version.createOSGi(2, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null,
				NO_TP_DATA, false, update, NO_REQUIRES);

		category = createNamedIU(CATEGORYIU, CATEGORYIU, Version.create("1.0.0"), true);
		createTestMetdataRepository(new IInstallableUnit[] { top1, top2, uninstalled, upgrade });

		metaManager = getAgent().getService(IMetadataRepositoryManager.class);
		artifactManager = getAgent().getService(IArtifactRepositoryManager.class);
		File site = new File(TestActivator.getTestDataFolder().toString(), TEST_REPO_PATH);
		testRepoLocation = site.toURI();
		metaManager.addRepository(testRepoLocation);
		artifactManager.addRepository(testRepoLocation);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		metaManager.removeRepository(testRepoLocation);
		artifactManager.removeRepository(testRepoLocation);
		regLicenseManager.unregister();
	}

	protected boolean managerContains(IRepositoryManager<?> manager, URI location) {
		URI[] locations = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (URI location1 : locations) {
			if (location1.equals(location)) {
				return true;
			}
		}
		return false;
	}

	protected ProvisioningSession getSession() {
		return ui.getSession();
	}

	protected ProvisioningUI getProvisioningUI() {
		return ui;
	}

	protected Policy getPolicy() {
		return ui.getPolicy();
	}

	protected IStatus install(IInstallableUnit iu, boolean root, boolean lock) {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.add(iu);
		if (root) {
			req.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
		}
		if (lock) {
			req.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU,
					Integer.valueOf(IProfile.LOCK_UNINSTALL | IProfile.LOCK_UPDATE).toString());
		}
		// Use an empty provisioning context to prevent repo access
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[] {});
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		IProvisioningPlan plan = getPlanner(getSession().getProvisioningAgent()).getProvisioningPlan(req, context,
				monitor);
		monitor.assertUsedUp();
		if (plan.getStatus().getSeverity() == IStatus.ERROR || plan.getStatus().getSeverity() == IStatus.CANCEL)
			return plan.getStatus();
		FussyProgressMonitor monitor2 = new FussyProgressMonitor();
		try {
			return getSession().performProvisioningPlan(plan, PhaseSetFactory.createDefaultPhaseSet(),
					new ProvisioningContext(getAgent()), monitor2);
		} finally {
			monitor2.assertUsedUp();
		}
	}

	protected IInstallableUnit createNamedIU(String id, String name, Version version, boolean isCategory) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(id);
		iu.setVersion(version);
		iu.setProperty(IInstallableUnit.PROP_NAME, name);
		if (isCategory)
			iu.setProperty(InstallableUnitDescription.PROP_TYPE_CATEGORY, Boolean.toString(true));
		return MetadataFactory.createInstallableUnit(iu);
	}

	protected ProfileModificationJob getLongTestOperation() {
		return new ProfileModificationJob("Test Operation", getSession(), TESTPROFILE, null, null) {
			@Override
			public IStatus runModal(IProgressMonitor monitor) {
				while (true) {
					// spin unless cancelled
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
				}
			}
		};
	}
}
