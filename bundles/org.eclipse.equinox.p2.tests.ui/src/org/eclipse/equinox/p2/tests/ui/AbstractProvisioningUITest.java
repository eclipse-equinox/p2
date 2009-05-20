/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui;

import java.io.File;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.*;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

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

	protected void setUp() throws Exception {
		super.setUp();
		ProvisioningOperationRunner.suppressRestart(true);
		profile = createProfile(TESTPROFILE);
		profileElement = new ProfileElement(null, TESTPROFILE);
		install((top1 = createIU(TOPLEVELIU, new Version("1.0.0"))), true, false);
		install((top2 = createIU(TOPLEVELIU2)), true, false);
		install((nested = createIU(NESTEDIU)), false, false);
		install((locked = createIU(LOCKEDIU)), true, true);
		uninstalled = createIU(UNINSTALLEDIU);
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor(TOPLEVELIU, new VersionRange("[1.0.0, 1.0.0]"), 0, "update description");
		upgrade = createIU(TOPLEVELIU, new Version(2, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);

		category = createNamedIU(CATEGORYIU, CATEGORYIU, new Version("1.0.0"), true);
		createTestMetdataRepository(new IInstallableUnit[] {top1, top2, uninstalled, upgrade});

		metaManager = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.context, IMetadataRepositoryManager.class.getName());
		artifactManager = (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.context, IArtifactRepositoryManager.class.getName());
		File site = new File(TestActivator.getTestDataFolder().toString(), TEST_REPO_PATH);
		testRepoLocation = site.toURI();
		metaManager.addRepository(testRepoLocation);
		artifactManager.addRepository(testRepoLocation);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		metaManager.removeRepository(testRepoLocation);
		artifactManager.removeRepository(testRepoLocation);
	}

	protected boolean managerContains(IRepositoryManager manager, URI location) {
		URI[] locations = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < locations.length; i++) {
			if (locations[i].equals(location))
				return true;
		}
		return false;
	}

	protected IStatus install(IInstallableUnit iu, boolean root, boolean lock) throws ProvisionException {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {iu});
		if (root) {
			String rootProp = Policy.getDefault().getQueryContext().getVisibleInstalledIUProperty();
			if (rootProp != null)
				req.setInstallableUnitProfileProperty(iu, rootProp, Boolean.toString(true));
		}
		if (lock) {
			req.setInstallableUnitProfileProperty(iu, IInstallableUnit.PROP_PROFILE_LOCKED_IU, new Integer(IInstallableUnit.LOCK_UNINSTALL | IInstallableUnit.LOCK_UPDATE).toString());
		}
		// Use an empty provisioning context to prevent repo access
		ProvisioningPlan plan = ProvisioningUtil.getProvisioningPlan(req, new ProvisioningContext(new URI[] {}), getMonitor());
		if (plan.getStatus().getSeverity() == IStatus.ERROR || plan.getStatus().getSeverity() == IStatus.CANCEL)
			return plan.getStatus();
		return ProvisioningUtil.performProvisioningPlan(plan, new DefaultPhaseSet(), profile, new ProvisioningContext(), getMonitor());
	}

	protected IInstallableUnit createNamedIU(String id, String name, Version version, boolean isCategory) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(id);
		iu.setVersion(version);
		iu.setProperty(IInstallableUnit.PROP_NAME, name);
		if (isCategory)
			iu.setProperty(IInstallableUnit.PROP_TYPE_CATEGORY, Boolean.toString(true));
		return MetadataFactory.createInstallableUnit(iu);
	}

	protected ProfileModificationOperation getLongTestOperation() {
		return new ProfileModificationOperation("Test Operation", TESTPROFILE, null, null) {
			protected IStatus doExecute(IProgressMonitor monitor) {
				while (true) {
					// spin unless cancelled
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
				}
			}
		};
	}
}
