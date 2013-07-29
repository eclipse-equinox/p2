/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     Ericsson AB - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.tests.verifier;

import java.net.URI;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration.MigrationSupport;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class MigrationWizardTestHelper extends MigrationSupport {
	//Variable that keeps track if the wizard has been requested to open
	public boolean wizardOpened = false;

	@Override
	protected void openMigrationWizard(final IProfile inputProfile, final Collection<IInstallableUnit> unitsToMigrate, final URI[] reposToMigrate) {
		wizardOpened = true;
	}

}
